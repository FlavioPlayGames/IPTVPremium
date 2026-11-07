// Server-only authentication. Browser passwords are never queued or persisted.
export class OpenPortalError extends Error {
  constructor(status,message){super(message);this.status=status;}
}
const error=(status,message)=>{throw new OpenPortalError(status,message)};
const digest=async value=>[...new Uint8Array(await crypto.subtle.digest('SHA-256',new TextEncoder().encode(value)))].map(x=>x.toString(16).padStart(2,'0')).join('');
export function openConfigured(env){return typeof env.OPENSUBTITLES_API_KEY==='string'&&/^[^\s\x00-\x1f\x7f]{8,512}$/.test(env.OPENSUBTITLES_API_KEY)&&typeof env.OPENSUBTITLES_USER_AGENT==='string'&&env.OPENSUBTITLES_USER_AGENT.length>=3&&env.OPENSUBTITLES_USER_AGENT.length<=200&&!/[\x00-\x1f\x7f]/.test(env.OPENSUBTITLES_USER_AGENT)}
export async function openPortalReady(env,snapshot){return openConfigured(env)&&snapshot?.subtitleProtocol>=2&&snapshot.openSubtitlesFingerprint===await digest(env.OPENSUBTITLES_API_KEY)}
export async function connectOpenAccount(env,input,fetcher=fetch,now=Date.now()) {
  const username=typeof input.username==='string'?input.username.trim():'';
  const password=typeof input.password==='string'?input.password:'';
  input.password='';
  if(!openConfigured(env))error(503,'OpenSubtitles ainda não está disponível. O SubDL continua funcionando.');
  if(!username||username.length>320||!password||password.length>1024||/[\x00-\x1f\x7f]/.test(username+password))error(422,'Informe seu usuário e senha do OpenSubtitles.com.');
  let response;
  try {
    response=await fetcher('https://api.opensubtitles.com/api/v1/login',{method:'POST',redirect:'manual',signal:AbortSignal.timeout(12000),headers:{'Api-Key':env.OPENSUBTITLES_API_KEY,'User-Agent':env.OPENSUBTITLES_USER_AGENT,'Content-Type':'application/json',Accept:'application/json'},body:JSON.stringify({username,password})});
  }catch{error(502,'Não foi possível conectar ao OpenSubtitles. Tente novamente.');}
  if(response.status===401)error(422,'Confira seu usuário e senha do OpenSubtitles.com.');
  if(response.status===429)error(429,'Aguarde antes de tentar conectar novamente ao OpenSubtitles.');
  if(response.status===403)error(503,'OpenSubtitles ainda não está disponível. O SubDL continua funcionando.');
  if(!response.ok)error(502,'OpenSubtitles indisponível. Tente novamente mais tarde.');
  let json;
  try {
    if(Number(response.headers.get('Content-Length'))>65536){await response.body?.cancel();error(502,'Resposta do OpenSubtitles incompatível.');}
    const reader=response.body.getReader();const chunks=[];let size=0;
    try{while(true){const {done,value}=await reader.read();if(done)break;size+=value.length;if(size>65536){await reader.cancel();error(502,'Resposta do OpenSubtitles incompatível.');}chunks.push(value);}}
    finally{reader.releaseLock();}
    const bytes=new Uint8Array(size);let offset=0;for(const c of chunks){bytes.set(c,offset);offset+=c.length;}
    json=JSON.parse(new TextDecoder().decode(bytes));
  }catch{error(502,'Resposta do OpenSubtitles incompatível.');}
  const token=json.token;
  if(typeof token!=='string'||token.length<8||token.length>16000||/[^\x21-\x7e]/.test(token))error(502,'Sessão do OpenSubtitles incompatível.');
  let host;
  try{const raw=json.base_url||'api.opensubtitles.com';const u=new URL(raw.startsWith('https://')?raw:'https://'+raw);if(u.protocol!=='https:'||u.username||u.password||u.port||!['','/'].includes(u.pathname)||u.search||u.hash||!['api.opensubtitles.com','vip-api.opensubtitles.com'].includes(u.hostname))throw Error();host=u.hostname;}
  catch{error(502,'Servidor OpenSubtitles incompatível.');}
  let expiresAt=now+23*60*60*1000;
  try{const parts=token.split('.');if(parts.length===3){const p=parts[1].replaceAll('-','+').replaceAll('_','/');const claim=JSON.parse(atob(p));if(Number.isSafeInteger(claim.exp)&&claim.exp*1000>now)expiresAt=Math.min(expiresAt,claim.exp*1000);}}
  catch{/* The API validates the token; expiry fallback is deliberately bounded. */}
  const appFingerprint=await digest(env.OPENSUBTITLES_API_KEY);
  const userId=String(json.user?.user_id??'');
  const identity=/^[1-9][0-9]{0,17}$/.test(userId)?'id:'+userId:'name:'+username.toLowerCase();
  return {provider:'opensubtitles',token,apiHost:host,expiresAt,appFingerprint,accountScope:await digest(appFingerprint+'|'+identity)};
}
