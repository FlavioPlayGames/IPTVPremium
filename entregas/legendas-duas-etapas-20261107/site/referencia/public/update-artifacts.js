export const updateArtifactNames={universal:'Universal','armeabi-v7a':'ARM 32 bits','arm64-v8a':'ARM 64 bits',x86:'x86 · emuladores',x86_64:'x86_64 · emuladores'};
export function createUpdateDraft(update){
 return Object.fromEntries(Object.keys(updateArtifactNames).map(abi=>{
  const value=abi==='universal'?update:update?.variants?.[abi];
  return [abi,{enabled:abi==='universal'||Boolean(value),url:value?.url||'',sha256:value?.sha256||'',sizeBytes:value?.sizeBytes||''}];
 }));
}
export function buildUpdateBody(meta,draft){
 const versionCode=Number(meta.versionCode),versionName=meta.versionName.trim();
 if(!Number.isSafeInteger(versionCode)||versionCode<1||versionCode>2100000000||!versionName||versionName.length>40)throw Error('Informe o número interno e o nome da versão.');
 function artifact(abi){
  const value=draft[abi];let url;
  const invalid=()=>{const error=Error('Confira o link HTTPS, tamanho e SHA-256 do APK '+updateArtifactNames[abi]+'.');error.abi=abi;throw error;};
  try{url=new URL(value.url.trim())}catch{invalid()}
  if(url.protocol!=='https:'||url.username||url.password||url.hash||value.url.trim().length>4096)invalid();
  const sizeBytes=Number(value.sizeBytes),sha256=value.sha256.trim().toLowerCase();
  if(!Number.isSafeInteger(sizeBytes)||sizeBytes<1||sizeBytes>536870912||!/^[a-f0-9]{64}$/.test(sha256))invalid();
  return {url:value.url.trim(),sha256,sizeBytes};
 }
 const variants={};
 for(const abi of Object.keys(updateArtifactNames))if(abi!=='universal'&&draft[abi].enabled)variants[abi]=artifact(abi);
 return {versionCode,versionName,...artifact('universal'),notes:meta.notes.trim().slice(0,500),...(Object.keys(variants).length?{variants}:{})};
}

export function samePublishedUpdate(expected,actual){
 if(!actual||actual.versionCode!==expected.versionCode||actual.versionName!==expected.versionName||actual.notes!==expected.notes||actual.mandatory!==false)return false;
 const same=(a,b)=>Boolean(b)&&a.url===b.url&&a.sha256===b.sha256&&a.sizeBytes===b.sizeBytes;
 if(!same(expected,actual))return false;
 const e=expected.variants||{},a=actual.variants||{};
 return Object.keys(e).length===Object.keys(a).length&&Object.keys(e).every(abi=>same(e[abi],a[abi]));
}
