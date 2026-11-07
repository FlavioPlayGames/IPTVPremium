import {createRequire} from 'node:module';
const require=createRequire(import.meta.url);
const {chromium}=require('playwright');
const {default:chromiumRuntime}=await import('/workspace/legendas-duas-etapas/dom-tests/node_modules/@sparticuz/chromium/build/index.js');
import http from 'node:http';import {readFile} from 'node:fs/promises';import path from 'node:path';import assert from 'node:assert/strict';
const root='/workspace/legendas-duas-etapas/site/public';const out='/workspace/legendas-duas-etapas';
const server=http.createServer(async(req,res)=>{try{const name=new URL(req.url,'http://localhost').pathname;const file=path.join(root,name==='/'?'index.html':name);const b=await readFile(file);res.setHeader('Content-Type',file.endsWith('.js')?'application/javascript':file.endsWith('.css')?'text/css':file.endsWith('.html')?'text/html':'application/octet-stream');res.end(b)}catch{res.statusCode=404;res.end('not found')}});
await new Promise(r=>server.listen(0,'127.0.0.1',r));const origin='http://127.0.0.1:'+server.address().port;let browser;let checks=0;
try {
 for(const [name,width,height]of [['mobile',390,844],['desktop',1440,1000]]) {
  browser=await chromium.launch({headless:true,executablePath:await chromiumRuntime.executablePath(),args:chromiumRuntime.args});
  const page=await browser.newPage({viewport:{width,height},deviceScaleFactor:1});const errors=[];page.on('pageerror',e=>errors.push(e.message));const requests=[];
  const now=Date.now();let pendingReads=0,accept=true;
  const account={id:'tv-a',mac:'02:11:22:33:44:55',label:'TV da sala',code:'123456',model:'TV teste',license:{state:'trial',trialUntil:now+86400000},renewal:{acquiredAt:null,expiresAt:null,daysRemaining:1},commands:[],snapshot:{lists:[],subtitles:{textColor:'FFFF00',background:'NONE',sizeSp:22,typeface:'DEFAULT',edge:'SHADOW'},providers:{subdl:false,opensubtitles:false},subtitleProtocol:2,openSubtitlesFingerprint:'a'.repeat(64)},openSubtitlesAvailable:true};
  await page.route('**/api/**',async route=>{
   const request=route.request(),u=new URL(request.url()),body=request.postData()?JSON.parse(request.postData()):null;requests.push({path:u.pathname,body});
   let data={ok:true},status=200;
   if(['/api/session','/api/qr-login'].includes(u.pathname))data={role:'user',csrf:'test-csrf'};
   else if(u.pathname==='/api/account') {
    if(account.commands.some(c=>c.state==='pending')) {
     pendingReads++;
     if(pendingReads>=2){for(const c of account.commands){c.state='received';if(c.provider==='opensubtitles')account.snapshot.providers.opensubtitles=true;if(c.provider==='subdl')account.snapshot.providers.subdl=true;}}
    }
    data=account;
   } else if(u.pathname==='/api/commands') {
    account.commands.push({id:body.requestId,kind:body.kind,provider:body.data?.provider,state:'pending',created_at:now});pendingReads=0;data={id:body.requestId,state:'pending'};
   } else if(u.pathname==='/api/opensubtitles/connect') {
    if(!accept){status=422;data={error:'Confira seu usuário e senha do OpenSubtitles.com.'};}
    else {await new Promise(r=>setTimeout(r,300));account.commands.push({id:body.requestId,kind:'subtitle_session',provider:'opensubtitles',state:'pending',created_at:now});pendingReads=0;data={id:body.requestId,state:'pending'};}
   } else if(u.pathname==='/api/logout')throw Error('Unexpected portal logout');
   await route.fulfill({status,contentType:'application/json',body:JSON.stringify(data)});
  });
  await page.goto(origin+'/#access=mock-qr&view=subtitles');await page.locator('#open-form').waitFor({state:'visible'});await page.waitForFunction(()=>!document.querySelector('#open-send').disabled);
  assert.equal(await page.locator('#subdl-title').textContent(),'SubDL');assert.equal(await page.locator('#open-title').textContent(),'OpenSubtitles');checks++;
  assert(!await page.locator('#provider-settings').innerText().then(t=>t.includes('SubSource')));checks++;
  assert(await page.locator('#show-providers').innerText().then(t=>t==='Conectar serviços'));checks++;
  const overflow=await page.evaluate(()=>document.documentElement.scrollWidth>innerWidth+1);assert(!overflow,name+' has horizontal overflow');checks++;
  await page.screenshot({path:out+'/site-'+name+'.png',fullPage:true});
  await page.locator('#show-appearance').click();await page.locator('#style-form').waitFor({state:'visible'});await page.locator('#show-providers').click();checks++;
  accept=false;await page.locator('#open-user').fill('viewer');await page.locator('#open-password').fill('bad-password');await page.locator('#open-send').click();await page.waitForFunction(()=>document.querySelector('#open-password').value==='');await page.waitForTimeout(150);
  assert(await page.locator('#app-view').isVisible());assert(!requests.some(r=>r.path==='/api/logout'));checks++;
  accept=true;await page.locator('#open-user').fill('viewer');await page.locator('#open-password').fill(' pass with spaces ');await page.locator('#open-send').click();await page.waitForFunction(()=>document.querySelector('#open-password').value==='');checks++;
  const sent=()=>requests.filter(r=>r.path==='/api/opensubtitles/connect').at(-1);await page.waitForTimeout(400);assert.equal(sent().body.password,' pass with spaces ');assert.deepEqual(Object.keys(sent().body).sort(),['password','requestId','username']);checks++;
  await page.waitForFunction(()=>document.querySelector('#open-status').textContent.includes('Conectado na TV'),{},{timeout:15000});checks++;
  await page.locator('#subdl-key').fill('fake-personal-subdl-key');await page.locator('#subdl-send').click();await page.waitForFunction(()=>document.querySelector('#subdl-key').value==='');await page.waitForTimeout(100);
  assert(requests.some(r=>r.path==='/api/commands'&&r.body.kind==='subtitle_key'&&r.body.data.provider==='subdl'));checks++;
  const stored=await page.evaluate(()=>JSON.stringify({local:{...localStorage},session:{...sessionStorage}}));assert(!stored.includes('pass with spaces')&&!stored.includes('bad-password'));checks++;
  assert.deepEqual(errors,[]);checks++;
  console.log('PASS '+name+': providers, appearance, responsive layout, password error keeps portal login, click-only auth, credentials cleared, same-account status, no browser credential storage.');
  await browser.close();browser=null;
 }
 console.log(checks+' browser checks passed; local mocked API only.');
}finally{if(browser)await browser.close();await new Promise(r=>server.close(r));}
