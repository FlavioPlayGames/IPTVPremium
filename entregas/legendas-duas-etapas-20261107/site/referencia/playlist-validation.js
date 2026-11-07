/** Read-only Xtream validation. Never logs or persists provider URLs/credentials. */
export class PlaylistValidationError extends Error {
  constructor(message, status = 422, kind = "format", httpStatus = null) { super(message); this.status = status; this.kind = kind; this.httpStatus = httpStatus; }
}
const invalid = message => { throw new PlaylistValidationError(message); };
function publicUrl(value) {
  let url;
  try { url = new URL(value); } catch { invalid('Informe um link completo, começando com http:// ou https://.'); }
  if (!['http:', 'https:'].includes(url.protocol) || url.username || url.password) invalid('Use um link HTTP ou HTTPS válido.');
  const host = url.hostname.toLowerCase().replace(/^\[|\]$/g, '').replace(/\.$/, '');
  // A provider validator is not a proxy to private or infrastructure services.
  if (!host.includes('.') || /(?:^|\.)(?:localhost|local|internal|test|invalid)$/.test(host) || host.includes(':')) invalid('Informe o endereço público do provedor.');
  if (/^\d+(?:\.\d+){3}$/.test(host)) {
    const [a,b] = host.split('.').map(Number);
    if (a === 0 || a === 10 || a === 127 || a >= 224 || a === 169 && b === 254 || a === 172 && b >= 16 && b <= 31 || a === 192 && b === 168 || a === 100 && b >= 64 && b <= 127 || a === 198 && [18,19].includes(b)) invalid('Informe o endereço público do provedor.');
  }
  if (host === 'metadata.google.internal' || host === 'metadata.google') invalid('Endereço do provedor inválido.');
  return url;
}
export function xtreamFromLink(name, link) {
  if (typeof name !== 'string' || !name.trim() || name.trim().length > 80) invalid('Informe o nome da lista, com até 80 caracteres.');
  if (typeof link !== 'string' || !link.trim() || link.length > 4096) invalid('Informe o link da lista.');
  const url = publicUrl(link.trim().replace(/&amp;/gi, '&').replace(/\\&/g, '&'));
  const path = url.pathname.replace(/\/+$/, '');
  if (!/\/(get|player_api)\.php$/i.test(path)) invalid('Esse link não contém uma configuração Xtream reconhecível. Peça ao provedor o link get.php com usuário e senha.');
  const credentials = {};
  for (const [rawKey, value] of url.searchParams) {
    const key = rawKey.toLowerCase();
    if (!['username', 'password'].includes(key)) continue;
    if (Object.hasOwn(credentials, key) && credentials[key] !== value) invalid('O link contém credenciais conflitantes. Confira com o provedor.');
    credentials[key] = value;
  }
  const username = credentials.username?.trim(), password = credentials.password;
  if (!username || username.length > 256 || !password || password.length > 512 || /[\u0000-\u001f]/.test(username + password)) invalid('O link precisa conter usuário e senha Xtream válidos.');
  const prefix = path.replace(/\/(get|player_api)\.php$/i, '').replace(/\/+$/, '');
  const fallback = new URL(url.href);
  fallback.pathname = prefix + '/get.php';fallback.searchParams.delete('action');
  if (!fallback.searchParams.has('type')) fallback.searchParams.set('type','m3u_plus');
  if (!fallback.searchParams.has('output')) fallback.searchParams.set('output','ts');
  return { type: 'xtream', name: name.trim(), isEnabled: true, serverUrl: url.origin + prefix, username, password,
    fallbackM3uUrl: fallback.href };
}
async function boundedJson(response, maxBytes = 65536) {
  if (!response.body) invalid('O provedor retornou uma resposta vazia.');
  const reader = response.body.getReader();
  const parts = []; let size = 0;
  try {
    while (true) {
      const { done, value } = await reader.read();
      if (done) break;
      size += value.length;
      if (size > maxBytes) invalid('A resposta do provedor não é uma autenticação Xtream válida.');
      parts.push(value);
    }
  } finally { await reader.cancel().catch(() => {}); }
  const bytes = new Uint8Array(size);let offset = 0;
  for (const part of parts) { bytes.set(part, offset);offset += part.length; }
  try { return JSON.parse(new TextDecoder().decode(bytes).replace(/^\uFEFF/, '')); }
  catch { invalid('O provedor não retornou dados Xtream válidos.'); }
}
async function validateXtreamLink(name, link, { fetcher = fetch, timeoutMs = 5000 } = {}) {
  const playlist = xtreamFromLink(name, link);
  let address = new URL(playlist.serverUrl + '/player_api.php');
  address.searchParams.set('username', playlist.username);address.searchParams.set('password', playlist.password);
  const controller = new AbortController();
  const timer = setTimeout(() => controller.abort(), timeoutMs);
  try {
    let response;
    for (let hop = 0; hop <= 3; hop++) {
      publicUrl(address.href);
      response = await fetcher(address.href, { redirect: 'manual', signal: controller.signal,
        headers: { Accept: 'application/json', 'User-Agent': 'IPTVPremium/1.0', 'Cache-Control': 'no-store' } });
      if (![301,302,303,307,308].includes(response.status)) break;
      const next = response.headers.get('Location');await response.body?.cancel();
      if (!next || hop === 3) invalid('O provedor está redirecionando sem concluir a validação. Confira o link.');
      address = publicUrl(new URL(next, address).href);
    }
    if ([401,403].includes(response.status)) { await response.body?.cancel();throw new PlaylistValidationError('Consulta Xtream recusada pelo provedor (HTTP ' + response.status + ').',422,'http',response.status); }
    if (!response.ok) { await response.body?.cancel();throw new PlaylistValidationError('API Xtream indisponível para o site (HTTP ' + response.status + ').',422,'http',response.status); }
    const body = await boundedJson(response);
    const info = body && !Array.isArray(body) && body.user_info;
    if (!info || typeof info !== 'object') invalid('Esse servidor não confirmou uma API Xtream compatível. Nada foi enviado ao aplicativo.');
    const auth = info.auth == null ? null : String(info.auth);
    const active = String(info.status || '').toLowerCase() === 'active';
    if (!(auth === '1' || auth === null && active) || info.status && !active) { throw new PlaylistValidationError('Usuário ou senha inválidos, ou a conta não está ativa.',422,'account'); }
    return { playlist, message: 'Xtream validado. Confira os dados e envie ao aplicativo.' };
  } catch (error) {
    if (error instanceof PlaylistValidationError) throw error;
    throw new PlaylistValidationError(controller.signal.aborted ? 'O provedor demorou para responder. Nada foi enviado. Você pode tentar novamente.' : 'Não foi possível acessar o provedor. Confira o link ou tente novamente.', 422, controller.signal.aborted ? 'timeout' : 'network');
  } finally { clearTimeout(timer); }
}

async function validateM3uLink(link, { fetcher, timeoutMs }) {
  let address = publicUrl(link);
  const controller = new AbortController();
  const timer = setTimeout(() => controller.abort(), timeoutMs);
  try {
    let response;
    for (let hop = 0; hop <= 3; hop++) {
      publicUrl(address.href);
      response = await fetcher(address.href, { redirect:'manual', signal:controller.signal,
        headers:{ Accept:'audio/x-mpegurl,application/vnd.apple.mpegurl,text/plain,*/*', Range:'bytes=0-65535', 'User-Agent':'IPTVPremium/1.0', 'Cache-Control':'no-store' } });
      if (![301,302,303,307,308].includes(response.status)) break;
      const next = response.headers.get('Location');await response.body?.cancel();
      if (!next || hop === 3) invalid('O link está redirecionando sem concluir. Confira com o provedor.');
      address = publicUrl(new URL(next, address).href);
    }
    if (!response.ok || !response.body) { await response.body?.cancel();throw new PlaylistValidationError('Consulta M3U recusada ou indisponível no site (HTTP ' + response.status + ').',422,'http',response.status); }
    const reader = response.body.getReader(), decoder = new TextDecoder();
    let prefix = '', bytes = 0;
    try {
      while (bytes < 65536) {
        const {done,value} = await reader.read();
        if (done) break;
        const take = value.subarray(0,65536-bytes);bytes += take.length;
        prefix += decoder.decode(take,{stream:true});
        const text = prefix.replace(/^\uFEFF/,'').trimStart().replace(/\r\n?/g,'\n');
        if (text.length > 8 && !text.startsWith('#EXTM3U')) invalid('A resposta não é uma lista M3U suportada.');
        if (/^#EXTM3U[^\r\n]*[\r\n]/.test(text) && /^#EXTINF[^\r\n]*[\r\n](?:#[^\r\n]*[\r\n])*[ \t]*(?:https?|rtsp|rtmp|udp):\/\/[^\s]+[\r\n]/m.test(text)) return;
      }
      // A last line without newline is also a complete entry at EOF.
      const text = (prefix + decoder.decode()).replace(/^\uFEFF/,'').trimStart().replace(/\r\n?/g,'\n');
      if (text.startsWith('#EXTM3U') && /^#EXTINF[^\r\n]*[\r\n](?:#[^\r\n]*[\r\n])*[ \t]*(?:https?|rtsp|rtmp|udp):\/\/[^\s]+(?:[\r\n]|$)/m.test(text)) return;
      invalid('Não encontramos uma entrada M3U válida no início da lista. Confira o link com o provedor.');
    } finally { await reader.cancel().catch(() => {}); }
  } catch(error) {
    if (error instanceof PlaylistValidationError) throw error;
    throw new PlaylistValidationError(controller.signal.aborted ? 'O provedor demorou para validar a lista. Nada foi enviado. Tente novamente.' : 'Não foi possível acessar a lista. Confira o link ou tente novamente.',422,controller.signal.aborted ? 'timeout' : 'network');
  } finally { clearTimeout(timer); }
}
function inconclusive(error) {
  return ['network','timeout'].includes(error?.kind) || error?.kind === 'http' &&
    ([403,408,429].includes(error.httpStatus) || error.httpStatus >= 500);
}
function pendingXtream(playlist, error) {
  return {playlist,validation:{state:'device_required',format:'xtream'},
    message:'Configuração Xtream identificada. O aplicativo concluirá a verificação do acesso.',
    diagnostics:error.message + ' A recusa da consulta do site não confirma incompatibilidade; a configuração Xtream será conferida no aparelho.'};
}

export async function validatePlaylistInput(input, options = {}) {
  if (!input || typeof input !== 'object' || Array.isArray(input)) invalid('Informe os dados da lista.');
  const format = input.format ?? 'm3u';
  if (format === 'm3u_direct') {
    if (options.allowDirectM3u !== true) throw new PlaylistValidationError('Opção disponível somente para o administrador.',403,'permission');
    if (typeof input.name !== 'string' || !input.name.trim() || input.name.trim().length > 80) invalid('Informe o nome da lista, com até 80 caracteres.');
    if (typeof input.link !== 'string' || !input.link.trim() || input.link.length > 4096) invalid('Informe o link da lista.');
    const link = input.link.trim().replace(/&amp;/gi,'&').replace(/\\&/g,'&');
    publicUrl(link);
    return {playlist:{type:'m3u',name:input.name.trim(),isEnabled:true,playlistUrl:link},
      validation:{state:'device_required',format:'m3u'},
      message:'M3U direta preparada, sem conversão. O aplicativo verificará o link ao importar.'};
  }
  if (format === 'm3u') return validatePlaylistLink(input.name, input.link, options);
  if (format !== 'xtream') invalid('Escolha M3U ou Xtream Codes.');
  if (typeof input.serverUrl !== 'string' || typeof input.username !== 'string' || typeof input.password !== 'string') invalid('Preencha servidor, usuário e senha.');
  const server = publicUrl(input.serverUrl.trim());
  if (server.search || server.hash) invalid('Informe somente o endereço do servidor, sem parâmetros.');
  server.pathname = server.pathname.replace(/\/+$/, '') + '/get.php';
  server.searchParams.set('username', input.username);server.searchParams.set('password', input.password);
  const candidate = xtreamFromLink(input.name, server.href);
  try {
    const result = await validateXtreamLink(input.name, server.href, options);
    return {...result,validation:{state:'verified',format:'xtream'}};
  } catch (error) {
    if (inconclusive(error)) return pendingXtream(candidate,error);
    throw error;
  }
}

export async function validatePlaylistLink(name, link, {fetcher=fetch, timeoutMs=12000}={}) {
  if (typeof name !== 'string' || !name.trim() || name.trim().length > 80) invalid('Informe o nome da lista, com até 80 caracteres.');
  if (typeof link !== 'string' || !link.trim() || link.length > 4096) invalid('Informe o link da lista.');
  const clean = link.trim().replace(/&amp;/gi,'&').replace(/\\&/g,'&');
  publicUrl(clean);
  const started = Date.now();
  let candidate = null, apiError = null;
  try { candidate = xtreamFromLink(name,clean); } catch(error) { if (!(error instanceof PlaylistValidationError)) throw error; }
  if (candidate) {
    try {
      const result = await validateXtreamLink(name,clean,{fetcher,timeoutMs:Math.min(5000,timeoutMs)});
      return {...result,validation:{state:'verified',format:'xtream'}};
    } catch(error) { if (!(error instanceof PlaylistValidationError)) throw error;apiError = error; }
    if (inconclusive(apiError)) return pendingXtream(candidate,apiError);
  }
  const remaining = timeoutMs - (Date.now()-started);
  let m3uError;
  try {
    if (remaining <= 0) throw new PlaylistValidationError('Tempo de consulta esgotado no site.',422,'timeout');
    await validateM3uLink(clean,{fetcher,timeoutMs:remaining});
    return { playlist:{type:'m3u',name:name.trim(),isEnabled:true,playlistUrl:clean},
      validation:{state:'verified',format:'m3u'},message:'M3U validada. Confira o link e envie ao aplicativo.' };
  } catch(error) { if (!(error instanceof PlaylistValidationError)) throw error;m3uError = error; }
  // A failed server-to-server request does not prove a broken link on the TV.
  // The user must explicitly choose device validation; never label it verified.
  if (apiError?.kind === 'account') throw apiError;
  if (!['http','network','timeout'].includes(m3uError.kind)) throw m3uError;
  // Only a confirmed API response is sent as Xtream. Unknown server-to-server
  // access falls back to the original M3U, to be checked on the device.
  let m3uUrl = clean;
  const parsed = new URL(clean);
  if (candidate && /\/player_api\.php$/i.test(parsed.pathname)) {
    parsed.pathname = parsed.pathname.replace(/player_api\.php$/i, 'get.php');
    parsed.searchParams.delete('action');
    parsed.searchParams.set('type', 'm3u_plus');
    parsed.searchParams.set('output', 'ts');
    m3uUrl = parsed.href;
  }
  const playlist = {type:'m3u',name:name.trim(),isEnabled:true,playlistUrl:m3uUrl};
  const reason = [apiError?.message,m3uError.message].filter(Boolean).join(' ');
  return {playlist,validation:{state:'device_required',format:'m3u'},
    message:'Lista preparada. Envie para concluir a configuração no aplicativo.',
    diagnostics:reason + ' A consulta do site não confirmou o provedor; M3U será conferida no aparelho.'};
}
