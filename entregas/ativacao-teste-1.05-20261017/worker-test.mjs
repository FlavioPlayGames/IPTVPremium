import assert from 'node:assert/strict';
import {DatabaseSync} from 'node:sqlite';
import {createHash} from 'node:crypto';
import {readFileSync,writeFileSync} from 'node:fs';
const source=readFileSync('/workspace/trial-worker-work/worker-atualizacao-abi.js','utf8');
writeFileSync('/workspace/storage-toolchain/tests/ativacao-teste/worker-test-module.mjs',source+'\nexport {trialNoticeInfo,saveTrialChoice,renewalInfo};\n');
const {default:worker,trialNoticeInfo,saveTrialChoice,renewalInfo,seal}=await import('./worker-test-module.mjs');
const token='a'.repeat(64),credential=createHash('sha256').update(token).digest('hex'),now=Date.now(),until=now+7*86400000;
let tests=0;async function test(name,run){await run();tests++;console.log('PASS '+name)}
function fixture(){
 const db=new DatabaseSync(':memory:');db.exec(`CREATE TABLE devices(id TEXT PRIMARY KEY,credential_hash TEXT,mac TEXT,label TEXT,model TEXT,status TEXT,created_at INTEGER,trial_until INTEGER,license_until INTEGER,license_acquired_at INTEGER,recovery_hash TEXT,code_hash TEXT,code_cipher TEXT,last_seen INTEGER);
 CREATE TABLE renewal_preferences(device_id TEXT PRIMARY KEY REFERENCES devices(id),license_until INTEGER NOT NULL,choice TEXT NOT NULL CHECK(choice IN ('now','due')),acked_stage INTEGER NOT NULL CHECK(acked_stage IN (30,15,7)),chosen_at INTEGER NOT NULL);
 CREATE TABLE sessions(device_id TEXT); CREATE TABLE qr_logins(device_id TEXT); CREATE TABLE limits(key TEXT PRIMARY KEY,count INTEGER,expires INTEGER);`);
 db.prepare('INSERT INTO devices(id,credential_hash,mac,label,model,status,created_at,trial_until,license_until,license_acquired_at) VALUES(?,?,?,?,?,?,?,?,?,?)').run('one',credential,'02:00:00:00:00:01','TV','test','active',now,until,0,0);
 let race=null;const sqls=[];const env={APP_KEY:'1'.repeat(64),ADMIN_PASSWORD:'test-password-fixture-only',DB:{prepare(sql){let args=[];sqls.push(sql);return{bind(...a){args=a;return this},async first(){return db.prepare(sql).get(...args)||null},async run(){if(race&&sql.startsWith('INSERT INTO renewal_preferences')){const r=race;race=null;r()}const r=db.prepare(sql).run(...args);return{meta:{changes:Number(r.changes)}}}}}}};
 env.DB.batch=async statements=>Promise.all(statements.map(s=>s.run()));
 return{db,env,sqls,device:()=>db.prepare('SELECT * FROM devices').get(),race:f=>race=f};
}
function request(choice='due',expiresAt=until,auth=token){return new Request('https://portal.example/api/device/trial-choice',{method:'POST',headers:{Authorization:'Bearer '+auth,'Content-Type':'application/json'},body:JSON.stringify({choice,expiresAt})})}
await test('first trial is offered once; due choice stored in one existing SQLite row',async()=>{
 const f=fixture();assert.equal((await trialNoticeInfo(f.env,f.device())).acknowledged,false);
 const r=await worker.fetch(request(),f.env,{});assert.equal(r.status,200);assert.equal((await r.json()).trialNotice.acknowledged,true);
 assert.equal(f.db.prepare('SELECT count(*) n FROM renewal_preferences').get().n,1);assert.equal(f.device().trial_until,until);
});
await test('now choice acknowledgement survives credential replacement/reinstallation and repeated requests',async()=>{
 const f=fixture();for(let i=0;i<3;i++)assert.equal((await worker.fetch(request('now'),f.env,{})).status,200);
 f.db.prepare('UPDATE devices SET credential_hash=?').run('changed-after-restore');
 assert.equal((await trialNoticeInfo(f.env,f.device())).acknowledged,true);assert.equal(f.db.prepare('SELECT count(*) n FROM renewal_preferences').get().n,1);
});
await test('actual reinstall registration recovers the same acknowledged trial without extending access',async()=>{
 const f=fixture(),recovery='c'.repeat(64),newSecret='b'.repeat(64);
 await saveTrialChoice(f.env,f.device(),{choice:'due',expiresAt:until});
 f.db.prepare('UPDATE devices SET recovery_hash=?,code_hash=?,code_cipher=?').run(createHash('sha256').update('reinstall:'+recovery).digest('hex'),'dummy-code-hash',await seal(f.env,'123456','one'));
 const response=await worker.fetch(new Request('https://portal.example/api/device/register',{method:'POST',headers:{'Content-Type':'application/json'},body:JSON.stringify({secret:newSecret,recoverySecret:recovery,model:'restored TV'})}),f.env,{});
 assert.equal(response.status,200);const data=await response.json();assert.equal(data.id,'one');assert.equal(data.mac,'02:00:00:00:00:01');assert.equal(data.trialNotice.acknowledged,true);assert.equal(data.license.trialUntil,until);assert.equal(data.code,'123456');assert.equal(f.db.prepare('SELECT count(*) n FROM devices').get().n,1);
});
await test('authenticated endpoint rejects missing or foreign credentials',async()=>{
 const f=fixture();for(const credential of ['', 'b'.repeat(64)])assert.equal((await worker.fetch(request('due',until,credential),f.env,{})).status,401);
 assert.equal(f.db.prepare('SELECT count(*) n FROM renewal_preferences').get().n,0);
});
await test('invalid choices/deadlines are rejected without writes',async()=>{
 const f=fixture();for(const choice of ['later','',null])assert.equal((await worker.fetch(request(choice),f.env,{})).status,422);
 assert.equal((await worker.fetch(request('due',until+1),f.env,{})).status,409);assert.equal((await worker.fetch(request('due',1.5),f.env,{})).status,422);
 assert.equal(f.db.prepare('SELECT count(*) n FROM renewal_preferences').get().n,0);
});
await test('paid or blocked devices never receive welcome and cannot acknowledge trial',async()=>{
 for(const paid of [false,true]){const f=fixture();f.db.prepare(paid?'UPDATE devices SET license_until=?':'UPDATE devices SET status=?').run(paid?until+86400000:'blocked');assert.equal(await trialNoticeInfo(f.env,f.device()),null);assert.equal((await worker.fetch(request(),f.env,{})).status,409)}
});
await test('expiration uses original server deadline; no fresh seven-day period',async()=>{
 const f=fixture();assert.equal((await trialNoticeInfo(f.env,f.device(),until)).acknowledged,true);
 f.db.prepare('UPDATE devices SET trial_until=?').run(now-1);assert.equal((await worker.fetch(request('due',now-1),f.env,{})).status,409);
 assert.equal(f.device().trial_until,now-1);assert.equal(f.db.prepare('SELECT count(*) n FROM renewal_preferences').get().n,0);
});
await test('new administrator trial deadline gets its own acknowledgement; existing trial remains acknowledged',async()=>{
 const f=fixture();await saveTrialChoice(f.env,f.device(),{choice:'due',expiresAt:until});f.db.prepare('UPDATE devices SET trial_until=?').run(until+86400000);assert.equal((await trialNoticeInfo(f.env,f.device())).acknowledged,false);
});
await test('activation racing with acknowledgement cannot overwrite preference after license grant',async()=>{
 const f=fixture();f.race(()=>f.db.prepare('UPDATE devices SET license_until=?').run(until+86400000));assert.equal((await worker.fetch(request(),f.env,{})).status,409);assert.equal(f.db.prepare('SELECT count(*) n FROM renewal_preferences').get().n,0);
});
await test('pre-existing licensed renewal preference is replaced only by a valid trial; later licensed renewal still notices stage7',async()=>{
 const f=fixture();f.db.prepare('INSERT INTO renewal_preferences VALUES(?,?,?,?,?)').run('one',now-1,'due',7,now);
 assert.equal((await worker.fetch(request(),f.env,{})).status,200);
 f.db.prepare('UPDATE devices SET license_until=?').run(until);assert.equal((await renewalInfo(f.env,f.device(),now+1)).noticeStage,7);
});
await test('no schema migrations, command writes, catalog or APK storage introduced',async()=>{
 const f=fixture();await worker.fetch(request(),f.env,{});assert.ok(!f.sqls.some(s=>/^(CREATE|ALTER|DELETE|UPDATE devices|INSERT INTO commands)/.test(s)));
 assert.equal(f.device().trial_until,until);
});
console.log(tests+' authenticated Worker/SQLite scenarios passed');
