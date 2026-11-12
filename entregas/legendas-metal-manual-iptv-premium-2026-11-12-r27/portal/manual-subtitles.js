// Manual uploads use a private R2 bucket. No catalogue/stream URLs or provider credentials enter it.
const MANUAL_MAX_BYTES=8*1024*1024, MANUAL_TTL=15*60*1000;
function manualTarget(value){
 if(!value||!['movie','episode','season'].includes(value.kind))fail(422,'Destino de legenda inválido.');
 const r={kind:value.kind,title:text(value.title,200,'Título'),playlistId:text(value.playlistId,120,'Lista'),contentId:text(value.contentId,200,'Conteúdo'),kids:value.kids===true};
 if(Number.isSafeInteger(value.year)&&value.year>=1888&&value.year<=2100)r.year=value.year;
 if(typeof value.tmdbId==='string'&&/^[0-9]{1,12}$/.test(value.tmdbId))r.tmdbId=value.tmdbId;
 if(value.kind!=='movie'){
  if(!Number.isSafeInteger(value.season)||value.season<0||value.season>9999)fail(422,'Temporada inválida.');r.season=value.season;
  if(value.kind==='episode'){
   if(!Number.isSafeInteger(value.episode)||value.episode<1||value.episode>99999)fail(422,'Episódio inválido.');r.episode=value.episode;
  }else{
   if(!Array.isArray(value.episodes)||!value.episodes.length||value.episodes.length>500||value.episodes.some(n=>!Number.isSafeInteger(n)||n<1||n>99999))fail(422,'Episódios da temporada inválidos.');
   r.episodes=[...new Set(value.episodes)].sort((a,b)=>a-b);
  }
 }
 return r;
}
function manualBucket(env){if(!env.MANUAL_SUBTITLES)fail(503,'O envio manual ainda não foi habilitado na central.');return env.MANUAL_SUBTITLES;}
function manualId(id){if(typeof id!=='string'||!/^[0-9a-f-]{36}$/.test(id))fail(422,'Pedido inválido.');return id;}
function manualPrefix(device,id){return `manual/${device}/${manualId(id)}/`;}
async function manualRead(bucket,device,id){
 const key=manualPrefix(device,id)+'request.json',object=await bucket.get(key);
 if(!object)fail(404,'Inicie um novo envio no aplicativo.');
 const data=await object.json();return {key,data,etag:object.etag};
}
async function manualCurrent(bucket,device,id){
 const current=await bucket.get(`manual/${device}/current.json`);
 if(!current||(await current.json()).id!==id)fail(410,'Este pedido foi substituído. Confira a tela da TV.');
}
async function manualCas(bucket,row,data){
 const result=await bucket.put(row.key,JSON.stringify(data),{onlyIf:{etagMatches:row.etag},httpMetadata:{contentType:'application/json'}});
 if(!result)fail(409,'O pedido mudou. Atualize a página e confira a TV.');
 return result;
}
function manualPublic(data,now=Date.now()){
 return {id:data.id,target:data.target,state:data.expires<=now&&!['received','cancelled'].includes(data.state)?'expired':data.state,
  expires:data.expires,fileName:data.fileName||null,message:data.message||null,receivedCount:data.receivedCount||0,maxBytes:MANUAL_MAX_BYTES};
}
async function manualRaw(request){
 if(Number(request.headers.get('Content-Length')||0)>MANUAL_MAX_BYTES)fail(413,'Envie um arquivo de até 8 MB.');
 const reader=request.body?.getReader();if(!reader)fail(422,'Selecione um arquivo SRT ou ZIP.');
 const parts=[];let size=0;
 try{while(true){const {value,done}=await reader.read();if(done)break;size+=value.byteLength;if(size>MANUAL_MAX_BYTES){await reader.cancel();fail(413,'Envie um arquivo de até 8 MB.');}parts.push(value);}}
 finally{reader.releaseLock();}
 if(!size)fail(422,'O arquivo está vazio.');
 const bytes=new Uint8Array(size);let at=0;for(const p of parts){bytes.set(p,at);at+=p.length;}return bytes;
}
async function manualApi(r,env){
 const u=new URL(r.url),path=u.pathname,m=r.method,bucket=manualBucket(env);
 const deviceSide=path.startsWith('/api/device/manual-subtitles/');
 const auth=deviceSide?await deviceAuth(r,env):await session(r,env,m!=='GET');
 const device=deviceSide?auth.id:target(auth,u);
 await findDevice(env,device);
 const id=u.searchParams.get('id');
 if(path==='/api/device/manual-subtitles/request'&&m==='POST'){
  await limit(env,'manual-create:'+device,12,60);
  const body=await readBody(r),target=manualTarget(body.target),requestId=manualId(body.id);
  const prefix=manualPrefix(device,requestId),key=prefix+'request.json',existing=await bucket.get(key);
  if(existing)fail(409,'Crie outro pedido para este envio.');
  // Retire the previous request before publishing the new one; uploads check the pointer again.
  const previous=await bucket.get(`manual/${device}/current.json`);
  if(previous){const old=await previous.json();try{const row=await manualRead(bucket,device,old.id);if(!['received','cancelled'].includes(row.data.state)){
   await manualCas(bucket,row,{...row.data,state:'cancelled',message:'Substituído por outro pedido na TV.'});if(row.data.fileKey)await bucket.delete(row.data.fileKey);
  }}catch(error){if(!(error instanceof ApiError&&[404,409].includes(error.status)))throw error;}}
  const now=Date.now(),expires=now+MANUAL_TTL,data={id:requestId,target,state:'waiting',created:now,expires};
  const created=await bucket.put(key,JSON.stringify(data),{onlyIf:{etagDoesNotMatch:'*'},httpMetadata:{contentType:'application/json'}});
  if(!created)fail(409,'Pedido já existente. Inicie outro envio.');
  await bucket.put(`manual/${device}/current.json`,JSON.stringify({id:requestId,expires}));
  const token=random(32);const d=await findDevice(env,device);
  await env.DB.prepare('INSERT INTO qr_logins VALUES(?,?,?,?) ON CONFLICT(device_id) DO UPDATE SET token_hash=excluded.token_hash,code_hash=excluded.code_hash,expires=excluded.expires').bind(await hash(token),device,d.code_hash,expires).run();
  return json({...manualPublic(data),url:u.origin+'/#access='+token+'&view=manual&request='+requestId});
 }
 if(path==='/api/manual-subtitles/request'&&m==='GET'&&!id){
  const current=await bucket.get(`manual/${device}/current.json`);if(!current)return json({request:null});
  const c=await current.json();const row=await manualRead(bucket,device,c.id);return json({request:manualPublic(row.data)});
 }
 manualId(id);const row=await manualRead(bucket,device,id),data=row.data;
 if(path.endsWith('/request')&&m==='GET')return json({request:manualPublic(data)});
 if(path==='/api/device/manual-subtitles/request'&&m==='DELETE'){
  if(['received','cancelled'].includes(data.state))return json({request:manualPublic(data)});
  await manualCas(bucket,row,{...data,state:'cancelled',message:'Cancelado na TV.'});if(data.fileKey)await bucket.delete(data.fileKey);return json({ok:true});
 }
 if(path==='/api/device/manual-subtitles/receipt'&&m==='POST'){
  if(data.state==='received')return json({ok:true});
  if(data.state!=='ready'||data.expires<=Date.now())fail(410,'O pedido não está disponível para confirmação.');
  const body=await readBody(r);if(typeof body.success!=='boolean'||body.success&&(!Number.isSafeInteger(body.count)||body.count<1||body.count>500))fail(422,'Confirmação inválida.');
  const next={...data,state:body.success?'received':'waiting',receivedCount:body.success?body.count:0,message:body.success?'Legenda recebida pela TV.':'O arquivo não foi aplicado. Confira o destino na TV e envie outro arquivo.',fileKey:null,sha256:null};
  await manualCas(bucket,row,next);if(data.fileKey)await bucket.delete(data.fileKey);return json({ok:true});
 }
 await manualCurrent(bucket,device,id);
 if(data.expires<=Date.now()||['received','cancelled'].includes(data.state))fail(410,'O pedido terminou. Inicie outro envio na TV.');
 if(path==='/api/manual-subtitles/upload'&&m==='POST'){
  await limit(env,'manual-upload:'+device,10,900);
  if(data.state!=='waiting')fail(409,'Já existe um arquivo em processamento para este pedido.');
  const name=text(u.searchParams.get('name'),200,'Nome do arquivo');if(!/\.(srt|zip)$/i.test(name))fail(422,'Selecione um arquivo SRT ou ZIP.');
  const bytes=await manualRaw(r),zip=name.toLowerCase().endsWith('.zip');
  if(zip&&(bytes[0]!==80||bytes[1]!==75))fail(422,'O arquivo não é um ZIP válido.');
  if(!zip&&bytes.length>1024*1024)fail(413,'Cada SRT deve ter até 1 MB.');
  const nonce=crypto.randomUUID(),fileKey=manualPrefix(device,id)+nonce+'.bin';
  const claimed=await manualCas(bucket,row,{...data,state:'uploading',nonce});
  try{
   await bucket.put(fileKey,bytes,{httpMetadata:{contentType:'application/octet-stream'},customMetadata:{expires:String(data.expires)}});
   await manualCurrent(bucket,device,id);if(data.expires<=Date.now())fail(410,'O pedido expirou durante o envio.');
   const sha256=hex(await crypto.subtle.digest('SHA-256',bytes));
   await manualCas(bucket,{...row,etag:claimed.etag},{...data,state:'ready',message:null,fileName:name,fileKey,sha256,size:bytes.length});
   return json({ok:true,request:manualPublic({...data,state:'ready',message:null,fileName:name})});
  }catch(error){await bucket.delete(fileKey);const latest=await manualRead(bucket,device,id);if(latest.data.state==='uploading'&&latest.data.nonce===nonce){await manualCas(bucket,latest,{...data,state:'waiting'});}throw error;}
 }
 if(path==='/api/device/manual-subtitles/file'&&m==='GET'){
  if(data.state!=='ready'||!data.fileKey)fail(409,'Ainda não há arquivo para receber.');
  const file=await bucket.get(data.fileKey);if(!file)fail(410,'O arquivo expirou. Envie novamente.');
  return new Response(file.body,{headers:{'Content-Type':'application/octet-stream','Content-Length':String(data.size),'X-Subtitle-SHA256':data.sha256,'X-Subtitle-Name':encodeURIComponent(data.fileName)}});
 }
 fail(404,'Página não encontrada.');
}
