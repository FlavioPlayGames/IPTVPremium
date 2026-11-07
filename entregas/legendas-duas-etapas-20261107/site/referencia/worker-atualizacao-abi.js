import { openPortalReady, connectOpenAccount, OpenPortalError } from './opensubtitles.js';
import { validatePlaylistInput } from './playlist-validation.js';
var __defProp = Object.defineProperty;
var __name = (target2, value) => __defProp(target2, "name", { value, configurable: true });

// src/fcm.js
var encoder = new TextEncoder();
var cachedAccess;
var AuthenticationError = class extends Error {
  static {
    __name(this, "AuthenticationError");
  }
  constructor(code2, status = null) {
    super("FCM authentication");
    this.code = code2;
    this.status = status;
  }
};
function authenticationReason(body) {
  const error = typeof body?.error === "string" ? body.error : "";
  const description = typeof body?.error_description === "string" ? body.error_description.toLowerCase() : "";
  if (error === "invalid_grant") {
    if (description.includes("signature")) return "oauth_signature";
    if (description.includes("account not found") || description.includes("account has been deleted")) return "oauth_account";
    if (description.includes("disabled")) return "oauth_disabled";
    if (description.includes("short-lived") || description.includes("timeframe") || description.includes("iat")) return "oauth_clock";
    return "oauth_invalid_grant";
  }
  return { invalid_client: "oauth_client", unauthorized_client: "oauth_permission", access_denied: "oauth_permission", invalid_scope: "oauth_scope", temporarily_unavailable: "oauth_unavailable", server_error: "oauth_unavailable" }[error] || "oauth_rejected";
}
__name(authenticationReason, "authenticationReason");
function account(env) {
  if (!env.FCM_SERVICE_ACCOUNT) return null;
  const value = JSON.parse(env.FCM_SERVICE_ACCOUNT);
  if (!/^[a-z][a-z0-9-]{4,62}$/.test(value.project_id || "") || !/^[^\s@]+@[^\s@]+\.iam\.gserviceaccount\.com$/.test(value.client_email || "") || !String(value.private_key || "").includes("-----BEGIN PRIVATE KEY-----")) throw new Error("FCM configuration");
  return value;
}
__name(account, "account");
function configuredProject(env) {
  return account(env)?.project_id || null;
}
__name(configuredProject, "configuredProject");
function base64url(bytes) {
  return btoa(String.fromCharCode(...bytes)).replaceAll("+", "-").replaceAll("/", "_").replace(/=+$/, "");
}
__name(base64url, "base64url");
async function accessToken(env, service) {
  const cacheKey = env.FCM_SERVICE_ACCOUNT;
  if (cachedAccess?.key === cacheKey && cachedAccess.expires > Date.now() + 6e4) return cachedAccess.token;
  const now = Math.floor(Date.now() / 1e3);
  const header = base64url(encoder.encode(JSON.stringify({ alg: "RS256", typ: "JWT", .../^[a-f0-9]{40}$/i.test(service.private_key_id || "") ? { kid: service.private_key_id } : {} })));
  const body = base64url(encoder.encode(JSON.stringify({
    iss: service.client_email,
    scope: "https://www.googleapis.com/auth/firebase.messaging",
    aud: "https://oauth2.googleapis.com/token",
    iat: now,
    exp: now + 3600
  })));
  let signature;
  try {
    const pem = service.private_key.replaceAll("\\n", "\n").replace(/-----[^-]+-----/g, "").replace(/\s/g, "");
    const key = await crypto.subtle.importKey(
      "pkcs8",
      Uint8Array.from(atob(pem), (c) => c.charCodeAt(0)),
      { name: "RSASSA-PKCS1-v1_5", hash: "SHA-256" },
      false,
      ["sign"]
    );
    signature = await crypto.subtle.sign("RSASSA-PKCS1-v1_5", key, encoder.encode(header + "." + body));
  } catch {
    throw new AuthenticationError("oauth_key");
  }
  let response;
  try {
    response = await fetch("https://oauth2.googleapis.com/token", {
      method: "POST",
      redirect: "manual",
      headers: { "Content-Type": "application/x-www-form-urlencoded" },
      signal: AbortSignal.timeout(8e3),
      body: new URLSearchParams({ grant_type: "urn:ietf:params:oauth:grant-type:jwt-bearer", assertion: header + "." + body + "." + base64url(new Uint8Array(signature)) }).toString()
    });
  } catch {
    throw new AuthenticationError("oauth_network");
  }
  let result;
  try {
    result = await response.json();
  } catch {
    throw new AuthenticationError("oauth_response", response.status);
  }
  if (!response.ok) throw new AuthenticationError(authenticationReason(result), response.status);
  if (typeof result.access_token !== "string" || !result.access_token) throw new AuthenticationError("oauth_response", response.status);
  cachedAccess = { key: cacheKey, token: result.access_token, expires: Date.now() + Math.min(Number(result.expires_in) || 3600, 3600) * 1e3 };
  return result.access_token;
}
__name(accessToken, "accessToken");
async function notifyLists(env, deviceId, unseal2) {
  let row, phase = "configuration";
  async function finish(result, httpStatus = null) {
    if (row) await env.DB.prepare("UPDATE device_push SET last_result=?,last_http_status=? WHERE device_id=? AND token_hash=?").bind(result, httpStatus, deviceId, row.token_hash).run();
    return result;
  }
  __name(finish, "finish");
  try {
    const service = account(env);
    if (!service) return "disabled";
    row = await env.DB.prepare("SELECT * FROM device_push WHERE device_id=?").bind(deviceId).first();
    if (!row) return "unavailable";
    await env.DB.prepare("UPDATE device_push SET last_attempt_at=?,last_result='sending',last_http_status=NULL WHERE device_id=? AND token_hash=?").bind(Date.now(), deviceId, row.token_hash).run();
    if (row.project_id !== service.project_id) return await finish("project_mismatch");
    const token = await unseal2(env, row.token_cipher, deviceId + ":push");
    phase = "authentication";
    const authorization = await accessToken(env, service);
    phase = "send";
    const response = await fetch(`https://fcm.googleapis.com/v1/projects/${service.project_id}/messages:send`, {
      method: "POST",
      redirect: "manual",
      signal: AbortSignal.timeout(8e3),
      headers: { "Authorization": "Bearer " + authorization, "Content-Type": "application/json" },
      body: JSON.stringify({ message: {
        token,
        data: { type: "lists_changed", version: "1" },
        android: { priority: "NORMAL", ttl: "86400s", collapse_key: "portal_lists" }
      } })
    });
    if (response.ok) return await finish("accepted", response.status);
    if (response.status === 401) cachedAccess = void 0;
    let result;
    try {
      result = await response.json();
    } catch {
    }
    const code2 = result?.error?.details?.find((d) => d["@type"] === "type.googleapis.com/google.firebase.fcm.v1.FcmError")?.errorCode;
    if (code2 === "UNREGISTERED") {
      await env.DB.prepare("DELETE FROM device_push WHERE device_id=? AND token_hash=?").bind(deviceId, row.token_hash).run();
      return "unregistered";
    }
    return await finish(code2 === "SENDER_ID_MISMATCH" ? "sender_mismatch" : "failed", response.status);
  } catch (error) {
    try {
      return await finish(error instanceof AuthenticationError ? error.code : phase === "authentication" ? "authentication_failed" : phase === "send" ? "network_failed" : "failed", error instanceof AuthenticationError ? error.status : null);
    } catch {
      return "failed";
    }
  }
}
__name(notifyLists, "notifyLists");
async function pushDiagnostics(env, deviceId) {
  let configured = false;
  try {
    configured = !!account(env);
  } catch {
  }
  const row = await env.DB.prepare("SELECT updated_at,last_attempt_at,last_result,last_http_status,last_pull_at FROM device_push WHERE device_id=?").bind(deviceId).first();
  return {
    configured,
    registered: !!row,
    registeredAt: row?.updated_at || null,
    attemptedAt: row?.last_attempt_at || null,
    result: row?.last_result || null,
    httpStatus: row?.last_http_status || null,
    pulledAt: row?.last_pull_at || null
  };
}
__name(pushDiagnostics, "pushDiagnostics");

// src/pix.js
function field(id, value) {
  const n = new TextEncoder().encode(value).length;
  if (n > 99) throw Error("Campo Pix muito longo.");
  return id + String(n).padStart(2, "0") + value;
}
__name(field, "field");
function ascii(s) {
  return s.normalize("NFD").replace(/[\u0300-\u036f]/g, "").toUpperCase().replace(/[^A-Z0-9 .-]/g, "").trim();
}
__name(ascii, "ascii");
function buildPix({ key, name, city, amountCents, txid }) {
  if (typeof key !== "string" || !key.trim() || key.length > 77 || !/^[\x21-\x7E]+$/.test(key) || !Number.isSafeInteger(amountCents) || amountCents < 1 || amountCents > 1e7 || !/^[A-Z0-9]{1,25}$/.test(txid)) throw Error("Configura\xE7\xE3o Pix inv\xE1lida.");
  const merchant = ascii(name).slice(0, 25), location = ascii(city).slice(0, 15);
  if (!merchant || !location) throw Error("Dados do recebedor inv\xE1lidos.");
  const account3 = field("00", "BR.GOV.BCB.PIX") + field("01", key.trim());
  let payload = field("00", "01") + field("26", account3) + field("52", "0000") + field("53", "986") + field("54", (amountCents / 100).toFixed(2)) + field("58", "BR") + field("59", merchant) + field("60", location) + field("62", field("05", txid)) + "6304";
  let crc = 65535;
  for (const b of new TextEncoder().encode(payload)) {
    crc ^= b << 8;
    for (let i = 0; i < 8; i++) crc = crc & 32768 ? (crc << 1 ^ 4129) & 65535 : crc << 1 & 65535;
  }
  return payload + crc.toString(16).toUpperCase().padStart(4, "0");
}
__name(buildPix, "buildPix");

// src/worker.js
var utf8 = new TextEncoder();
var DAY = 864e5;
var QUEUE_TTL = 2 * DAY;
var SNAPSHOT_TTL = 5 * 6e4;
var COLORS = ["FFFFFF", "F2F2F2", "E5E5E5", "FFFF00", "FFD54F", "FFE991"];
var ApiError = class extends Error {
  static {
    __name(this, "ApiError");
  }
  constructor(status, message) {
    super(message);
    this.status = status;
  }
};
function fail(status, message) {
  throw new ApiError(status, message);
}
__name(fail, "fail");
var hex = /* @__PURE__ */ __name((bytes) => [...new Uint8Array(bytes)].map((b) => b.toString(16).padStart(2, "0")).join(""), "hex");
var random = /* @__PURE__ */ __name((n) => hex(crypto.getRandomValues(new Uint8Array(n))), "random");
var hash = /* @__PURE__ */ __name(async (s) => hex(await crypto.subtle.digest("SHA-256", utf8.encode(s))), "hash");
function equal(a, b) {
  if (typeof a !== "string" || typeof b !== "string") return false;
  let diff = a.length ^ b.length;
  for (let i = 0; i < Math.max(a.length, b.length); i++) diff |= (a.charCodeAt(i) || 0) ^ (b.charCodeAt(i) || 0);
  return diff === 0;
}
__name(equal, "equal");
var json = /* @__PURE__ */ __name((data, status = 200, headers = {}) => new Response(JSON.stringify(data), { status, headers: { "Content-Type": "application/json; charset=utf-8", ...headers } }), "json");
function macValue(value) {
  const m = String(value || "").replaceAll("-", ":").toUpperCase();
  return /^(?:[0-9A-F]{2}:){5}[0-9A-F]{2}$/.test(m) ? m : null;
}
__name(macValue, "macValue");
function virtualMac() {
  const b = crypto.getRandomValues(new Uint8Array(6));
  b[0] = b[0] & 252 | 2;
  return [...b].map((x) => x.toString(16).padStart(2, "0")).join(":").toUpperCase();
}
__name(virtualMac, "virtualMac");
function code() {
  let v;
  do {
    v = crypto.getRandomValues(new Uint32Array(1))[0];
  } while (v >= 4294e6);
  return String(v % 1e6).padStart(6, "0");
}
__name(code, "code");
async function cryptoKey(env, purpose, algorithm) {
  const raw = await crypto.subtle.digest("SHA-256", utf8.encode(purpose + ":" + env.APP_KEY));
  return crypto.subtle.importKey("raw", raw, algorithm, false, algorithm === "AES-GCM" ? ["encrypt", "decrypt"] : ["sign"]);
}
__name(cryptoKey, "cryptoKey");
async function codeHash(env, mac, value) {
  return hex(await crypto.subtle.sign("HMAC", await cryptoKey(env, "login", { name: "HMAC", hash: "SHA-256" }), utf8.encode(mac + ":" + value)));
}
__name(codeHash, "codeHash");
async function seal(env, value, id) {
  const iv = crypto.getRandomValues(new Uint8Array(12)), raw = await crypto.subtle.encrypt({ name: "AES-GCM", iv, additionalData: utf8.encode(id) }, await cryptoKey(env, "storage", "AES-GCM"), utf8.encode(JSON.stringify(value)));
  return hex(iv) + ":" + hex(raw);
}
__name(seal, "seal");
async function unseal(env, value, id) {
  const [iv, raw] = value.split(":").map((s) => Uint8Array.from(s.match(/../g) || [], (h) => parseInt(h, 16)));
  const clear = await crypto.subtle.decrypt({ name: "AES-GCM", iv, additionalData: utf8.encode(id) }, await cryptoKey(env, "storage", "AES-GCM"), raw);
  return JSON.parse(new TextDecoder().decode(clear));
}
__name(unseal, "unseal");
async function readBody(request) {
  const size = Number(request.headers.get("Content-Length") || 0);
  if (size > 262144) fail(413, "Dados muito grandes.");
  let n = 0;
  const chunks = [];
  if (request.body) {
    const reader = request.body.getReader();
    while (true) {
      const { done, value } = await reader.read();
      if (done) break;
      n += value.length;
      if (n > 262144) {
        await reader.cancel();
        fail(413, "Dados muito grandes.");
      }
      chunks.push(value);
    }
  }
  const raw = new Uint8Array(n);
  let offset = 0;
  for (const chunk of chunks) {
    raw.set(chunk, offset);
    offset += chunk.length;
  }
  let data;
  try {
    data = JSON.parse(new TextDecoder().decode(raw));
  } catch {
    fail(400, "Dados inv\xE1lidos.");
  }
  if (!data || Array.isArray(data) || typeof data !== "object") fail(400, "Dados inv\xE1lidos.");
  return data;
}
__name(readBody, "readBody");
function text(value, max, name, allowEmpty = false) {
  if (typeof value !== "string" || value.length > max || !allowEmpty && !value.trim() || /[\u0000-\u0008\u000b-\u001f]/.test(value)) fail(422, name + " inv\xE1lido.");
  return value.trim();
}
__name(text, "text");
function sourceUrl(value) {
  const v = text(value, 4096, "URL");
  let u;
  try {
    u = new URL(v);
  } catch {
    fail(422, "Informe uma URL completa.");
  }
  if (!["http:", "https:"].includes(u.protocol)) fail(422, "Use uma URL HTTP ou HTTPS.");
  return v;
}
__name(sourceUrl, "sourceUrl");
function playlist(b) {
  if (!["m3u", "xtream"].includes(b.type)) fail(422, "Tipo de lista inv\xE1lido.");
  const id = text(b.id, 120, "ID da lista");
  if (!/^[A-Za-z0-9_-]+$/.test(id)) fail(422, "ID da lista inv\xE1lido.");
  const r = { id, type: b.type, name: text(b.name, 80, "Nome"), isEnabled: b.isEnabled !== false };
  if (b.type === "m3u") r.playlistUrl = sourceUrl(b.playlistUrl);
  else {
    r.serverUrl = sourceUrl(b.serverUrl);
    r.username = text(b.username, 256, "Usu\xE1rio");
    if (typeof b.password !== "string" || !b.password.length || b.password.length > 512) fail(422, "Senha inv\xE1lida.");
    r.password = b.password;
    if (b.fallbackM3uUrl != null) r.fallbackM3uUrl = sourceUrl(b.fallbackM3uUrl);
  }
  return r;
}
__name(playlist, "playlist");
function style(b) {
  if (!b || !COLORS.includes(b.textColor) || !["NONE", "TRANSLUCENT", "OPAQUE"].includes(b.background) || ![18, 22, 26, 30].includes(b.sizeSp) || !["DEFAULT", "SERIF", "MONOSPACE"].includes(b.typeface) || !["NONE", "OUTLINE", "SHADOW"].includes(b.edge)) fail(422, "Estilo de legenda inv\xE1lido.");
  return { textColor: b.textColor, background: b.background, sizeSp: b.sizeSp, typeface: b.typeface, edge: b.edge };
}
__name(style, "style");
function originCheck(r) {
  if (r.headers.get("Origin") !== new URL(r.url).origin) fail(403, "Origem inv\xE1lida.");
}
__name(originCheck, "originCheck");
async function limit(env, key, max, seconds) {
  const now = Math.floor(Date.now() / 1e3), k = await hash(key + ":" + Math.floor(now / seconds));
  const r = await env.DB.prepare("INSERT INTO limits(key,count,expires) VALUES(?,1,?) ON CONFLICT(key) DO UPDATE SET count=count+1 RETURNING count").bind(k, now + seconds * 2).first();
  if (r.count > max) fail(429, "Muitas tentativas. Aguarde alguns minutos.");
}
__name(limit, "limit");
async function session(r, env, write = false) {
  const token = r.headers.get("Cookie")?.match(/(?:^|;\s*)iptv_session=([0-9a-f]{64})(?:;|$)/)?.[1];
  if (!token) fail(401, "Entre na sua conta.");
  const s = await env.DB.prepare("SELECT * FROM sessions WHERE token_hash=? AND expires>?").bind(await hash(token), Date.now()).first();
  if (!s) fail(401, "Sua sess\xE3o terminou. Entre novamente.");
  if (write) {
    originCheck(r);
    if (!equal(r.headers.get("X-CSRF-Token"), s.csrf)) fail(403, "Atualize a p\xE1gina e tente novamente.");
  }
  return s;
}
__name(session, "session");
async function newSession(r, env, role, id = null) {
  const token = random(32), csrf = random(24);
  await env.DB.prepare("INSERT INTO sessions VALUES(?,?,?,?,?)").bind(await hash(token), role, id, csrf, Date.now() + 8 * 36e5).run();
  return json({ role, csrf }, 200, { "Set-Cookie": `iptv_session=${token}; Path=/; HttpOnly; SameSite=Strict; Max-Age=28800${new URL(r.url).protocol === "https:" ? "; Secure" : ""}` });
}
__name(newSession, "newSession");
async function deviceAuth(r, env) {
  const token = r.headers.get("Authorization") || "";
  if (!/^Bearer [0-9a-f]{64}$/.test(token)) fail(401, "Credencial inv\xE1lida.");
  const d = await env.DB.prepare("SELECT * FROM devices WHERE credential_hash=?").bind(await hash(token.slice(7))).first();
  if (!d) fail(401, "Aparelho n\xE3o cadastrado.");
  return d;
}
__name(deviceAuth, "deviceAuth");
async function findDevice(env, id) {
  const d = await env.DB.prepare("SELECT * FROM devices WHERE id=?").bind(id).first();
  if (!d) fail(404, "Aparelho n\xE3o encontrado.");
  return d;
}
__name(findDevice, "findDevice");
function target(s, url) {
  if (s.role !== "admin") return s.device_id;
  const id = url.searchParams.get("device");
  if (!id) fail(400, "Selecione um aparelho.");
  return id;
}
__name(target, "target");
function license(d, now = Date.now()) {
  const until = Math.max(d.trial_until, d.license_until || 0);
  return { state: d.status === "blocked" ? "blocked" : (d.license_until || 0) > now ? "licensed" : d.trial_until > now ? "trial" : "expired", trialUntil: d.trial_until, licenseUntil: d.license_until, validUntil: until, serverNow: now };
}
__name(license, "license");
function grantUntil(start, duration, now = Date.now()) {
  const unit = duration?.unit || "years", value = duration?.value ?? 1;
  if (!Number.isSafeInteger(value)) fail(422, "Prazo inv\xE1lido.");
  if (unit === "until") {
    if (value <= now || value > now + 36600 * DAY) fail(422, "Escolha uma data futura v\xE1lida.");
    return value;
  }
  if (!["days", "months", "years"].includes(unit) || value < 1 || value > { days: 36500, months: 1200, years: 100 }[unit]) fail(422, "Prazo inv\xE1lido.");
  if (unit === "days") return start + value * DAY;
  const d = new Date(start), day = d.getUTCDate();
  d.setUTCDate(1);
  d.setUTCMonth(d.getUTCMonth() + value * (unit === "years" ? 12 : 1));
  const last = new Date(Date.UTC(d.getUTCFullYear(), d.getUTCMonth() + 1, 0)).getUTCDate();
  d.setUTCDate(Math.min(day, last));
  return d.getTime();
}
__name(grantUntil, "grantUntil");
async function renewalInfo(env, d, now = Date.now()) {
  const until = d.license_until || 0, days = until > now ? Math.ceil((until - now) / DAY) : 0;
  const stage = days > 0 && days <= 7 ? 7 : days <= 15 && days > 0 ? 15 : days <= 30 && days > 0 ? 30 : null;
  const row = until ? await env.DB.prepare("SELECT * FROM renewal_preferences WHERE device_id=? AND license_until=?").bind(d.id, until).first() : null;
  return { acquiredAt: d.license_acquired_at || null, expiresAt: until || null, daysRemaining: days, stage, noticeStage: d.status === "active" && stage && row?.acked_stage !== stage ? stage : null, choice: row?.choice || null, chosenAt: row?.chosen_at || null };
}
__name(renewalInfo, "renewalInfo");
async function saveRenewalChoice(env, d, b) {
  if (!["now", "due"].includes(b.choice) || ![30, 15, 7].includes(b.stage) || !Number.isSafeInteger(b.expiresAt)) fail(422, "Escolha de renova\xE7\xE3o inv\xE1lida.");
  const current = await renewalInfo(env, d);
  if (d.status !== "active" || current.stage !== b.stage || current.expiresAt !== b.expiresAt) fail(409, "A situa\xE7\xE3o da assinatura mudou. Atualize as informa\xE7\xF5es do aparelho.");
  const result = await env.DB.prepare("INSERT INTO renewal_preferences(device_id,license_until,choice,acked_stage,chosen_at) SELECT ?,?,?,?,? WHERE EXISTS(SELECT 1 FROM devices WHERE id=? AND license_until=? AND status='active') ON CONFLICT(device_id) DO UPDATE SET license_until=excluded.license_until,choice=excluded.choice,acked_stage=CASE WHEN renewal_preferences.license_until=excluded.license_until THEN min(renewal_preferences.acked_stage,excluded.acked_stage) ELSE excluded.acked_stage END,chosen_at=excluded.chosen_at").bind(d.id, b.expiresAt, b.choice, b.stage, Date.now(), d.id, b.expiresAt).run();
  if (!result.meta.changes) fail(409, "A assinatura mudou. Verifique novamente.");
  return renewalInfo(env, await findDevice(env, d.id));
}
__name(saveRenewalChoice, "saveRenewalChoice");
// A single existing preference row records acknowledgement; no schema migration or extra history.
async function trialNoticeInfo(env, d, now = Date.now()) {
  const current = license(d, now);
  if (d.status !== "active" || current.state === "licensed" || (current.state === "expired" && (d.license_until || 0) > 0)) return null;
  const until = d.trial_until || 0;
  if (!Number.isSafeInteger(until) || until <= 0) return null;
  const row = current.state === "trial" ? await env.DB.prepare("SELECT * FROM renewal_preferences WHERE device_id=? AND license_until=?").bind(d.id, until).first() : null;
  return { protocol: 1, expiresAt: until, acknowledged: current.state === "expired" || row?.acked_stage === 30 };
}
async function saveTrialChoice(env, d, body) {
  const now = Date.now();
  if (!["now", "due"].includes(body.choice) || !Number.isSafeInteger(body.expiresAt)) fail(422, "Escolha de ativação inválida.");
  if (license(d, now).state !== "trial" || d.status !== "active" || body.expiresAt !== d.trial_until) fail(409, "O período de teste mudou. Sincronize o aparelho.");
  // Stage 30 is a supported preference value; trial rows are distinguished by their expiry.
  // The guarded write cannot acknowledge a licensed/blocked device or overwrite a changed trial.
  const result = await env.DB.prepare("INSERT INTO renewal_preferences(device_id,license_until,choice,acked_stage,chosen_at) SELECT ?,?,?,30,? WHERE EXISTS(SELECT 1 FROM devices WHERE id=? AND trial_until=? AND trial_until>? AND coalesce(license_until,0)<=? AND status='active') ON CONFLICT(device_id) DO UPDATE SET license_until=excluded.license_until,choice=excluded.choice,acked_stage=excluded.acked_stage,chosen_at=excluded.chosen_at").bind(d.id, body.expiresAt, body.choice, now, d.id, body.expiresAt, now, now).run();
  if (!result.meta.changes) fail(409, "O acesso mudou. Sincronize o aparelho.");
  return trialNoticeInfo(env, await findDevice(env, d.id));
}
async function publicDevice(env, d, includeCode = false) {
  return { id: d.id, mac: d.mac, label: d.label, model: d.model, lastSeen: d.last_seen, createdAt: d.created_at, status: d.status, license: license(d), renewal: await renewalInfo(env, d), ...includeCode ? { code: await unseal(env, d.code_cipher, d.id), trialNotice: await trialNoticeInfo(env, d) } : {} };
}
__name(publicDevice, "publicDevice");
async function audit(env, id, action) {
  await env.DB.prepare("INSERT INTO audit(device_id,action,at) VALUES(?,?,?)").bind(id, action, Date.now()).run();
}
__name(audit, "audit");
async function expireDevice(env, id) {
  const now = Date.now();
  await env.DB.batch([env.DB.prepare("UPDATE commands SET cipher=NULL,state='expired',finished_at=? WHERE device_id=? AND state IN ('pending','delivering') AND expires<=?").bind(now, id, now), env.DB.prepare("DELETE FROM snapshots WHERE device_id=? AND expires<=?").bind(id, now)]);
}
__name(expireDevice, "expireDevice");
async function queue(env, id, kind, payload, requestId, ttl = QUEUE_TTL) {
  if (!/^[0-9a-f-]{36}$/.test(requestId || "")) fail(422, "Identifica\xE7\xE3o de envio inv\xE1lida.");
  const existing = await env.DB.prepare("SELECT device_id,kind,state FROM commands WHERE id=?").bind(requestId).first();
  if (existing) {
    if (existing.device_id !== id || existing.kind !== kind) fail(409, "Identifica\xE7\xE3o de envio j\xE1 utilizada.");
    return { id: requestId, state: existing.state };
  }
  const cipher = await seal(env, payload, id), now = Date.now();
  const result = await env.DB.prepare("INSERT OR IGNORE INTO commands(id,device_id,kind,cipher,created_at,expires) SELECT ?,?,?,?,?,? WHERE (SELECT count(*) FROM commands WHERE device_id=? AND state IN ('pending','delivering') AND expires>?)<20").bind(requestId, id, kind, cipher, now, now + Math.min(QUEUE_TTL, ttl), id, now).run();
  if (!result.meta.changes) {
    const race = await env.DB.prepare("SELECT state FROM commands WHERE id=? AND device_id=?").bind(requestId, id).first();
    if (race) return { id: requestId, state: race.state };
    fail(429, "H\xE1 muitos envios pendentes. Aguarde o aparelho receber.");
  }
  return { id: requestId, state: "pending" };
}
__name(queue, "queue");
const updateAbis = ["armeabi-v7a", "arm64-v8a", "x86", "x86_64"];
function validateUpdateArtifact(b) {
  if (!b || typeof b !== "object" || Array.isArray(b)) fail(422, "Dados do APK inválidos.");
  if (!Number.isSafeInteger(b.sizeBytes) || b.sizeBytes < 1 || b.sizeBytes > 512 * 1024 * 1024) fail(422, "Informe o tamanho do APK em bytes (até 512 MB).");
  const url = text(b.url, 4096, "Link do APK");
  let parsed;
  try { parsed = new URL(url); } catch { fail(422, "Link do APK inválido."); }
  if (parsed.protocol !== "https:" || parsed.username || parsed.password || parsed.hash) fail(422, "Use um link HTTPS direto do APK, sem usuário, senha ou fragmento.");
  if (typeof b.sha256 !== "string" || !/^[a-fA-F0-9]{64}$/.test(b.sha256)) fail(422, "SHA-256 inválido. Selecione o APK para calcular.");
  return { url, sha256: b.sha256.toLowerCase(), sizeBytes: b.sizeBytes };
}
function validateUpdate(b) {
  if (!Number.isSafeInteger(b.versionCode) || b.versionCode < 1 || b.versionCode > 2100000000) fail(422, "Número interno de versão inválido.");
  const universal = validateUpdateArtifact(b);
  const variants = Object.create(null);
  if (Object.hasOwn(b, "variants")) {
    if (!b.variants || typeof b.variants !== "object" || Array.isArray(b.variants)) fail(422, "Variantes de APK inválidas.");
    for (const abi of Object.keys(b.variants)) {
      if (!updateAbis.includes(abi)) fail(422, "Arquitetura de APK inválida.");
      const artifact = b.variants[abi];
      if (Object.keys(artifact || {}).some(key => !["url", "sha256", "sizeBytes"].includes(key))) fail(422, "Os APKs devem compartilhar a versão informada acima.");
      variants[abi] = validateUpdateArtifact(artifact);
    }
  }
  const value = { versionCode: b.versionCode, versionName: text(b.versionName, 40, "Nome da versão"), ...universal,
    sha256: universal.sha256, notes: text(b.notes || "", 500, "Descrição", true), mandatory: false, publishedAt: Date.now(),
    ...(Object.keys(variants).length ? { variants } : {}) };
  // Older Android clients also read this object and have a 16 KiB limit.
  if (utf8.encode(JSON.stringify(value)).length > 16384) fail(422, "Os links desta publicação são muito longos. Use links diretos menores.");
  return value;
}
__name(validateUpdate, "validateUpdate");
async function appUpdate(env) {
  const row = await env.DB.prepare("SELECT value FROM settings WHERE key='app_update'").first();
  return row ? { ...JSON.parse(row.value), mandatory: false } : null;
}
__name(appUpdate, "appUpdate");
// Old clients only understand blocking updates. Returning null also clears their cached demand.
async function appUpdateForClient(request, env) {
  if (request.headers.get("X-IPTV-Update-Protocol") !== "2") return null;
  return appUpdate(env);
}
async function pixSettings(env) {
  const row = await env.DB.prepare("SELECT value FROM settings WHERE key='pix'").first();
  return row ? unseal(env, row.value, "settings") : null;
}
__name(pixSettings, "pixSettings");
async function account2(env, id, admin = false) {
  await expireDevice(env, id);
  const d = await findDevice(env, id), row = await env.DB.prepare("SELECT cipher,received_at FROM snapshots WHERE device_id=? AND expires>?").bind(id, Date.now()).first();
  const cmds = await env.DB.prepare("SELECT id,kind,state,result,created_at,cipher FROM commands WHERE device_id=? ORDER BY seq DESC LIMIT 30").bind(id).all();
  return { ...await publicDevice(env, d, admin), ...admin ? { push: await pushDiagnostics(env, id) } : {}, renewal: { ...await renewalInfo(env, d), amountCents: (await pixSettings(env))?.amountCents || null }, snapshot: row ? { ...await unseal(env, row.cipher, id), receivedAt: row.received_at } : null, commands: await Promise.all(cmds.results.map(async (row2) => {
    const { cipher, ...item } = row2;
    return { ...item, ...row2.kind === "subtitle_session" ? { provider: "opensubtitles" } : row2.kind === "subtitle_key" && cipher ? { provider: (await unseal(env, cipher, id)).provider } : {} };
  })) };
}
__name(account2, "account");
function normalizeSnapshot(b) {
  if (!Array.isArray(b.lists) || b.lists.length > 30) fail(422, "Listas inv\xE1lidas.");
  const lists = b.lists.map(playlist);
  if (new Set(lists.map((x) => x.id)).size !== lists.length) fail(422, "Listas duplicadas.");
  return { lists, subtitles: style(b.subtitles), subtitleProtocol: b.subtitleProtocol===2 ? 2 : 1,
    openSubtitlesFingerprint: /^[a-f0-9]{64}$/.test(b.openSubtitlesFingerprint||"") ? b.openSubtitlesFingerprint : "",
    providers: { subdl: !!b.providers?.subdl, subsource: false, opensubtitles: !!b.providers?.opensubtitles } };
}
__name(normalizeSnapshot, "normalizeSnapshot");
function deviceFilter(q = "", status = "all", now = Date.now()) {
  if (!["all", "active", "licensed", "trial", "expired", "blocked", "due30", "due15", "due7", "renew_due"].includes(status)) fail(422, "Filtro de situa\xE7\xE3o inv\xE1lido.");
  q = String(q).slice(0, 80);
  const like = "%" + q.replace(/[\\%_]/g, "\\$&") + "%";
  const search = "(mac LIKE ? ESCAPE '\\' OR label LIKE ? ESCAPE '\\' OR replace(mac,':','') LIKE ? ESCAPE '\\' OR EXISTS(SELECT 1 FROM payments p WHERE p.device_id=devices.id AND p.txid LIKE ? ESCAPE '\\'))";
  const conditions = { all: "1=1", blocked: "status='blocked'", active: "status='active' AND (trial_until>? OR coalesce(license_until,0)>?)", licensed: "status='active' AND coalesce(license_until,0)>?", trial: "status='active' AND coalesce(license_until,0)<=? AND trial_until>?", expired: "status='active' AND coalesce(license_until,0)<=? AND trial_until<=?" };
  conditions.due30 = "status='active' AND license_until>? AND license_until<=?";
  conditions.due15 = conditions.due30;
  conditions.due7 = conditions.due30;
  conditions.renew_due = "license_until>? AND EXISTS(SELECT 1 FROM renewal_preferences rp WHERE rp.device_id=devices.id AND rp.license_until=devices.license_until AND rp.choice='due')";
  const times = status === "licensed" || status === "renew_due" ? [now] : ["active", "trial", "expired"].includes(status) ? [now, now] : ["due30", "due15", "due7"].includes(status) ? [now, now + Number(status.slice(3)) * DAY] : [];
  return { where: "WHERE " + search + " AND (" + conditions[status] + ")", args: [like, like, like, like, ...times] };
}
__name(deviceFilter, "deviceFilter");
async function api(r, env, ctx) {
  const u = new URL(r.url), p = u.pathname, m = r.method, ip = r.headers.get("CF-Connecting-IP") || "local";
  if (p === "/api/app/update" && m === "GET") return json({ update: await appUpdateForClient(r, env) });
  if (p === "/api/health" && m === "GET") {
    await env.DB.prepare("SELECT id FROM devices LIMIT 1").first();
    return json({ status: "ok" });
  }
  if (p === "/api/login" && m === "POST") {
    originCheck(r);
    await limit(env, "login-ip:" + ip, 20, 900);
    const b = await readBody(r), mac = macValue(b.mac);
    if (!mac || !/^\d{6}$/.test(b.code || "")) fail(401, "MAC ou c\xF3digo incorreto.");
    await limit(env, "login-mac:" + mac, 8, 900);
    const d2 = await env.DB.prepare("SELECT id,code_hash FROM devices WHERE mac=?").bind(mac).first(), h = await codeHash(env, mac, b.code);
    if (!d2 || !equal(h, d2.code_hash)) fail(401, "MAC ou c\xF3digo incorreto.");
    return newSession(r, env, "user", d2.id);
  }
  if (p === "/api/device/qr-login" && m === "POST") {
    const d2 = await deviceAuth(r, env);
    await limit(env, "qr-create:" + d2.id, 12, 60);
    const token = random(32), expires = Date.now() + 15 * 6e4;
    await env.DB.prepare("INSERT INTO qr_logins VALUES(?,?,?,?) ON CONFLICT(device_id) DO UPDATE SET token_hash=excluded.token_hash,code_hash=excluded.code_hash,expires=excluded.expires").bind(await hash(token), d2.id, d2.code_hash, expires).run();
    return json({ url: u.origin + "/#access=" + token, expires });
  }
  if (p === "/api/qr-login" && m === "POST") {
    originCheck(r);
    await limit(env, "qr-login:" + ip, 30, 900);
    const b = await readBody(r);
    if (!/^[0-9a-f]{64}$/.test(b.token || "")) fail(401, "QR Code inv\xE1lido ou expirado. Gere outro no aplicativo.");
    const row = await env.DB.prepare("DELETE FROM qr_logins WHERE token_hash=? AND expires>? AND EXISTS(SELECT 1 FROM devices WHERE devices.id=qr_logins.device_id AND devices.code_hash=qr_logins.code_hash) RETURNING device_id").bind(await hash(b.token), Date.now()).first();
    if (!row) fail(401, "QR Code inv\xE1lido, j\xE1 utilizado ou expirado. Gere outro no aplicativo.");
    return newSession(r, env, "user", row.device_id);
  }
  if (p === "/api/admin/login" && m === "POST") {
    originCheck(r);
    await limit(env, "admin:" + ip, 5, 900);
    const b = await readBody(r);
    if (typeof b.password !== "string" || !equal(await hash(b.password), await hash(env.ADMIN_PASSWORD))) fail(401, "Senha incorreta.");
    return newSession(r, env, "admin");
  }
  if (p === "/api/session" && m === "GET") {
    const s2 = await session(r, env);
    return json({ role: s2.role, csrf: s2.csrf });
  }
  if (p === "/api/logout" && m === "POST") {
    const s2 = await session(r, env, true);
    await env.DB.prepare("DELETE FROM sessions WHERE token_hash=?").bind(s2.token_hash).run();
    return json({ ok: true }, 200, { "Set-Cookie": "iptv_session=; Path=/; HttpOnly; SameSite=Strict; Max-Age=0" });
  }
  if (p === "/api/device/register" && m === "POST") {
    await limit(env, "register:" + ip, 60, 3600);
    const b = await readBody(r);
    if (!/^[0-9a-f]{64}$/.test(b.secret || "")) fail(422, "Credencial inv\xE1lida.");
    if (b.recoverySecret !== void 0 && !/^[0-9a-f]{64}$/.test(b.recoverySecret)) fail(422, "Credencial de recupera\xE7\xE3o inv\xE1lida.");
    const credential = await hash(b.secret), recovery = b.recoverySecret ? await hash("reinstall:" + b.recoverySecret) : null;
    let d2 = await env.DB.prepare("SELECT * FROM devices WHERE credential_hash=?").bind(credential).first();
    if (d2) {
      if (recovery) {
        if (d2.recovery_hash && d2.recovery_hash !== recovery) fail(409, "A identifica\xE7\xE3o de recupera\xE7\xE3o deste aparelho mudou. Contate o administrador.");
        const bound = await env.DB.prepare("SELECT id FROM devices WHERE recovery_hash=?").bind(recovery).first();
        if (bound && bound.id !== d2.id) fail(409, "Esta TV j\xE1 possui outro cadastro. Contate o administrador para preservar o cadastro correto.");
        await env.DB.prepare("UPDATE devices SET recovery_hash=? WHERE id=? AND (recovery_hash IS NULL OR recovery_hash=?) AND NOT EXISTS(SELECT 1 FROM devices WHERE recovery_hash=? AND id!=?)").bind(recovery, d2.id, recovery, recovery, d2.id).run();
        const check = await findDevice(env, d2.id);
        if (check.recovery_hash !== recovery) fail(409, "Esta TV j\xE1 est\xE1 vinculada a outro cadastro. Contate o administrador.");
      }
      return json(await publicDevice(env, d2, true));
    }
    if (!recovery) fail(426, "Atualize o aplicativo para cadastrar a identifica\xE7\xE3o permanente da TV.");
    const restore = /* @__PURE__ */ __name(async () => {
      if (!recovery) return null;
      const previous = await env.DB.prepare("SELECT * FROM devices WHERE recovery_hash=?").bind(recovery).first();
      if (!previous) return null;
      await env.DB.prepare("UPDATE devices SET credential_hash=?,last_seen=? WHERE id=? AND recovery_hash=?").bind(credential, Date.now(), previous.id, recovery).run();
      await env.DB.batch([env.DB.prepare("DELETE FROM sessions WHERE device_id=?").bind(previous.id), env.DB.prepare("DELETE FROM qr_logins WHERE device_id=?").bind(previous.id)]);
      return publicDevice(env, await findDevice(env, previous.id), true);
    }, "restore");
    const restored = await restore();
    if (restored) return json(restored);
    const model = text(b.model || b.label || "Aparelho Android", 80, "Modelo");
    for (let i = 0; i < 16; i++) {
      const proposed = i === 0 ? macValue(b.proposedMac) : null, mac = proposed && (parseInt(proposed.slice(0, 2), 16) & 3) === 2 ? proposed : virtualMac(), id2 = crypto.randomUUID(), value = code(), now = Date.now();
      await env.DB.prepare("INSERT OR IGNORE INTO devices(id,credential_hash,mac,code_hash,code_cipher,label,model,created_at,trial_until,last_seen,recovery_hash) VALUES(?,?,?,?,?,?,?,?,?,?,?)").bind(id2, credential, mac, await codeHash(env, mac, value), await seal(env, value, id2), model, model, now, now + 7 * DAY, now, recovery).run();
      d2 = await env.DB.prepare("SELECT * FROM devices WHERE credential_hash=?").bind(credential).first();
      if (d2) return json(await publicDevice(env, d2, true));
      const raced = await restore();
      if (raced) return json(raced);
    }
    fail(503, "N\xE3o foi poss\xEDvel registrar. Tente novamente.");
  }
  if (p === "/api/device/push-token" && m === "PUT") {
    const d2 = await deviceAuth(r, env);
    await limit(env, "push-token:" + d2.id, 20, 60);
    const b = await readBody(r);
    const token = text(b.token, 4096, "Token");
    if (token.length < 20 || !/^[A-Za-z0-9_:\-]+$/.test(token)) fail(422, "Token inv\xE1lido.");
    const project = text(b.projectId, 63, "Projeto");
    if (!/^[a-z][a-z0-9-]{4,62}$/.test(project)) fail(422, "Projeto inv\xE1lido.");
    let expected;
    try {
      expected = configuredProject(env);
    } catch {
      fail(503, "Firebase aguardando configura\xE7\xE3o.");
    }
    if (expected && project !== expected) fail(422, "Projeto Firebase diferente da central.");
    const tokenHash = await hash(token), owner = await env.DB.prepare("SELECT device_id FROM device_push WHERE token_hash=?").bind(tokenHash).first();
    if (owner && owner.device_id !== d2.id) fail(409, "Token j\xE1 vinculado a outro aparelho.");
    await env.DB.prepare("INSERT INTO device_push(device_id,token_hash,token_cipher,project_id,updated_at) VALUES(?,?,?,?,?) ON CONFLICT(device_id) DO UPDATE SET token_hash=excluded.token_hash,token_cipher=excluded.token_cipher,project_id=excluded.project_id,updated_at=excluded.updated_at").bind(d2.id, tokenHash, await seal(env, token, d2.id + ":push"), project, Date.now()).run();
    return json({ ok: true });
  }
  if (p === "/api/device/pull" && m === "GET") {
    const d2 = await deviceAuth(r, env), automatic = u.searchParams.get("mode") === "auto";
    await expireDevice(env, d2.id);
    await env.DB.prepare("UPDATE devices SET last_seen=? WHERE id=?").bind(Date.now(), d2.id).run();
    await env.DB.prepare("UPDATE device_push SET last_pull_at=? WHERE device_id=?").bind(Date.now(), d2.id).run();
    const subset = automatic ? " AND kind IN ('list_upsert','list_delete','snapshot')" : "";
    const rows = await env.DB.prepare("SELECT id,kind,cipher FROM commands WHERE device_id=? AND state IN ('pending','delivering') AND expires>?" + subset + " ORDER BY seq LIMIT 20").bind(d2.id, Date.now()).all();
    if (rows.results.length) await env.DB.prepare("UPDATE commands SET state='delivering' WHERE device_id=? AND id IN (" + rows.results.map(() => "?").join(",") + ") AND state='pending'").bind(d2.id, ...rows.results.map((x) => x.id)).run();
    const info = await publicDevice(env, d2, true), pix = await pixSettings(env);
    return json({ ...info, renewal: { ...info.renewal, amountCents: pix?.amountCents || null }, syncPolicyVersion: 2, syncMode: automatic ? "lists" : "all", appUpdate: await appUpdateForClient(r, env), commands: await Promise.all(rows.results.map(async (x) => ({ id: x.id, kind: x.kind, data: await unseal(env, x.cipher, d2.id) }))) });
  }
  if (p === "/api/device/trial-choice" && m === "POST") {
    const device = await deviceAuth(r, env);
    await limit(env, "trial-choice:" + device.id, 20, 60);
    return json({ trialNotice: await saveTrialChoice(env, device, await readBody(r)) });
  }
  if (p === "/api/device/renewal-choice" && m === "POST") {
    const d2 = await deviceAuth(r, env);
    await limit(env, "renewal:" + d2.id, 20, 60);
    return json({ renewal: await saveRenewalChoice(env, d2, await readBody(r)) });
  }
  if (p === "/api/device/ack" && m === "POST") {
    const d2 = await deviceAuth(r, env);
    await limit(env, "ack:" + d2.id, 60, 60);
    const b = await readBody(r);
    if (!Array.isArray(b.results) || b.results.length > 20) fail(422, "Confirma\xE7\xF5es inv\xE1lidas.");
    const statements = [];
    for (const x of b.results) {
      if (!/^[0-9a-f-]{36}$/.test(x.id || "") || typeof x.success !== "boolean") fail(422, "Confirma\xE7\xE3o inv\xE1lida.");
      const result = x.success ? "received" : ["invalid_key", "invalid_data", "storage_error", "not_found"].includes(x.error) ? x.error : "not_applied";
      statements.push(env.DB.prepare("UPDATE commands SET cipher=NULL,state=?,result=?,finished_at=? WHERE id=? AND device_id=? AND state IN ('pending','delivering','expired')").bind(x.success ? "received" : "failed", result, Date.now(), x.id, d2.id));
    }
    if (b.snapshot) {
      const payload = normalizeSnapshot(b.snapshot);
      const recent = await env.DB.prepare("SELECT id FROM commands WHERE device_id=? AND created_at>? LIMIT 1").bind(d2.id, Date.now() - 10 * 6e4).first();
      if (recent) statements.push(env.DB.prepare("INSERT INTO snapshots VALUES(?,?,?,?) ON CONFLICT(device_id) DO UPDATE SET cipher=excluded.cipher,received_at=excluded.received_at,expires=excluded.expires").bind(d2.id, await seal(env, payload, d2.id), Date.now(), Date.now() + SNAPSHOT_TTL));
    }
    if (statements.length) await env.DB.batch(statements);
    return json({ ok: true, serverNow: Date.now() });
  }
  const s = await session(r, env, ["POST", "PUT", "PATCH", "DELETE"].includes(m));
  if (p === "/api/admin/update" && ["GET", "PUT", "DELETE"].includes(m)) {
    if (s.role !== "admin") fail(403, "Acesso reservado ao administrador.");
    if (m === "GET") return json({ update: await appUpdate(env), updateCapabilities: { manifestVersion: 2, optionalProtocol: 2, abis: updateAbis, universalRequired: true } });
    if (m === "DELETE") {
      await env.DB.prepare("DELETE FROM settings WHERE key='app_update'").run();
      return json({ update: null });
    }
    const value = validateUpdate(await readBody(r));
    await env.DB.prepare("INSERT INTO settings VALUES('app_update',?) ON CONFLICT(key) DO UPDATE SET value=excluded.value").bind(JSON.stringify(value)).run();
    return json({ update: value });
  }
  if (p === "/api/admin/devices" && m === "GET") {
    if (s.role !== "admin") fail(403, "Acesso reservado ao administrador.");
    const filter = deviceFilter(u.searchParams.get("q") || "", u.searchParams.get("status") || "all"), page = Math.max(1, parseInt(u.searchParams.get("page") || "1") || 1);
    const rows = await env.DB.prepare("SELECT * FROM devices " + filter.where + " ORDER BY created_at DESC,id LIMIT 50 OFFSET ?").bind(...filter.args, (page - 1) * 50).all();
    const total = await env.DB.prepare("SELECT count(*) n FROM devices " + filter.where).bind(...filter.args).first(), counts = await env.DB.prepare("SELECT count(*) total,sum(CASE WHEN status='blocked' THEN 1 ELSE 0 END) blocked,sum(CASE WHEN status='active' AND (trial_until>? OR coalesce(license_until,0)>?) THEN 1 ELSE 0 END) active FROM devices").bind(Date.now(), Date.now()).first();
    return json({ items: await Promise.all(rows.results.map((d2) => publicDevice(env, d2))), total: total.n, page, counts });
  }
  if (p === "/api/admin/devices/selection" && m === "GET") {
    if (s.role !== "admin") fail(403, "Acesso reservado ao administrador.");
    const filter = deviceFilter(u.searchParams.get("q") || "", u.searchParams.get("status") || "all");
    const rows = await env.DB.prepare("SELECT id FROM devices " + filter.where + " ORDER BY id LIMIT 5001").bind(...filter.args).all();
    if (rows.results.length > 5e3) fail(422, "Selecione grupos de at\xE9 5000 aparelhos.");
    return json({ ids: rows.results.map((d2) => d2.id) });
  }
  if (p === "/api/admin/devices/actions-preview" && m === "POST") {
    if (s.role !== "admin") fail(403, "Acesso reservado ao administrador.");
    await limit(env, "bulk:" + s.token_hash, 10, 60);
    const b = await readBody(r);
    if (!["block", "unblock", "renew_license", "renew_trial"].includes(b.action)) fail(422, "A\xE7\xE3o inv\xE1lida.");
    if (!Array.isArray(b.ids) || !b.ids.length || b.ids.length > 5e3 || b.ids.some((id2) => typeof id2 !== "string" || !/^[0-9a-f-]{36}$/.test(id2))) fail(422, "Selecione de 1 a 5000 aparelhos.");
    const rows = await env.DB.prepare("SELECT id,mac,label FROM devices WHERE id IN (SELECT value FROM json_each(?)) ORDER BY id").bind(JSON.stringify([...new Set(b.ids)])).all();
    if (!rows.results.length) fail(404, "Nenhum aparelho selecionado foi encontrado.");
    const token = random(32), expires = Date.now() + 5 * 6e4;
    await env.DB.batch([env.DB.prepare("DELETE FROM bulk_action_previews WHERE session_hash=?").bind(s.token_hash), env.DB.prepare("INSERT INTO bulk_action_previews VALUES(?,?,?,?,?)").bind(await hash(token), s.token_hash, b.action, JSON.stringify(rows.results.map((d2) => d2.id)), expires)]);
    return json({ token, action: b.action, count: rows.results.length, examples: rows.results.slice(0, 5).map((d2) => ({ mac: d2.mac, label: d2.label })), expires });
  }
  if (p === "/api/admin/devices/actions-apply" && m === "POST") {
    if (s.role !== "admin") fail(403, "Acesso reservado ao administrador.");
    const b = await readBody(r);
    if (!/^[0-9a-f]{64}$/.test(b.token || "")) fail(422, "Confirma\xE7\xE3o inv\xE1lida.");
    const token = await hash(b.token), now = Date.now();
    const preview = await env.DB.prepare("SELECT action,ids FROM bulk_action_previews WHERE token_hash=? AND session_hash=? AND expires>?").bind(token, s.token_hash, now).first();
    if (!preview) fail(410, "A confirma\xE7\xE3o expirou ou j\xE1 foi utilizada. Selecione os aparelhos novamente.");
    const valid = "EXISTS(SELECT 1 FROM bulk_action_previews WHERE token_hash=? AND session_hash=? AND expires>?)";
    const ids = "SELECT value FROM json_each((SELECT ids FROM bulk_action_previews WHERE token_hash=? AND session_hash=? AND expires>?))";
    let update, grants = [];
    if (preview.action === "renew_license") {
      const rows = await env.DB.prepare("SELECT id,coalesce(license_until,0) old FROM devices WHERE id IN (SELECT value FROM json_each(?))").bind(preview.ids).all();
      const values = JSON.stringify(rows.results.map((d2) => ({ id: d2.id, old: d2.old, until: grantUntil(Math.max(now, d2.old), null, now) })));
      update = env.DB.prepare("UPDATE devices SET license_acquired_at=coalesce(license_acquired_at,?),license_until=(SELECT json_extract(value,'$.until') FROM json_each(?) WHERE json_extract(value,'$.id')=devices.id),release_kind='manual',release_at=?,release_reference='Renova\xE7\xE3o pelo menu A\xE7\xF5es',release_request=?||id WHERE id IN (" + ids + ") AND " + valid + " AND coalesce(license_until,0)=(SELECT json_extract(value,'$.old') FROM json_each(?) WHERE json_extract(value,'$.id')=devices.id)").bind(now, values, now, token, token, s.token_hash, now, token, s.token_hash, now, values);
      grants = [env.DB.prepare("INSERT OR IGNORE INTO license_grants SELECT release_request,id,license_until,?,'manual','Renova\xE7\xE3o pelo menu A\xE7\xF5es' FROM devices WHERE release_request=?||id AND " + valid).bind(now, token, token, s.token_hash, now)];
    } else if (preview.action === "renew_trial") update = env.DB.prepare("UPDATE devices SET trial_until=? WHERE id IN (" + ids + ")").bind(now + 7 * DAY, token, s.token_hash, now);
    else update = env.DB.prepare("UPDATE devices SET status=? WHERE id IN (" + ids + ")").bind(preview.action === "block" ? "blocked" : "active", token, s.token_hash, now);
    const results = await env.DB.batch([update, ...grants, env.DB.prepare("INSERT INTO audit(device_id,action,at) SELECT id,?,? FROM devices WHERE id IN (" + ids + ")").bind("bulk:" + preview.action, now, token, s.token_hash, now), env.DB.prepare("DELETE FROM bulk_action_previews WHERE token_hash=? AND session_hash=?").bind(token, s.token_hash)]);
    return json({ action: preview.action, updated: results[0].meta.changes });
  }
  if (p === "/api/admin/devices/deletion-preview" && m === "POST") {
    if (s.role !== "admin") fail(403, "Acesso reservado ao administrador.");
    await limit(env, "deletion:" + s.token_hash, 10, 60);
    const b = await readBody(r);
    if (!["selected", "filtered", "all"].includes(b.scope)) fail(422, "Tipo de exclus\xE3o inv\xE1lido.");
    let rows;
    if (b.scope === "selected") {
      if (!Array.isArray(b.ids) || b.ids.length < 1 || b.ids.length > 5e3 || b.ids.some((id2) => typeof id2 !== "string" || !/^[0-9a-f-]{36}$/.test(id2))) fail(422, "Selecione de 1 a 5000 aparelhos.");
      rows = await env.DB.prepare("SELECT id,mac,label FROM devices WHERE id IN (SELECT value FROM json_each(?)) ORDER BY id").bind(JSON.stringify([...new Set(b.ids)])).all();
    } else {
      const filter = b.scope === "all" ? { where: "", args: [] } : deviceFilter(b.q || "", b.status || "all");
      rows = await env.DB.prepare("SELECT id,mac,label FROM devices " + filter.where + " ORDER BY id LIMIT 5001").bind(...filter.args).all();
    }
    if (!rows.results.length) fail(404, "Nenhum aparelho corresponde \xE0 exclus\xE3o.");
    if (rows.results.length > 5e3) fail(422, "Exclua em grupos de at\xE9 5000 aparelhos.");
    const token = random(32), expires = Date.now() + 5 * 6e4;
    await env.DB.prepare("DELETE FROM deletion_previews WHERE session_hash=?").bind(s.token_hash).run();
    await env.DB.prepare("INSERT INTO deletion_previews VALUES(?,?,?,?,?)").bind(await hash(token), s.token_hash, b.scope, JSON.stringify(rows.results.map((d2) => d2.id)), expires).run();
    return json({ token, scope: b.scope, count: rows.results.length, examples: rows.results.slice(0, 5).map((d2) => ({ mac: d2.mac, label: d2.label })), confirmation: b.scope === "all" ? "EXCLUIR TODOS" : "EXCLUIR", expires });
  }
  if (p === "/api/admin/devices/delete" && m === "POST") {
    if (s.role !== "admin") fail(403, "Acesso reservado ao administrador.");
    const b = await readBody(r);
    if (!/^[0-9a-f]{64}$/.test(b.token || "")) fail(422, "Confirma\xE7\xE3o inv\xE1lida.");
    const token = await hash(b.token), now = Date.now();
    const preview = await env.DB.prepare("SELECT scope FROM deletion_previews WHERE token_hash=? AND session_hash=? AND expires>?").bind(token, s.token_hash, now).first();
    if (!preview) fail(410, "A confirma\xE7\xE3o expirou ou j\xE1 foi utilizada. Selecione os aparelhos novamente.");
    if (b.confirmation !== (preview.scope === "all" ? "EXCLUIR TODOS" : "EXCLUIR")) fail(422, "Digite a confirma\xE7\xE3o exatamente como indicada.");
    const ids = "SELECT value FROM json_each((SELECT ids FROM deletion_previews WHERE token_hash=? AND session_hash=? AND expires>?))";
    const results = await env.DB.batch([
      env.DB.prepare("DELETE FROM audit WHERE device_id IN (" + ids + ")").bind(token, s.token_hash, now),
      env.DB.prepare("DELETE FROM devices WHERE id IN (" + ids + ") RETURNING id").bind(token, s.token_hash, now),
      env.DB.prepare("DELETE FROM deletion_previews WHERE token_hash=? AND session_hash=?").bind(token, s.token_hash)
    ]);
    return json({ deleted: results[1].results.length });
  }
  if (p === "/api/admin/pix" && ["GET", "PUT"].includes(m)) {
    if (s.role !== "admin") fail(403, "Acesso reservado ao administrador.");
    if (m === "GET") return json({ settings: await pixSettings(env) });
    const b = await readBody(r), value = { key: text(b.key, 77, "Chave Pix"), name: text(b.name, 80, "Recebedor"), city: text(b.city, 60, "Cidade"), amountCents: b.amountCents };
    try {
      buildPix({ ...value, txid: "TESTE" });
    } catch {
      fail(422, "Confira os dados do Pix e o valor anual.");
    }
    await env.DB.prepare("INSERT INTO settings VALUES('pix',?) ON CONFLICT(key) DO UPDATE SET value=excluded.value").bind(await seal(env, value, "settings")).run();
    return json({ ok: true });
  }
  const id = target(s, u);
  let d = await findDevice(env, id);
  if (p === "/api/lists/validate" && m === "POST") {
    await limit(env, "validate-list:" + id, 10, 60);
    const input = await readBody(r);
    if (input?.format === 'm3u_direct' && s.role !== 'admin') fail(403, 'Opção disponível somente para o administrador.');
    try {
      const result = await validatePlaylistInput(input, {allowDirectM3u:s.role === 'admin'});
      if (s.role !== "admin") delete result.diagnostics;
      return json(result);
    }
    catch (error) { fail(error.status === 422 ? 422 : 502,
      s.role === "admin" && error.status === 422 ? error.message : "Não foi possível preparar esta lista. Confira o link ou tente novamente."); }
  }
  if (p === "/api/account" && m === "GET") {
    const account = await account2(env, id, s.role === "admin");
    return json({ ...account, openSubtitlesAvailable: await openPortalReady(env, account.snapshot) });
  }
  if (p === "/api/opensubtitles/connect" && m === "POST") {
    await limit(env,"open-login:"+id,6,600);
    const b=await readBody(r);
    if(!/^[0-9a-f-]{36}$/.test(b.requestId||"")) fail(422,"Identificação de envio inválida.");
    const previous=await env.DB.prepare("SELECT device_id,kind,state FROM commands WHERE id=?").bind(b.requestId).first();
    if(previous) {
      if(previous.device_id!==id || previous.kind!=="subtitle_session")fail(409,"Identificação de envio já utilizada.");
      return json({id:b.requestId,state:previous.state});
    }
    const account=await account2(env,id,s.role==="admin");
    if(!await openPortalReady(env,account.snapshot))fail(503,"Abra a etapa de conexão na TV e atualize o status. Se necessário, atualize o aplicativo.");
    let payload;
    try{payload=await connectOpenAccount(env,b);}
    catch(error){if(error instanceof OpenPortalError)fail(error.status,error.message);throw error;}
    finally{b.password="";}
    const queued=await queue(env,id,"subtitle_session",payload,b.requestId,Math.min(900000,payload.expiresAt-Date.now()));
    if(["pending","delivering"].includes(queued.state)) {
      const task=notifyLists(env,id,unseal);if(ctx?.waitUntil)ctx.waitUntil(task);else await task;
    }
    return json(queued);
  }
  if (p === "/api/renewal-choice" && m === "POST") return json({ renewal: await saveRenewalChoice(env, d, await readBody(r)) });
  if (p === "/api/payment" && m === "POST") {
    const settings = await pixSettings(env);
    if (!settings) fail(409, "O administrador ainda n\xE3o configurou o Pix. Entre em contato com ele.");
    let payment = await env.DB.prepare("SELECT * FROM payments WHERE device_id=? AND status='pending' AND expires>? ORDER BY created_at DESC LIMIT 1").bind(id, Date.now()).first();
    if (!payment) {
      const paymentId = crypto.randomUUID(), txid = "IP" + d.mac.replaceAll(":", "") + random(5).toUpperCase();
      await env.DB.prepare("INSERT INTO payments(id,device_id,txid,amount_cents,created_at,expires) VALUES(?,?,?,?,?,?)").bind(paymentId, id, txid, settings.amountCents, Date.now(), Date.now() + DAY).run();
      payment = await env.DB.prepare("SELECT * FROM payments WHERE id=?").bind(paymentId).first();
    }
    return json({ id: payment.id, mac: d.mac, txid: payment.txid, amountCents: payment.amount_cents, expires: payment.expires, recipient: settings.name, payload: buildPix({ ...settings, amountCents: payment.amount_cents, txid: payment.txid }), manual: true });
  }
  if (p === "/api/admin/payments" && m === "GET") {
    if (s.role !== "admin") fail(403, "Acesso reservado ao administrador.");
    const rows = await env.DB.prepare("SELECT id,txid,amount_cents,status,created_at,paid_at FROM payments WHERE device_id=? ORDER BY created_at DESC LIMIT 20").bind(id).all();
    return json({ items: rows.results });
  }
  if (p === "/api/rename" && m === "PATCH") {
    const b = await readBody(r);
    await env.DB.prepare("UPDATE devices SET label=? WHERE id=?").bind(text(b.label, 80, "Nome"), id).run();
    return json({ ok: true });
  }
  if (p === "/api/commands" && m === "POST") {
    await limit(env, "send:" + id, 60, 60);
    const b = await readBody(r);
    let payload;
    switch (b.kind) {
      case "list_upsert":
        payload = playlist(b.data || {});
        break;
      case "list_delete":
        payload = { id: text(b.data?.id, 120, "ID da lista") };
        break;
      case "subtitle_key":
        if (!["subdl", "opensubtitles"].includes(b.data?.provider)) fail(422, "Provedor inv\xE1lido.");
        if(b.data.provider==="opensubtitles" && b.data.key!==null)fail(422,"Use o login OpenSubtitles nesta página.");
        const key = b.data.key === null ? null : text(b.data.key, 512, "Chave");
        if (key !== null && (key.length < 8 || /\s/.test(key))) fail(422, "Chave inv\xE1lida.");
        payload = { provider: b.data.provider, key };
        break;
      case "subtitle_style":
        payload = style(b.data);
        break;
      case "snapshot":
        payload = {};
        break;
      default:
        fail(422, "Envio inv\xE1lido.");
    }
    const queued = await queue(env, id, b.kind, payload, b.requestId);
    if (["list_upsert", "list_delete", "snapshot", "subtitle_key", "subtitle_style"].includes(b.kind) && ["pending", "delivering"].includes(queued.state)) {
      const task = notifyLists(env, id, unseal);
      if (ctx?.waitUntil) ctx.waitUntil(task);
      else await task;
    }
    return json(queued);
  }
  if (p === "/api/cancel-command" && m === "POST") {
    const b = await readBody(r);
    const result = await env.DB.prepare("UPDATE commands SET cipher=NULL,state='cancelled',finished_at=? WHERE id=? AND device_id=? AND state='pending'").bind(Date.now(), b.id || "", id).run();
    if (!result.meta.changes) {
      const existing = await env.DB.prepare("SELECT state FROM commands WHERE id=? AND device_id=?").bind(b.id || "", id).first();
      if (existing && existing.state !== "cancelled") fail(409, "O envio j\xE1 entrou em entrega ou terminou e n\xE3o pode ser cancelado.");
    }
    return json({ ok: true });
  }
  if (s.role !== "admin") fail(403, "Acesso reservado ao administrador.");
  if (p === "/api/admin/device" && m === "PATCH") {
    const b = await readBody(r);
    if (!["active", "blocked"].includes(b.status)) fail(422, "Situa\xE7\xE3o inv\xE1lida.");
    await env.DB.prepare("UPDATE devices SET label=?,status=? WHERE id=?").bind(text(b.label, 80, "Nome"), b.status, id).run();
    await audit(env, id, "device:" + b.status);
    return json({ ok: true });
  }
  if (p === "/api/admin/grant" && m === "POST") {
    const b = await readBody(r);
    if (!["payment", "manual"].includes(b.kind) || !/^[0-9a-f-]{36}$/.test(b.requestId || "")) fail(422, "Libera\xE7\xE3o inv\xE1lida.");
    const previous = await env.DB.prepare("SELECT device_id FROM license_grants WHERE request_id=?").bind(b.requestId).first();
    if (previous) {
      if (previous.device_id !== id) fail(409, "Identifica\xE7\xE3o de libera\xE7\xE3o j\xE1 utilizada.");
      return json({ license: license(d) });
    }
    let payment = null;
    if (b.kind === "payment") {
      payment = await env.DB.prepare("SELECT * FROM payments WHERE id=? AND device_id=?").bind(b.paymentId || "", id).first();
      if (!payment) fail(422, "Selecione o Pix recebido.");
      if (payment.status === "paid") return json({ license: license(d) });
    }
    const reference = payment?.txid || text(b.reference || "", 120, "Refer\xEAncia", true);
    for (let retry = 0; retry < 3; retry++) {
      d = await findDevice(env, id);
      if (d.release_request === b.requestId) return json({ license: license(d) });
      const now = Date.now(), start = Math.max(now, d.license_until || 0), until = grantUntil(start, b.kind === "payment" ? null : b.duration, now);
      const statements = [env.DB.prepare("UPDATE devices SET license_acquired_at=coalesce(license_acquired_at,?),license_until=?,status='active',release_kind=?,release_at=?,release_reference=?,release_request=? WHERE id=? AND coalesce(license_until,0)=? AND NOT EXISTS(SELECT 1 FROM license_grants WHERE request_id=?) AND (? IS NULL OR EXISTS(SELECT 1 FROM payments WHERE id=? AND device_id=? AND status!='paid'))").bind(now, until, b.kind, now, reference, b.requestId, id, d.license_until || 0, b.requestId, payment?.id || null, payment?.id || null, id)];
      statements.push(env.DB.prepare("INSERT OR IGNORE INTO license_grants SELECT ?,?,?,?,?,? WHERE EXISTS(SELECT 1 FROM devices WHERE id=? AND release_request=?)").bind(b.requestId, id, until, now, b.kind, reference, id, b.requestId));
      if (payment) statements.push(env.DB.prepare("UPDATE payments SET status='paid',paid_at=? WHERE id=? AND device_id=? AND EXISTS(SELECT 1 FROM devices WHERE id=? AND release_request=?)").bind(now, payment.id, id, id, b.requestId));
      const results = await env.DB.batch(statements);
      if (results[0].meta.changes) {
        await audit(env, id, "license:" + b.kind);
        return json({ license: license(await findDevice(env, id)) });
      }
      if (payment && (await env.DB.prepare("SELECT status FROM payments WHERE id=?").bind(payment.id).first()).status === "paid") return json({ license: license(await findDevice(env, id)) });
    }
    fail(409, "A licen\xE7a mudou. Atualize a p\xE1gina.");
  }
  if (p === "/api/admin/reset-code" && m === "POST") {
    const value = code();
    await env.DB.batch([env.DB.prepare("UPDATE devices SET code_hash=?,code_cipher=? WHERE id=?").bind(await codeHash(env, d.mac, value), await seal(env, value, id), id), env.DB.prepare("DELETE FROM sessions WHERE device_id=?").bind(id)]);
    await audit(env, id, "reset-code");
    return json({ code: value });
  }
  fail(404, "P\xE1gina n\xE3o encontrada.");
}
__name(api, "api");
var worker_default = {
  async fetch(r, env, ctx) {
    let response;
    try {
      if (!new URL(r.url).pathname.startsWith("/api/")) return await env.ASSETS.fetch(r);
      if (!/^[0-9a-f]{64}$/.test(env.APP_KEY || "") || typeof env.ADMIN_PASSWORD !== "string" || env.ADMIN_PASSWORD.length < 16) fail(503, "Servidor aguardando configura\xE7\xE3o.");
      response = await api(r, env, ctx);
    } catch (e) {
      response = json({ error: e instanceof ApiError ? e.message : "N\xE3o foi poss\xEDvel concluir. Tente novamente." }, e instanceof ApiError ? e.status : 500);
    }
    const h = new Headers(response.headers);
    h.set("Cache-Control", "no-store");
    h.set("X-Content-Type-Options", "nosniff");
    h.set("Referrer-Policy", "no-referrer");
    h.set("Content-Security-Policy", "default-src 'self'; script-src 'self'; style-src 'self'; font-src 'self' data:; img-src 'self' data:; connect-src 'self'; frame-ancestors 'none'; base-uri 'none'; form-action 'self'");
    if (new URL(r.url).protocol === "https:") h.set("Strict-Transport-Security", "max-age=31536000");
    return new Response(response.body, { status: response.status, headers: h });
  },
  async scheduled(event, env) {
    const now = Date.now();
    await env.DB.batch([env.DB.prepare("DELETE FROM bulk_action_previews WHERE expires<?").bind(now), env.DB.prepare("DELETE FROM deletion_previews WHERE expires<?").bind(now), env.DB.prepare("DELETE FROM qr_logins WHERE expires<?").bind(now), env.DB.prepare("DELETE FROM sessions WHERE expires<?").bind(now), env.DB.prepare("DELETE FROM limits WHERE expires<?").bind(Math.floor(now / 1e3)), env.DB.prepare("UPDATE commands SET cipher=NULL,state='expired',finished_at=? WHERE state IN ('pending','delivering') AND expires<?").bind(now, now), env.DB.prepare("DELETE FROM commands WHERE finished_at<?").bind(now - DAY), env.DB.prepare("DELETE FROM snapshots WHERE expires<?").bind(now), env.DB.prepare("DELETE FROM audit WHERE at<?").bind(now - 30 * DAY), env.DB.prepare("UPDATE payments SET status='expired' WHERE status='pending' AND expires<?").bind(now), env.DB.prepare("DELETE FROM payments WHERE status='expired' AND expires<?").bind(now - 30 * DAY)]);
  }
};
export {
  worker_default as default,
  grantUntil,
  license,
  playlist,
  seal,
  style,
  unseal,
  virtualMac
};
//# sourceMappingURL=worker.js.map
