import assert from 'node:assert/strict';import {readFile,writeFile}from'node:fs/promises';import{webcrypto,randomUUID}from'node:crypto';
import{fileURLToPath,pathToFileURL}from'node:url';import{mkdtemp}from'node:fs/promises';import{tmpdir}from'node:os';import{join}from'node:path';
const root=fileURLToPath(new URL('../portal',import.meta.url));const temp=await mkdtemp(join(tmpdir(),'iptv-r26-'));const modulePath=join(temp,'worker.mjs');
await writeFile(modulePath,await readFile(root+'/worker.js'));
const {default:worker}=await import(pathToFileURL(modulePath).href);
const digest=async x=>Buffer.from(await crypto.subtle.digest('SHA-256',new TextEncoder().encode(x))).toString('hex');
const secret='a'.repeat(64),secret2='b'.repeat(64),cookie='c'.repeat(64),cookie2='d'.repeat(64),id1=randomUUID(),id2=randomUUID();
const hashes=new Map([[await digest(secret),id1],[await digest(secret2),id2]]);const sessions=new Map([[await digest(cookie),id1],[await digest(cookie2),id2]]);
class DB{prepare(sql){let args=[];return{bind(...a){args=a;return this},async first(){
 if(sql.startsWith('SELECT * FROM devices WHERE credential_hash=')){const id=hashes.get(args[0]);return id?{id,code_hash:'test-code'}:null;}
 if(sql.startsWith('SELECT * FROM devices WHERE id='))return [id1,id2].includes(args[0])?{id:args[0],code_hash:'test-code'}:null;
 if(sql.startsWith('SELECT * FROM sessions')){const id=sessions.get(args[0]);return id?{device_id:id,role:'user',csrf:'csrf',token_hash:args[0]}:null;}
 if(sql.startsWith('INSERT INTO limits'))return{count:1};throw Error('Unexpected first SQL '+sql);
 },async run(){if(sql.startsWith('INSERT INTO qr_logins'))return{meta:{changes:1}};throw Error('Unexpected run '+sql);}}}}
class Bucket{map=new Map();seq=0;beforePut=null;
 async get(key){const o=this.map.get(key);if(!o)return null;const bytes=Buffer.from(o.bytes);return{etag:o.etag,json:async()=>JSON.parse(bytes),body:new Response(bytes).body};}
 async put(key,body,options={}){if(this.beforePut)await this.beforePut(key);
 const old=this.map.get(key),condition=options.onlyIf;
 if(condition?.etagMatches&&old?.etag!==condition.etagMatches)return null;
 if(condition?.etagDoesNotMatch==='*'&&old)return null;
 const value={bytes:Buffer.from(typeof body==='string'?body:body),etag:String(++this.seq)};this.map.set(key,value);return{etag:value.etag};}
 async delete(key){this.map.delete(key);}}
const bucket=new Bucket(),env={DB:new DB(),MANUAL_SUBTITLES:bucket,APP_KEY:'0'.repeat(64),ADMIN_PASSWORD:'test-password-not-real'};
const origin='https://portal.test';
async function call(path,{method='GET',body,device=false,other=false,csrf=true,raw=false}={}){
 const headers=device?{Authorization:'Bearer '+(other?secret2:secret)}:{Cookie:'iptv_session='+(other?cookie2:cookie),Origin:origin,...csrf?{'X-CSRF-Token':'csrf'}:{}};
 if(body&&!raw)headers['Content-Type']='application/json';
 const r=await worker.fetch(new Request(origin+path,{method,headers,body:body==null?undefined:raw?body:JSON.stringify(body)}),env,{});return r;
}
const target={kind:'episode',playlistId:'list-a',contentId:'series-1',title:'Fear the Walking Dead',season:5,episode:1,kids:false};
async function create(t=target){const id=randomUUID(),r=await call('/api/device/manual-subtitles/request',{method:'POST',body:{id,target:t},device:true});assert.equal(r.status,200,await r.clone().text());const result=await r.json();assert.ok(result.url.startsWith(origin+'/#access='));assert.ok(result.url.includes('&view=manual&request='+id));return result.id;}
const bytes=Buffer.from('1\n00:00:01,000 --> 00:00:03,000\nOlá!\n');const uploaded=id=>call('/api/manual-subtitles/upload?id='+id+'&name=episode.srt',{method:'POST',body:bytes,raw:true});
let r=await worker.fetch(new Request(origin+'/api/device/manual-subtitles/request'),env,{});assert.equal(r.status,401);
const id=await create();r=await call('/api/manual-subtitles/request?id='+id,{other:true});assert.equal(r.status,404);
r=await call('/api/manual-subtitles/upload?id='+id+'&name=x.srt',{method:'POST',body:bytes,raw:true,csrf:false});assert.equal(r.status,403);
r=await uploaded(id);assert.equal(r.status,200,await r.clone().text());
r=await call('/api/device/manual-subtitles/file?id='+id,{device:true});assert.equal(r.status,200);assert.deepEqual(Buffer.from(await r.arrayBuffer()),bytes);assert.equal(r.headers.get('x-subtitle-sha256'),Buffer.from(await crypto.subtle.digest('SHA-256',bytes)).toString('hex'));
r=await call('/api/device/manual-subtitles/receipt?id='+id,{device:true,method:'POST',body:{success:true,count:1}});assert.equal(r.status,200);assert.equal([...bucket.map.keys()].filter(k=>k.endsWith('.bin')).length,0);
r=await call('/api/manual-subtitles/request?id='+id);assert.equal((await r.json()).request.state,'received');assert.equal((await uploaded(id)).status,410);
const old=await create();await create();assert.equal((await uploaded(old)).status,410);
const cancelled=await create();assert.equal((await call('/api/device/manual-subtitles/request?id='+cancelled,{device:true,method:'DELETE'})).status,200);assert.equal((await uploaded(cancelled)).status,410);
const concurrent=await create();const results=await Promise.all([uploaded(concurrent),uploaded(concurrent)]);assert.deepEqual(results.map(x=>x.status).sort(),[200,409]);
await call('/api/device/manual-subtitles/request?id='+concurrent,{device:true,method:'DELETE'});
const racing=await create();let unblock,started;const gate=new Promise(r=>{unblock=r}),seen=new Promise(r=>{started=r});
bucket.beforePut=async key=>{if(key.endsWith('.bin')){started();await gate;}};
const race=uploaded(racing);await seen;await call('/api/device/manual-subtitles/request?id='+racing,{device:true,method:'DELETE'});unblock();assert.equal((await race).status,409);bucket.beforePut=null;assert.equal([...bucket.map.keys()].filter(k=>k.endsWith('.bin')).length,0);
const invalid=await create();r=await call('/api/manual-subtitles/upload?id='+invalid+'&name=x.exe',{method:'POST',body:bytes,raw:true});assert.equal(r.status,422);
r=await call('/api/device/manual-subtitles/request',{method:'POST',body:{id:randomUUID(),target:{...target,episode:-1}},device:true});assert.equal(r.status,422);
const season=await create({...target,kind:'season',episodes:[1,2,3]});r=await call('/api/manual-subtitles/upload?id='+season+'&name=x.zip',{method:'POST',body:bytes,raw:true});assert.equal(r.status,422);
const retryable=await create();assert.equal((await uploaded(retryable)).status,200);
r=await call('/api/device/manual-subtitles/receipt?id='+retryable,{device:true,method:'POST',body:{success:false,count:0}});assert.equal(r.status,200);
r=await call('/api/manual-subtitles/request?id='+retryable);assert.equal((await r.json()).request.state,'waiting');assert.equal((await uploaded(retryable)).status,200);
await call('/api/device/manual-subtitles/request?id='+retryable,{device:true,method:'DELETE'});
const expired=await create(),key=`manual/${id1}/${expired}/request.json`;const row=JSON.parse(bucket.map.get(key).bytes);await bucket.put(key,JSON.stringify({...row,expires:Date.now()-1}));
r=await call('/api/manual-subtitles/request?id='+expired);assert.equal((await r.json()).request.state,'expired');assert.equal((await uploaded(expired)).status,410);
assert.equal((await call('/api/device/manual-subtitles/file?id='+expired,{device:true})).status,410);
const sizeLimited=await create();r=await call('/api/manual-subtitles/upload?id='+sizeLimited+'&name=x.zip',{method:'POST',body:Buffer.alloc(8*1024*1024+1),raw:true});assert.equal(r.status,413);
console.log('PASS adicional: QR aponta ao destino manual, rejeicao permite novo arquivo, expiracao recusa upload/download, limite real de 8 MB.');
console.log('PASS: autenticação, isolamento entre TVs, CSRF, ciclo de envio/recebimento, hash, descarte, substituição, cancelamento, uploads concorrentes e corrida cancelamento/upload.');
