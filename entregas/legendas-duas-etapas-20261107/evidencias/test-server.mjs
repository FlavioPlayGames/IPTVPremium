import assert from 'node:assert/strict';
import {DatabaseSync} from 'node:sqlite';
import {writeFileSync} from 'node:fs';
import worker,{seal,unseal} from './site/worker-atualizacao-abi.js';
import {connectOpenAccount,openPortalReady} from './site/opensubtitles.js';
let checks=0;const ok=(v,label)=>{assert(v,label);checks++;console.log('PASS '+label)};
const key='test-open-application-key',agent='IPTVPremium v1.13',token='test-open-session-token';
const fp=await crypto.subtle.digest('SHA-256',new TextEncoder().encode(key)).then(b=>Buffer.from(b).toString('hex'));
const env={APP_KEY:'1'.repeat(64),ADMIN_PASSWORD:'only-test-password-123',OPENSUBTITLES_API_KEY:key,OPENSUBTITLES_USER_AGENT:agent,ASSETS:{fetch:()=>new Response('asset')}};
const snapshot={lists:[],subtitles:{textColor:'FFFF00',background:'NONE',sizeSp:22,typeface:'DEFAULT',edge:'SHADOW'},providers:{subdl:false,opensubtitles:false},subtitleProtocol:2,openSubtitlesFingerprint:fp};
const db=new DatabaseSync(':memory:');db.exec(`CREATE TABLE devices(id TEXT PRIMARY KEY,credential_hash TEXT,mac TEXT,label TEXT,model TEXT,last_seen INTEGER,created_at INTEGER,status TEXT,trial_until INTEGER,license_until INTEGER,code_cipher TEXT);
CREATE TABLE sessions(token_hash TEXT PRIMARY KEY,role TEXT,device_id TEXT,csrf TEXT,expires INTEGER);
CREATE TABLE limits(key TEXT PRIMARY KEY,count INTEGER,expires INTEGER);
CREATE TABLE snapshots(device_id TEXT PRIMARY KEY,cipher TEXT,received_at INTEGER,expires INTEGER);
CREATE TABLE commands(seq INTEGER PRIMARY KEY AUTOINCREMENT,id TEXT UNIQUE,device_id TEXT,kind TEXT,cipher TEXT,created_at INTEGER,expires INTEGER,state TEXT DEFAULT 'pending',result TEXT,finished_at INTEGER);
CREATE TABLE settings(key TEXT PRIMARY KEY,value TEXT);
CREATE TABLE renewal_preferences(device_id TEXT PRIMARY KEY,license_until INTEGER,acked_stage INTEGER,choice TEXT,chosen_at INTEGER);
CREATE TABLE device_push(device_id TEXT PRIMARY KEY,last_pull_at INTEGER);`);
const sql=[];
env.DB={prepare(q){const s=db.prepare(q);let a=[];return {bind(...args){a=args;return this},async first(){return s.get(...a)||null},async all(){return {results:s.all(...a)}},async run(){sql.push([q,a]);return {meta:{changes:Number(s.run(...a).changes)}}}}},async batch(ss){return Promise.all(ss.map(s=>s.run()))}};
const digest=x=>crypto.subtle.digest('SHA-256',new TextEncoder().encode(x)).then(b=>Buffer.from(b).toString('hex'));
for(const id of ['tv-a','tv-b'])db.prepare('INSERT INTO devices VALUES(?,?,?,?,?,?,?,?,?,?,?)').run(id,await digest((id==='tv-a'?'d':'e').repeat(64)),'02:00:00:00:00:01',id,'TV teste',0,Date.now(),'active',Date.now()+86400000,0,await seal(env,'123456',id));
const cookie='a'.repeat(64),csrf='test-csrf';db.prepare('INSERT INTO sessions VALUES(?,?,?,?,?)').run(await digest(cookie),'user','tv-a',csrf,Date.now()+3600000);
async function saveSnapshot(value){db.prepare('INSERT INTO snapshots VALUES(?,?,?,?) ON CONFLICT(device_id) DO UPDATE SET cipher=excluded.cipher,received_at=excluded.received_at,expires=excluded.expires').run('tv-a',await seal(env,value,'tv-a'),Date.now(),Date.now()+3600000)}
await saveSnapshot(snapshot);
let upstream=0;const original=globalThis.fetch;
globalThis.fetch=async(url,options)=>{upstream++;assert.equal(url,'https://api.opensubtitles.com/api/v1/login');assert.equal(options.redirect,'manual');assert.equal(options.headers['Api-Key'],key);assert.equal(JSON.parse(options.body).password,' pass with spaces ');return Response.json({token,base_url:'api.opensubtitles.com',user:{user_id:42}})};
function request(path,body={},headers={}){return new Request('https://portal.test'+path,{method:'POST',headers:{Cookie:'iptv_session='+cookie,Origin:'https://portal.test','X-CSRF-Token':csrf,'Content-Type':'application/json',...headers},body:JSON.stringify(body)})}
const payload=()=>({username:'viewer',password:' pass with spaces ',requestId:crypto.randomUUID()});
try{
 for(const headers of [{Cookie:''},{'X-CSRF-Token':'wrong'},{Origin:'https://evil.test'}]){const r=await worker.fetch(request('/api/opensubtitles/connect',payload(),headers),env,{});ok([401,403].includes(r.status),'session, CSRF and origin required before provider access')}
 ok(upstream===0,'unauthenticated requests perform no OpenSubtitles calls');
 let r=await worker.fetch(new Request('https://portal.test/api/account',{headers:{Cookie:'iptv_session='+cookie}}),env,{});let account=await r.json();ok(account.openSubtitlesAvailable===true,'ready app registration enables phone login');
 const body=payload();r=await worker.fetch(request('/api/opensubtitles/connect?device=tv-b',body),env,{});assert.equal(r.status,200);const receipt=await r.json();ok(!JSON.stringify(receipt).includes(token),'browser receives only delivery receipt, never token');
 let row=db.prepare('SELECT * FROM commands WHERE id=?').get(body.requestId);ok(row.device_id==='tv-a','user cannot redirect connection to a different TV');
 ok(row.expires-row.created_at<=900000 && row.expires>row.created_at,'encrypted session expires from delivery queue within 15 minutes');
 ok(!row.cipher.includes(token)&&!row.cipher.includes('viewer')&&!row.cipher.includes('pass with spaces'),'queue ciphertext contains no plaintext credentials');
 const delivery=await unseal(env,row.cipher,'tv-a');ok(delivery.token===token&&!('password'in delivery)&&!('username'in delivery),'only verified session is queued; password and username are absent');
 writeFileSync('/workspace/legendas-duas-etapas/server-session-fixture.json',JSON.stringify(delivery,null,2));
 const firstCalls=upstream;r=await worker.fetch(request('/api/opensubtitles/connect',body),env,{});ok(r.status===200&&upstream===firstCalls,'same request ID does not repeat authentication');
 r=await worker.fetch(new Request('https://portal.test/api/account',{headers:{Cookie:'iptv_session='+cookie}}),env,{});account=await r.json();const publicText=JSON.stringify(account);ok(!publicText.includes(token)&&!publicText.includes('pass with spaces')&&account.commands[0].provider==='opensubtitles','account status exposes metadata, not encrypted session contents');
 r=await worker.fetch(new Request('https://portal.test/api/device/pull',{headers:{Authorization:'Bearer '+'e'.repeat(64)}}),env,{});let pull=await r.json();ok(pull.commands.length===0,'another authenticated TV cannot pull the session');
 r=await worker.fetch(new Request('https://portal.test/api/device/pull',{headers:{Authorization:'Bearer '+'d'.repeat(64)}}),env,{});pull=await r.json();ok(pull.commands.some(c=>c.kind==='subtitle_session'&&c.data.token===token),'bound TV receives the verified session');
 const ack=request('/api/device/ack',{results:[{id:body.requestId,success:true}],snapshot:{...snapshot,providers:{subdl:false,opensubtitles:true}}},{Authorization:'Bearer '+'d'.repeat(64)});r=await worker.fetch(ack,env,{});assert.equal(r.status,200,await r.clone().text());ok(r.status===200,'TV receipt accepts new provider snapshot');
 row=db.prepare('SELECT * FROM commands WHERE id=?').get(body.requestId);ok(row.state==='received'&&row.cipher===null,'receipt deletes session ciphertext');
 r=await worker.fetch(new Request('https://portal.test/api/account',{headers:{Cookie:'iptv_session='+cookie}}),env,{});account=await r.json();ok(account.snapshot.providers.opensubtitles===true,'phone status confirms connection only after TV receipt');
 await saveSnapshot({...snapshot,subtitleProtocol:1});const savedCalls=upstream;r=await worker.fetch(request('/api/opensubtitles/connect',payload()),env,{});ok(r.status===503&&upstream===savedCalls,'older app cannot receive unsupported login payload');
 await saveSnapshot({...snapshot,openSubtitlesFingerprint:'f'.repeat(64)});r=await worker.fetch(request('/api/opensubtitles/connect',payload()),env,{});ok(r.status===503&&upstream===savedCalls,'mismatched application keys do not request user authentication');
 await saveSnapshot(snapshot);
 const incoming={username:'viewer',password:'secret'};await connectOpenAccount(env,incoming,async()=>Response.json({token,base_url:'api.opensubtitles.com'}));ok(incoming.password==='','submitted password is removed from parsed request object');
 for(const host of ['evil.test','https://api.opensubtitles.com@evil.test','http://api.opensubtitles.com'])await assert.rejects(connectOpenAccount(env,{username:'viewer',password:'secret'},async()=>Response.json({token,base_url:host})),e=>e.status===502);
 ok(true,'untrusted API host is rejected');
 await assert.rejects(connectOpenAccount(env,{username:'viewer',password:'secret'},async()=>new Response('upstream-secret',{status:401})),e=>e.status===422&&!e.message.includes('upstream-secret'));ok(true,'invalid provider password does not log the user out of our portal');
 await assert.rejects(connectOpenAccount(env,{username:'viewer',password:'secret'},async()=>new Response('x'.repeat(70000))),e=>e.status===502);ok(true,'oversized provider response rejected');
 ok(sql.every(([q,args])=>!JSON.stringify(args).includes('pass with spaces')),'no executed SQL bind contains a password');
}finally{globalThis.fetch=original;db.close()}
console.log(`${checks} server checks passed; SQLite and provider mock, no production writes.`);
