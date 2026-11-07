import {createSubtitlePortal} from './subtitles.js';
import {updateArtifactNames,createUpdateDraft,buildUpdateBody,samePublishedUpdate} from './update-artifacts.js';
import {icons} from './icons.js';
const $=id=>document.getElementById(id),COLORS=[['FFFFFF','Branco forte'],['F2F2F2','Branco médio'],['E5E5E5','Branco suave'],['FFFF00','Amarelo forte'],['FFD54F','Amarelo médio'],['FFE991','Amarelo suave']];
const adminPage=/^\/admin(?:\/|\/index\.html)?$/.test(location.pathname);
let csrf='',role='',device='',account=null,lists=[],color='FFFFFF',editingId=null,page=1,query='',toastTimer,pollTimer,accountVersion=0,loginAdmin=false,pollCount=0;
const snapshotRequests=new Map();
let subtitlePortal;
const licenseNames={trial:'Período de teste',licensed:'Licença ativa',expired:'Acesso expirado',blocked:'Aparelho bloqueado'};
const stateNames={pending:'Aguardando recebimento',delivering:'Em entrega ao aplicativo',received:'Recebido pelo aplicativo',failed:'Não aplicado',expired:'Prazo de envio encerrado',cancelled:'Envio cancelado'};
const kindNames={list_upsert:'Cadastro / edição de lista',list_delete:'Exclusão de lista',subtitle_key:'Conexão de legendas',subtitle_session:'Conta OpenSubtitles',subtitle_style:'Aparência das legendas',snapshot:'Consulta ao aparelho'};
const date=v=>v?new Date(v).toLocaleString('pt-BR',{dateStyle:'short',timeStyle:'short'}):'—';
const shortDate=v=>v?new Date(v).toLocaleDateString('pt-BR'):'—';
const money=c=>new Intl.NumberFormat('pt-BR',{style:'currency',currency:'BRL'}).format(c/100);
function toast(message){$('toast').textContent=message;$('toast').hidden=false;clearTimeout(toastTimer);toastTimer=setTimeout(()=>$('toast').hidden=true,6500)}
function stopPoll(){clearTimeout(pollTimer);pollCount=0}
async function run(task,button){if(button){button.disabled=true;button.setAttribute('aria-busy','true')}try{await task()}catch(e){toast(e.message)}finally{if(button){button.disabled=false;button.removeAttribute('aria-busy')}}}
async function api(path,options={}){let r;try{r=await fetch(path,{credentials:'same-origin',...options,headers:{'Content-Type':'application/json','X-CSRF-Token':csrf,...options.headers}})}catch{throw Error('Não foi possível conectar. Verifique sua conexão e tente novamente.')}let b;try{b=await r.json()}catch{throw Error('O servidor não respondeu como esperado. Tente novamente.')}if(!r.ok){if(r.status===401&&!path.endsWith('/login'))logoutView();throw Error(b.error||'Não foi possível concluir.')}return b}
function target(path){return role==='admin'?path+'?device='+encodeURIComponent(device):path}
function wipeInputs(){subtitlePortal?.wipe();['subdl-key','open-user','open-password','list-pass','list-user','list-server','list-url','list-xtream-server','list-xtream-user','list-xtream-pass','admin-password','login-code','pix-key'].forEach(id=>{if($(id))$(id).value=''})}
function logoutView(){deviceRequest++;selectedDevices.clear();$('site-header').hidden=true;document.querySelector('.shell').classList.add('login-shell');stopPoll();accountVersion++;csrf='';role='';device='';account=null;lists=[];wipeInputs();$('list-rows').replaceChildren();$('device-rows').replaceChildren();$('owner-payments').replaceChildren();$('app-view').hidden=true;$('login-view').hidden=false;$('avatar').hidden=true;document.querySelectorAll('dialog[open]').forEach(d=>d.close());setLoginMode(adminPage)}
function setLoginMode(admin){loginAdmin=admin;$('login-form').hidden=admin;$('admin-login-form').hidden=!admin;$('admin-password').value='';document.querySelector('.login-card h1').innerHTML=admin?'Acesso do administrador':'Seu entretenimento,<br>do seu jeito.';document.querySelector('.login-card .subtitle').textContent=admin?'Gerencie os aparelhos e as licenças do seu aplicativo.':'Gerencie suas listas e personalize as legendas do seu aplicativo em um só lugar.';document.querySelector('.login-bottom').hidden=admin}
async function enter(result){$('site-header').hidden=false;document.querySelector('.shell').classList.remove('login-shell');csrf=result.csrf;role=result.role;wipeInputs();$('login-view').hidden=true;$('app-view').hidden=false;$('avatar').hidden=false;$('side-caption').textContent=role==='admin'?'Administração':'Minha conta';$('nav-lists').querySelector('span').textContent=role==='admin'?'Aparelhos':'Minhas listas';if(role==='admin')await loadDevices();else{await loadAccount();if(!account.snapshot)await requestSnapshot()}}
$('login-form').onsubmit=e=>{e.preventDefault();run(async()=>{await enter(await api('/api/login',{method:'POST',body:JSON.stringify({mac:$('login-mac').value.trim(),code:$('login-code').value})}))},e.submitter)};
$('admin-login-form').onsubmit=e=>{e.preventDefault();run(async()=>{await enter(await api('/api/admin/login',{method:'POST',body:JSON.stringify({password:$('admin-password').value})}))},e.submitter)};
$('nav-login').onclick=()=>run(async()=>{await api('/api/logout',{method:'POST'});logoutView();setLoginMode(adminPage)});
function show(view){if(view==='style')subtitlePortal?.opened();['lists','style','admin'].forEach(v=>$(v+'-view').hidden=v!==view);$('nav-lists').classList.toggle('selected',view==='lists'||view==='admin');$('nav-style').classList.toggle('selected',view==='style');$('app-view').hidden=false;$('login-view').hidden=true}
$('nav-lists').onclick=()=>run(async()=>{if(role==='admin')await loadDevices();else show('lists')});$('nav-style').onclick=()=>{if(!account)return toast('Selecione um aparelho primeiro.');show('style')};
function cell(row,text,className){const td=document.createElement('td');td.textContent=text;if(className)td.className=className;row.append(td);return td}
function action(parent,label,task,icon=null){const b=document.createElement('button');b.type='button';b.className='ghost';if(icon)b.innerHTML=icon;const t=document.createElement('span');t.textContent=label;b.append(t);b.onclick=()=>run(task,b);parent.append(b);return b}
let deviceStatus='all',deviceRequest=0,visibleDevices=[],deviceTotal=0,deletePreview=null,deleteBusy=false,selectingAll=false;
const selectedDevices=new Set();
function closeDeviceActions(){$('device-actions-menu').hidden=true;$('device-actions-toggle').setAttribute('aria-expanded','false')}
function selectionChanged(){const n=selectedDevices.size;$('selection-count').textContent=`${n} selecionado${n===1?'':'s'}`;$('device-actions-toggle').disabled=!n||selectingAll;if(!n)closeDeviceActions();$('select-page').checked=selectingAll||(deviceTotal>0&&n===deviceTotal);$('select-page').indeterminate=!selectingAll&&n>0&&n!==deviceTotal;$('select-page').disabled=!deviceTotal||selectingAll;document.querySelectorAll('#device-rows input[type=checkbox]').forEach(c=>{c.checked=selectedDevices.has(c.value);c.disabled=selectingAll})}

async function loadDevices(){stopPoll();accountVersion++;const request=++deviceRequest;const b=await api(`/api/admin/devices?page=${page}&q=${encodeURIComponent(query)}&status=${deviceStatus}`);if(request!==deviceRequest||role!=='admin')return;if(page>1&&!b.items.length){page=1;return loadDevices()}visibleDevices=b.items;deviceTotal=b.total;show('admin');$('rename-device').hidden=true;$('mini-name').textContent='Administrador';$('mini-mac').textContent='';$('mini-status').textContent='Acesso administrativo';$('avatar').textContent='AD';$('count-total').textContent=b.counts.total||0;$('count-active').textContent=b.counts.active||0;$('count-blocked').textContent=b.counts.blocked||0;const fragment=document.createDocumentFragment();b.items.forEach(d=>{const row=document.createElement('tr');const check=document.createElement('input');check.type='checkbox';check.value=d.id;check.setAttribute('aria-label',`Selecionar ${d.label} · ${d.mac}`);check.onchange=()=>{if(check.checked)selectedDevices.add(d.id);else selectedDevices.delete(d.id);selectionChanged()};cell(row,'').append(check);cell(row,d.label);cell(row,d.mac,'mono');cell(row,licenseNames[d.license.state],d.status==='blocked'?'blocked':'gold');cell(row,shortDate(d.renewal?.acquiredAt));cell(row,shortDate(d.renewal?.expiresAt||d.license.trialUntil));const renewal=d.renewal;cell(row,renewal?.expiresAt?(renewal.daysRemaining<=0?'Vencida · pagamento necessário':(renewal.choice==='due'?'No vencimento · ':renewal.choice==='now'?'Renovar agora · ':'')+`Faltam ${renewal.daysRemaining} dia(s)`):'Teste · até '+shortDate(d.license.trialUntil));if(renewal?.choice==='due'&&renewal.daysRemaining>0)row.classList.add('renewal-attention');if(renewal?.expiresAt&&renewal.daysRemaining<=0)row.classList.add('renewal-expired');cell(row,date(d.lastSeen));const actions=cell(row,'');actions.className='device-actions';action(actions,'Gerenciar',async()=>{device=d.id;await loadAccount();if(!account.snapshot)await requestSnapshot()});fragment.append(row)});if(!b.items.length){const r=document.createElement('tr');cell(r,'Nenhum aparelho encontrado.').colSpan=9;fragment.append(r)}$('device-rows').replaceChildren(fragment);$('page-info').textContent=`Página ${page} · ${b.total} aparelhos`;$('prev-page').disabled=page===1;$('next-page').disabled=page*50>=b.total;selectionChanged()}
let searchTimer;function changeDeviceFilter(){deviceRequest++;selectedDevices.clear();query=$('search').value.trim();deviceStatus=$('status-filter').value;page=1;run(loadDevices)}
$('search').oninput=()=>{deviceRequest++;clearTimeout(searchTimer);searchTimer=setTimeout(changeDeviceFilter,500)};$('status-filter').onchange=()=>{clearTimeout(searchTimer);changeDeviceFilter()};$('prev-page').onclick=()=>{page--;run(loadDevices)};$('next-page').onclick=()=>{page++;run(loadDevices)};$('refresh').onclick=()=>run(loadDevices);$('back-admin').onclick=()=>run(loadDevices);
$('select-page').onchange=async()=>{if(!$('select-page').checked){selectedDevices.clear();selectionChanged();return}selectingAll=true;selectionChanged();const generation=deviceRequest;try{const result=await api(`/api/admin/devices/selection?q=${encodeURIComponent(query)}&status=${deviceStatus}`);if(generation!==deviceRequest)return;selectedDevices.clear();result.ids.forEach(id=>selectedDevices.add(id))}catch(e){toast(e.message)}finally{selectingAll=false;selectionChanged()}};

const deviceActionLabels={delete:'Excluir',block:'Bloquear',unblock:'Desbloquear',renew_license:'Renovar assinatura',renew_trial:'Renovar teste'};
const deviceActionDescriptions={delete:'Esta ação remove os cadastros selecionados, licenças, pagamentos e vínculos de recuperação. As sessões serão encerradas. Não desinstala o aplicativo nem apaga as listas locais da TV. Um novo cadastro poderá gerar outra identificação.',block:'Bloqueia o acesso dos aparelhos selecionados. As datas do teste e da assinatura serão preservadas.',unblock:'Remove o bloqueio dos aparelhos selecionados. Assinaturas e testes vencidos continuam expirados; esta ação não renova os prazos.',renew_license:'Renova por 1 ano. Se a assinatura ainda estiver válida, acrescenta 1 ano ao vencimento atual; se estiver vencida, conta a partir de agora. Aparelhos bloqueados continuam bloqueados. Esta liberação é manual e não confirma o recebimento de um Pix.',renew_trial:'Define um novo teste de 7 dias a partir da confirmação. Não altera o vencimento da assinatura. Aparelhos bloqueados continuam bloqueados.'};
$('device-actions-toggle').onclick=()=>{const open=$('device-actions-menu').hidden;$('device-actions-menu').hidden=!open;$('device-actions-toggle').setAttribute('aria-expanded',String(open))};
document.addEventListener('click',e=>{if(!e.target.closest('.actions-dropdown'))closeDeviceActions()});document.addEventListener('keydown',e=>{if(e.key==='Escape'&&!$('device-actions-menu').hidden){closeDeviceActions();$('device-actions-toggle').focus()}});
document.querySelectorAll('[data-device-action]').forEach(button=>button.onclick=()=>{closeDeviceActions();run(()=>previewDeviceAction(button.dataset.deviceAction),$('device-actions-toggle'))});
async function previewDeviceAction(action){if(!selectedDevices.size)return toast('Selecione pelo menos um aparelho.');const deleting=action==='delete';const p=await api(deleting?'/api/admin/devices/deletion-preview':'/api/admin/devices/actions-preview',{method:'POST',body:JSON.stringify({scope:'selected',ids:[...selectedDevices],action})});deletePreview={...p,action};deleteBusy=false;$('device-action-title').textContent=deviceActionLabels[action].toLocaleUpperCase('pt-BR');$('device-action-description').textContent=deviceActionDescriptions[action];$('delete-summary').textContent=`${p.count} aparelho(s) selecionado(s). Confira antes de confirmar.`;$('delete-examples').replaceChildren(...p.examples.map(d=>{const li=document.createElement('li');li.textContent=`${d.label} · ${d.mac}`;return li}));$('delete-label').hidden=!deleting;$('delete-confirmation').hidden=!deleting;$('delete-confirmation').required=deleting;$('delete-label').textContent=deleting?`Para confirmar, digite ${p.confirmation}:`:'';$('delete-confirmation').value='';$('delete-confirmation').disabled=false;$('delete-feedback').hidden=true;$('delete-submit').hidden=false;$('delete-submit').disabled=deleting;$('delete-submit').textContent=deleting?'Confirmar exclusão':'Confirmar ação';$('delete-close').textContent='Cancelar';$('delete-close').disabled=false;$('delete-modal').showModal()}
$('delete-confirmation').oninput=()=>{$('delete-submit').disabled=deleteBusy||!deletePreview||$('delete-confirmation').value!==deletePreview.confirmation};$('delete-close').onclick=()=>{$('delete-modal').close();deletePreview=null};$('delete-modal').addEventListener('cancel',e=>{if(deleteBusy)e.preventDefault()});
$('delete-form').onsubmit=async e=>{e.preventDefault();if(deleteBusy||!deletePreview)return;deleteBusy=true;const deleting=deletePreview.action==='delete';$('delete-submit').disabled=true;$('delete-close').disabled=true;$('delete-feedback').hidden=false;$('delete-feedback').textContent='Aplicando ação aos aparelhos…';$('delete-submit').textContent='Aplicando…';try{const result=await api(deleting?'/api/admin/devices/delete':'/api/admin/devices/actions-apply',{method:'POST',body:JSON.stringify({token:deletePreview.token,confirmation:$('delete-confirmation').value})});deletePreview=null;selectedDevices.clear();page=1;$('delete-feedback').textContent=deleting?`Exclusão concluída: ${result.deleted} aparelho(s) removido(s).`:`Ação concluída: ${deviceActionLabels[result.action]} aplicada a ${result.updated} aparelho(s).`;$('delete-submit').hidden=true;$('delete-confirmation').disabled=true;try{await loadDevices()}catch{$('delete-feedback').textContent+=' Atualize a lista para conferir os cadastros.'}}catch(error){$('delete-feedback').textContent=`Não foi possível confirmar o resultado: ${error.message} Feche esta janela e atualize a lista antes de tentar novamente.`;$('delete-submit').hidden=true}finally{deleteBusy=false;$('delete-close').disabled=false;$('delete-close').textContent='Fechar';$('delete-feedback').focus()}};

async function loadAccount(quiet=false){const token=++accountVersion;const b=await api(target('/api/account'));if(token!==accountVersion||!csrf)return;if(account?.id!==b.id)subtitlePortal?.wipe();account=b;device=b.id;lists=b.snapshot?.lists||[];if(!quiet)show('lists');renderAccount()}
function renderPushDiagnostic(){
 let node=$('owner-push-info');if(!node){node=document.createElement('span');node.id='owner-push-info';$('owner-device-info').parentElement.append(node)}
 if(role!=='admin'||!account.push){node.textContent='';return}
 const stamp=v=>new Date(v).toLocaleString('pt-BR',{dateStyle:'short',timeStyle:'medium'});
 const p=account.push,states={sending:'Envio em andamento',accepted:'Aviso aceito pelo Firebase (a entrega depende do aplicativo)',project_mismatch:'Projeto do token diferente do servidor',sender_mismatch:'Firebase recusou: remetente diferente do token',authentication_failed:'Falha na autenticação da conta de serviço',oauth_key:'Chave privada inválida ou em formato incorreto',oauth_network:'Sem resposta do Google na autenticação (conexão ou tempo esgotado)',oauth_response:'Resposta OAuth inesperada do Google',oauth_signature:'Google recusou a assinatura da chave; confira se a chave foi revogada',oauth_account:'Conta de serviço não encontrada ou excluída',oauth_disabled:'Conta de serviço desativada',oauth_clock:'Google recusou os horários do token JWT',oauth_invalid_grant:'Google recusou a credencial JWT (invalid_grant)',oauth_client:'Google não reconheceu a conta de serviço (invalid_client)',oauth_permission:'Google recusou a autorização da conta de serviço',oauth_scope:'Escopo de envio FCM recusado pelo Google',oauth_unavailable:'Autenticação Google temporariamente indisponível',oauth_rejected:'Google recusou a autenticação; confira o HTTP',network_failed:'Falha de conexão com o Firebase',failed:'Firebase não aceitou o aviso'};
 const state=!p.configured?'Firebase não configurado':!p.registered?'Aplicativo sem token registrado':p.result?(states[p.result]||'Aguardando novo envio'):'Token registrado; nenhum aviso enviado';
 node.textContent='Sincronização automática: '+state+(p.httpStatus?' · HTTP '+p.httpStatus:'')+(p.attemptedAt?' · Última tentativa: '+stamp(p.attemptedAt):'')+(p.pulledAt?' · Última consulta do aplicativo: '+stamp(p.pulledAt):' · Nenhuma consulta registrada');
}
function renderSnapshotStatus(){
 const pending=account.commands.some(c=>c.kind==='snapshot'&&['pending','delivering'].includes(c.state));
 const cached=account.snapshot?'Últimas listas recebidas em '+date(account.snapshot.receivedAt)+'. ':'';
 $('snapshot-status').textContent=pending?cached+'Solicitação enviada. Aguardando o aparelho responder; mantenha o aplicativo aberto e conectado.':account.snapshot?cached+'Use Editar na lista desejada ou busque novamente para consultar o aparelho.':'Clique em “Buscar listas do aparelho” para consultar as listas instaladas. Mantenha o aplicativo aberto e conectado.';
 $('fetch-device-lists').textContent=pending?'Buscar novamente':'Buscar listas do aparelho';
}
function renderAccount(){renderPushDiagnostic();renderRenewalNotice();$('owner-license-dates').textContent='Aquisição da assinatura: '+shortDate(account.renewal?.acquiredAt)+' · Vencimento: '+shortDate(account.renewal?.expiresAt);$('rename-device').hidden=false;$('mini-name').textContent=account.label;$('mini-mac').textContent=account.mac;$('mini-status').textContent=licenseNames[account.license.state];$('avatar').textContent=account.label.split(/\s+/).filter(Boolean).slice(0,2).map(x=>x[0]).join('').toUpperCase();document.querySelector('.mobile-meta').textContent=account.label+' · '+account.mac;$('license-title').textContent=licenseNames[account.license.state];const until=account.license.state==='trial'?account.license.trialUntil:account.license.licenseUntil;$('license-detail').textContent=until?'Válido até '+date(until):'Seu teste de 7 dias terminou. A licença é anual.';$('repair').hidden=role!=='admin';$('owner-device-info').textContent=account.label+(account.code?' · Código: '+account.code:'');$('list-count').textContent=account.snapshot?lists.length+' lista'+(lists.length===1?'':'s'):'';renderSnapshotStatus();renderLists();renderCommands();subtitlePortal?.render(account);if(account.snapshot?.subtitles&&!document.querySelector('#style-form:focus-within'))applyStyle(account.snapshot.subtitles)}
function renderLists(){const fragment=document.createDocumentFragment();lists.forEach(list=>{const row=document.createElement('article');row.className='list';const icon=document.createElement('div');icon.className='list-icon';icon.innerHTML=icons.list;const info=document.createElement('div');info.className='list-info';const name=document.createElement('h3');name.textContent=list.name;const desc=document.createElement('p');desc.textContent=list.type==='m3u'?'M3U · Link da lista':'Xtream Codes · Usuário e senha';info.append(name,desc);const actions=document.createElement('div');actions.className='list-actions';action(actions,'Editar',async()=>openList(list),icons.edit);action(actions,'Excluir',async()=>{if(!confirm('Enviar a exclusão de “'+list.name+'” para o aparelho?'))return;await sendCommand('list_delete',{id:list.id});toast('Exclusão enviada. Aguarde a confirmação do aplicativo.')},icons.trash);row.append(icon,info,actions);fragment.append(row)});if(account.snapshot&&!lists.length){const p=document.createElement('p');p.className='subtitle';p.textContent='Nenhuma lista cadastrada neste aparelho.';fragment.append(p)}$('list-rows').replaceChildren(fragment)}
function renderCommands(){const fragment=document.createDocumentFragment();account.commands.filter(x=>x.kind!=='snapshot'&&x.state!=='cancelled').slice(0,5).forEach(c=>{const row=document.createElement('div');row.className='pending-item';const t=document.createElement('span');t.textContent=kindNames[c.kind]+' · '+(stateNames[c.state]||c.state)+(c.state==='failed'?' ('+({invalid_key:'chave não aceita pelo provedor',storage_error:'falha ao salvar no aparelho',invalid_data:'dados inválidos',not_found:'item não encontrado'}[c.result]||'verifique no aplicativo')+')':'');row.append(t);if(c.state==='pending')action(row,'Cancelar',async()=>{await api(target('/api/cancel-command'),{method:'POST',body:JSON.stringify({id:c.id})});await loadAccount(true)});fragment.append(row)});$('pending-commands').replaceChildren(fragment)}
async function sendCommand(kind,data){const requestId=crypto.randomUUID();const b=await api(target('/api/commands'),{method:'POST',body:JSON.stringify({requestId,kind,data})});await loadAccount(true);startPoll();return b}
async function requestSnapshot(){
 if(!account)return;
 const id=account.id;if(snapshotRequests.has(id))return snapshotRequests.get(id);
 const request=(async()=>{
  const path=target('/api/commands');
  const pending=account.commands.find(c=>c.kind==='snapshot'&&['pending','delivering'].includes(c.state));
  // Reuse pending IDs: retry the wake-up without duplicating queued commands.
  await api(path,{method:'POST',body:JSON.stringify({requestId:pending?.id||crypto.randomUUID(),kind:'snapshot',data:{}})});
  if(account?.id!==id||!csrf)return;
  await loadAccount(true);startPoll();
 })();
 snapshotRequests.set(id,request);
 try{return await request}finally{if(snapshotRequests.get(id)===request)snapshotRequests.delete(id)}
}
function startPoll(){clearTimeout(pollTimer);pollCount=0;async function poll(){if(!csrf||!account||$('app-view').hidden||!$('admin-view').hidden||document.hidden)return;await run(async()=>{await loadAccount(true);pollCount++;if(pollCount<18&&account.commands.some(c=>['pending','delivering'].includes(c.state)))pollTimer=setTimeout(poll,5000)})}pollTimer=setTimeout(poll,5000)}
function fetchDeviceLists(button){return run(async()=>{await requestSnapshot();toast('Consulta enviada. As listas aparecerão aqui quando o aplicativo responder. Em aparelhos sem avisos automáticos, use Sincronizar agora no aplicativo.')},button)}
$('fetch-device-lists').onclick=()=>fetchDeviceLists($('fetch-device-lists'));
$('refresh-device').onclick=()=>fetchDeviceLists($('refresh-device'));
$('rename-device').onclick=()=>run(async()=>{if(!account)return;const label=prompt('Nome do aparelho:',account.label);if(label===null)return;await api(target('/api/rename'),{method:'PATCH',body:JSON.stringify({label})});await loadAccount(true)});
let validatedPlaylist = null, validationSequence = 0, validationController = null;
function invalidatePlaylistValidation() {
  validationSequence++; validationController?.abort(); validationController = null;
  validatedPlaylist = null;
  $('send-list').disabled = true;
  $('send-list').textContent = 'Enviar lista';
  $('list-validation-result').hidden = true;
  $('list-validation-status').textContent = '';
  ['list-server','list-user','list-pass'].forEach(id => $(id).value = '');
  $('validate-list').disabled = false;
}
function applyListInputFormat() {
  const xtream = $('list-input-format').value === 'xtream';
  const direct = role === 'admin' && $('list-input-format').value === 'm3u_direct';
  $('list-m3u-input').hidden = xtream;$('list-xtream-input').hidden = !xtream;
  $('list-url').disabled = xtream;$('list-url').required = !xtream;
  ['list-xtream-server','list-xtream-user','list-xtream-pass'].forEach(id => { $(id).disabled = !xtream;$(id).required = xtream; });
  $('list-format-help').textContent = direct ? 'Enviaremos o link como M3U, sem conversão para Xtream. O aplicativo fará a importação direta.' : xtream ? 'Tentaremos Xtream primeiro. A M3U será usada somente se essa tentativa falhar.' : 'Identificaremos a configuração Xtream do link. Quando identificada, a M3U seguirá como alternativa, usada somente se o Xtream falhar.';
}
$('list-input-format').onchange = () => { invalidatePlaylistValidation();applyListInputFormat(); };
['list-xtream-server','list-xtream-user','list-xtream-pass'].forEach(id => $(id).oninput = invalidatePlaylistValidation);
function openList(list=null) {
  invalidatePlaylistValidation();
  const formatSelect = $('list-input-format');
  const directOption = formatSelect.querySelector('option[value="m3u_direct"]');
  if (role === 'admin' && !directOption) formatSelect.add(new Option('M3U direta — sem conversão', 'm3u_direct'));
  if (role !== 'admin') directOption?.remove();
  editingId = list?.id || null;
  $('list-name').value = list?.name || '';
  $('list-input-format').value = list?.type === 'xtream' ? 'xtream' : 'm3u';
  $('list-xtream-server').value = list?.serverUrl || '';
  $('list-xtream-user').value = list?.username || '';
  $('list-xtream-pass').value = list?.password || '';
  applyListInputFormat();
  let link = list?.playlistUrl || '';
  if (list?.type === 'xtream') {
    try { const url = new URL(list.serverUrl.replace(/\/+$/, '') + '/get.php');
      url.searchParams.set('username', list.username);url.searchParams.set('password', list.password);
      url.searchParams.set('type','m3u_plus');link = url.href;
    } catch { link = ''; }
  }
  $('list-url').value = link;
  $('modal-title').textContent = list ? 'Editar lista' : 'Adicionar lista';
  $('list-modal').showModal();
}
$('new-list').onclick = () => openList();
$('list-name').oninput = invalidatePlaylistValidation;
$('list-url').oninput = invalidatePlaylistValidation;
$('list-modal').addEventListener('close', () => { invalidatePlaylistValidation();$('list-form').reset(); });
$('cancel-modal').onclick = () => $('list-modal').close();
$('validate-list').onclick = async () => {
  if (!$('list-form').reportValidity()) return;
  invalidatePlaylistValidation();
  const sequence = validationSequence, name = $('list-name').value, format = $('list-input-format').value;
  const input = format === 'xtream' ? {name,format,serverUrl:$('list-xtream-server').value,username:$('list-xtream-user').value,password:$('list-xtream-pass').value} : {name,format,link:$('list-url').value};
  validationController = new AbortController();
  $('validate-list').disabled = true;
  $('list-validation-status').textContent = format === 'm3u_direct' ? 'Preparando envio da M3U direta…' : 'Validando a lista e identificando o formato…';
  try {
    const result = await api(target('/api/lists/validate'), { method:'POST', signal:validationController.signal, body:JSON.stringify(input) });
    if (sequence !== validationSequence || !$('list-modal').open) return;
    const p = result.playlist;
    if (!p || !['xtream','m3u'].includes(p.type) || (p.type === 'xtream' ? !p.serverUrl || !p.username || !p.password : !p.playlistUrl)) throw Error('A validação não confirmou uma lista compatível.');
    validatedPlaylist = { ...p, name: name.trim() };
    $('list-detected-format').textContent = p.type === 'xtream' ? 'Xtream Codes' : 'Lista M3U';
    $('list-server-label').textContent = p.type === 'xtream' ? 'Servidor' : 'Link da lista';
    $('xtream-fields').hidden = p.type !== 'xtream';
    $('list-server').value = p.serverUrl || p.playlistUrl;$('list-user').value = p.username || '';$('list-pass').value = p.password || '';
    $('list-validation-result').hidden = false;
    $('list-validation-status').textContent = result.message + (role === 'admin' && result.diagnostics ? '\nDetalhes para o administrador: ' + result.diagnostics : '');
    $('send-list').textContent = 'Enviar lista';
    $('send-list').disabled = false;
  } catch (error) {
    if (sequence === validationSequence) $('list-validation-status').textContent = error.message;
  } finally {
    if (sequence === validationSequence) { $('validate-list').disabled = false;validationController = null; }
  }
};
$('list-form').onsubmit = e => { e.preventDefault();
  if (!validatedPlaylist) { $('validate-list').click();return; }
  const data = { ...validatedPlaylist, id:editingId || crypto.randomUUID(), isEnabled:lists.find(item => item.id === editingId)?.isEnabled !== false };
  run(async () => { await sendCommand('list_upsert', data);invalidatePlaylistValidation();$('list-form').reset();$('list-modal').close();toast('Lista enviada ao aplicativo como ' + (data.type === 'xtream' ? 'Xtream.' : 'M3U.')); }, e.submitter);
};
function appearanceTab(active){$('provider-settings').hidden=active;$('appearance-settings').hidden=!active;$('show-providers').classList.toggle('selected',!active);$('show-appearance').classList.toggle('selected',active);$('show-providers').setAttribute('aria-pressed',String(!active));$('show-appearance').setAttribute('aria-pressed',String(active));$('legend-description').textContent=active?'Personalize a aparência das legendas e acompanhe a prévia.':'Conecte suas contas de legendas ao aparelho.';preview()}
$('show-providers').onclick=()=>appearanceTab(false);$('show-appearance').onclick=()=>appearanceTab(true);
COLORS.forEach(([value,name])=>{const b=document.createElement('button');b.type='button';b.className='swatch';b.style.setProperty('--swatch','#'+value);b.title=name;b.setAttribute('aria-label',name);b.dataset.color=value;b.onclick=()=>{color=value;preview()};$('swatches').append(b)});
function styleData(){return {textColor:color,background:$('background').value,sizeSp:Number($('size').value),typeface:{'sans-serif':'DEFAULT','Georgia,serif':'SERIF',monospace:'MONOSPACE'}[$('font').value],edge:$('edge').value}}
function applyStyle(s){color=s.textColor;$('background').value=s.background;$('size').value=String(s.sizeSp);$('font').value={DEFAULT:'sans-serif',SERIF:'Georgia,serif',MONOSPACE:'monospace'}[s.typeface];$('edge').value=s.edge;preview()}
function preview(){const s=styleData(),p=$('preview-text');p.style.color='#'+s.textColor;p.style.backgroundColor={TRANSLUCENT:'#0009',NONE:'transparent',OPAQUE:'#000'}[s.background];p.style.fontSize=s.sizeSp+'px';p.style.fontFamily=$('font').value;p.style.textShadow={NONE:'none',OUTLINE:'-1px 0 #000,1px 0 #000,0 -1px #000,0 1px #000',SHADOW:'2px 2px 4px #000'}[s.edge];$('color-name').textContent=COLORS.find(x=>x[0]===color)?.[1]||'';document.querySelectorAll('.swatch').forEach(b=>{const active=b.dataset.color===color;b.classList.toggle('selected',active);b.setAttribute('aria-pressed',String(active))})}
$('style-form').oninput=preview;$('style-form').onsubmit=e=>{e.preventDefault();run(async()=>{await sendCommand('subtitle_style',styleData());toast('Preferências enviadas. Aguarde o aplicativo receber.')},e.submitter)};
subtitlePortal=createSubtitlePortal({$,api,target,run,toast,getAccount:()=>account,sendCommand,loadAccount,requestSnapshot,startPoll});
async function openPayment(){const b=await api(target('/api/payment'),{method:'POST',body:'{}'});$('payment-amount').textContent=money(b.amountCents)+' / ano';$('payment-mac').textContent=b.mac;$('payment-reference').textContent=b.txid;$('payment-recipient').textContent=b.recipient;$('pix-payload').value=b.payload;const qr=window.qrcode(0,'M');qr.addData(b.payload);qr.make();$('payment-qr').innerHTML=qr.createSvgTag(5,4);$('payment-modal').showModal()}
$('pay-license').onclick=()=>run(openPayment,$('pay-license'));
function renderRenewalNotice(){const r=account.renewal;$('renewal-notice').hidden=role==='admin'||!r?.noticeStage;if(!r?.noticeStage)return;$('renewal-message').textContent=`Agradecemos pela preferência pelo IPTV Premium! Para continuar usando nosso aplicativo, renove sua assinatura anual${r.amountCents?' por '+money(r.amountCents):' (consulte o valor na central)'}. Faltam ${r.daysRemaining} dia(s) para o vencimento, em ${shortDate(r.expiresAt)}. Você pode renovar agora ou no vencimento.`;$('renewal-feedback').hidden=true}
async function chooseRenewal(choice){const r=account.renewal;await api(target('/api/renewal-choice'),{method:'POST',body:JSON.stringify({choice,stage:r.noticeStage,expiresAt:r.expiresAt})});await loadAccount(true);if(choice==='now')await openPayment();else toast('Sua escolha foi registrada: renovar no vencimento. O acesso continua até o prazo contratado.')}
$('renewal-now').onclick=()=>run(()=>chooseRenewal('now'),$('renewal-now'));$('renewal-due').onclick=()=>run(()=>chooseRenewal('due'),$('renewal-due'));
$('close-payment').onclick=()=>$('payment-modal').close();$('copy-pix').onclick=()=>run(async()=>{if(navigator.clipboard){await navigator.clipboard.writeText($('pix-payload').value);toast('Pix copiado. Confira os dados no seu banco antes de pagar.')}else{$('pix-payload').select();toast('Selecione o código e copie para o seu banco.')}});
let updateBusy=false,updateHashing=false,updateHashEpoch=0,updateHasPublished=false,updateServerSupportsVariants=false;
let updateDraft=createUpdateDraft(null);
function saveUpdateArtifact(){
 const abi=$('update-artifact').value;
 updateDraft[abi]={enabled:abi==='universal'||$('update-artifact-enabled').checked,url:$('update-url').value,sizeBytes:$('update-size').value,sha256:$('update-sha').value};
}
function showUpdateArtifact(){
 updateHashEpoch++;updateHashing=false;
 const abi=$('update-artifact').value,value=updateDraft[abi];previousUpdateArtifact=abi;
 $('update-artifact-enabled').checked=value.enabled;
 $('update-artifact-help').textContent=abi==='universal'?'Obrigatório: também atende os aplicativos antigos e serve de alternativa.':'Opcional: quando incluído, será escolhido pelos aparelhos compatíveis.';
 $('update-url').value=value.url;$('update-size').value=value.sizeBytes;$('update-sha').value=value.sha256;$('update-apk').value='';
 updateWorking(false);
}
function fillUpdate(update){
 updateHasPublished=Boolean(update);updateDraft=createUpdateDraft(update);
 $('update-current').textContent=update?'Versão ativa: '+update.versionName+' · número interno '+update.versionCode+' · '+(1+Object.keys(update.variants||{}).length)+' APK(s) · opcional':'Nenhuma versão publicada.';
 $('update-code').value=update?.versionCode||'';$('update-name').value=update?.versionName||'';$('update-notes').value=update?.notes||'';
 $('update-artifact').value='universal';showUpdateArtifact();
}
$('update-settings').onclick=()=>run(async()=>{const result=await api('/api/admin/update');updateServerSupportsVariants=result.updateCapabilities?.manifestVersion>=2&&result.updateCapabilities?.optionalProtocol>=2;fillUpdate(result.update);$('update-feedback').hidden=true;$('update-modal').showModal()});
$('cancel-update').onclick=()=>{if(updateBusy)return;updateHashEpoch++;updateHashing=false;updateWorking(false);$('update-modal').close()};
$('update-modal').addEventListener('cancel',e=>{if(updateBusy)e.preventDefault();else{updateHashEpoch++;updateHashing=false;updateWorking(false)}});
// The previous artifact must be saved before reading the newly selected option.
let previousUpdateArtifact='universal';
$('update-artifact').onfocus=()=>{previousUpdateArtifact=$('update-artifact').value};
$('update-artifact').onchange=()=>{
 const next=$('update-artifact').value;$('update-artifact').value=previousUpdateArtifact;saveUpdateArtifact();$('update-artifact').value=next;previousUpdateArtifact=next;showUpdateArtifact();
};
$('update-artifact-enabled').onchange=()=>{saveUpdateArtifact();updateWorking(false)};
$('update-apk').onchange=()=>run(async()=>{
 const file=$('update-apk').files[0];if(!file)return;
 if(!file.name.toLowerCase().endsWith('.apk')||file.size<1||file.size>536870912)throw Error('Selecione um APK de até 512 MB.');
 const epoch=++updateHashEpoch,abi=$('update-artifact').value;
 $('update-sha').value='';$('update-size').value='';saveUpdateArtifact();
 updateHashing=true;updateWorking(false);
 try{
  const digest=await crypto.subtle.digest('SHA-256',await file.arrayBuffer());
  if(epoch!==updateHashEpoch||$('update-artifact').value!==abi)return;
  $('update-sha').value=[...new Uint8Array(digest)].map(x=>x.toString(16).padStart(2,'0')).join('');$('update-size').value=file.size;saveUpdateArtifact();
  toast('Tamanho e SHA-256 calculados para '+updateArtifactNames[abi]+'. Informe o link direto desse mesmo arquivo.');
 }finally{if(epoch===updateHashEpoch){updateHashing=false;updateWorking(false)}}
});
function updateFeedback(message){const node=$('update-feedback');node.textContent=message;node.hidden=false;node.focus({preventScroll:true});node.scrollIntoView({block:'nearest',behavior:'smooth'})}
function updateWorking(busy,action){
 updateBusy=busy;
 $('publish-update').disabled=busy||updateHashing;$('remove-update').disabled=busy||updateHashing||!updateHasPublished;
 $('publish-update').textContent=busy&&action==='publish'?'Publicando…':updateHashing?'Calculando SHA-256…':'Publicar atualização';$('remove-update').textContent=busy&&action==='remove'?'Suspendendo…':'Retirar atualização';
 $('update-form').setAttribute('aria-busy',String(busy||updateHashing));
 ['update-artifact','update-code','update-name','update-notes','cancel-update'].forEach(id=>$(id).disabled=busy);
 const abi=$('update-artifact').value,enabled=abi==='universal'||$('update-artifact-enabled').checked;
 $('update-artifact-enabled').disabled=busy||abi==='universal';
 ['update-url','update-size','update-sha','update-apk'].forEach(id=>{$(id).disabled=busy||!enabled;if(id!=='update-apk')$(id).required=enabled});
}
$('update-form').onsubmit=async e=>{
 e.preventDefault();if(updateBusy||updateHashing)return;saveUpdateArtifact();let body;
 try{body=buildUpdateBody({versionCode:$('update-code').value,versionName:$('update-name').value,notes:$('update-notes').value},updateDraft)}catch(error){
  if(error.abi){$('update-artifact').value=error.abi;previousUpdateArtifact=error.abi;showUpdateArtifact()}updateFeedback(error.message);return;
 }
 if(!updateServerSupportsVariants){updateFeedback('Atualize a central antes de publicar atualizações opcionais.');return;}
 if(!confirm('Você testou os APKs com a mesma assinatura e o mesmo número interno, maior que o instalado? A atualização será opcional nos aplicativos compatíveis.'))return;
 updateWorking(true,'publish');updateFeedback('Publicando atualização… Aguarde a confirmação da central.');
 try{
  const result=await api('/api/admin/update',{method:'PUT',body:JSON.stringify(body)});
  if(!samePublishedUpdate(body,result.update))throw Error('A central não retornou a confirmação esperada. Reabra esta janela para consultar a versão ativa.');
  fillUpdate(result.update);previousUpdateArtifact='universal';
  updateFeedback('✓ Atualização publicada! Versão '+result.update.versionName+' · '+(1+Object.keys(result.update.variants||{}).length)+' APK(s). Aplicativos compatíveis podem adiar e escolhem o APK adequado. Versões antigas precisam instalar uma vez o aplicativo com atualização opcional.');
 }catch(error){updateFeedback(error.message)}finally{updateWorking(false)}
};
$('remove-update').onclick=async()=>{
 if(updateBusy||updateHashing||!confirm('Retirar a atualização publicada para todos os aparelhos?'))return;
 updateWorking(true,'remove');updateFeedback('Retirando atualização…');
 try{await api('/api/admin/update',{method:'DELETE'});fillUpdate(null);previousUpdateArtifact='universal';updateFeedback('✓ Atualização retirada. No aplicativo, use “Verificar novamente” para consultar a central.');}
 catch(error){updateFeedback(error.message)}finally{updateWorking(false)}
};
$('pix-settings').onclick=()=>run(async()=>{const {settings}=await api('/api/admin/pix');$('pix-key').value=settings?.key||'';$('pix-name').value=settings?.name||'';$('pix-city').value=settings?.city||'';$('pix-price').value=settings?String(settings.amountCents/100):'';$('pix-modal').showModal()});$('cancel-pix').onclick=()=>{$('pix-modal').close();$('pix-form').reset()};$('pix-form').onsubmit=e=>{e.preventDefault();run(async()=>{await api('/api/admin/pix',{method:'PUT',body:JSON.stringify({key:$('pix-key').value,name:$('pix-name').value,city:$('pix-city').value,amountCents:Math.round(Number($('pix-price').value)*100)})});$('pix-modal').close();$('pix-form').reset();toast('Dados de Pix atualizados.')},e.submitter)};
$('repair-action').onclick=()=>run(async()=>{$('owner-label').value=account.label;$('owner-status').value=account.status;$('access-modal').showModal();await loadPayments()});$('close-access').onclick=()=>$('access-modal').close();
$('access-form').onsubmit=e=>{e.preventDefault();run(async()=>{await api(target('/api/admin/device'),{method:'PATCH',body:JSON.stringify({label:$('owner-label').value,status:$('owner-status').value})});await loadAccount(true);toast('Aparelho atualizado.')},e.submitter)};
async function grant(duration=null,paymentId=null){await api(target('/api/admin/grant'),{method:'POST',body:JSON.stringify({kind:paymentId?'payment':'manual',paymentId,requestId:crypto.randomUUID(),duration})});await loadAccount(true);await loadPayments();toast('Prazo de acesso atualizado.')}
$('grant-year').onclick=()=>run(async()=>{if(confirm('Liberar um ano para este aparelho? Se já houver licença vigente, o ano será acrescentado ao prazo atual.'))await grant()},$('grant-year'));
$('duration-unit').onchange=()=>{const until=$('duration-unit').value==='until';$('duration-date-field').hidden=!until;$('duration-value-field').hidden=until;$('duration-date').required=until;$('duration-value').required=!until};$('duration-form').onsubmit=e=>{e.preventDefault();run(async()=>{const unit=$('duration-unit').value,value=unit==='until'?new Date($('duration-date').value).getTime():Number($('duration-value').value);if(confirm('Aplicar este prazo excepcional ao aparelho? A oferta padrão ao usuário continua anual.'))await grant({unit,value})},e.submitter)};
$('reset-code').onclick=()=>run(async()=>{if(!confirm('Gerar novo código? As sessões do usuário no site serão encerradas.'))return;await api(target('/api/admin/reset-code'),{method:'POST',body:'{}'});await loadAccount(true);toast('Código atualizado. O aplicativo receberá na próxima sincronização.')});
async function loadPayments(){const b=await api(target('/api/admin/payments'));const fragment=document.createDocumentFragment();for(const p of b.items){const row=document.createElement('div');row.className='payment-row';const strong=document.createElement('strong');strong.textContent=money(p.amount_cents)+' · '+({pending:'Aguardando conferência',paid:'Confirmado pelo administrador',expired:'Solicitação antiga'}[p.status]);const ref=document.createElement('div');ref.textContent=p.txid+' · '+date(p.created_at);row.append(strong,ref);if(p.status!=='paid')action(row,'Confirmar recebimento e liberar 1 ano',async()=>{if(confirm('Confirme no extrato do banco o recebimento deste Pix. Você confirma que recebeu '+money(p.amount_cents)+' com referência '+p.txid+'?'))await grant(null,p.id)});fragment.append(row)}if(!b.items.length){const p=document.createElement('p');p.className='subtitle';p.textContent='Nenhum Pix gerado para este aparelho.';fragment.append(p)}$('owner-payments').replaceChildren(fragment)}
const qrParams=new URLSearchParams(location.hash.slice(1)),qrAccess=qrParams.get('access'),qrView=qrParams.get('view');
if(qrAccess)history.replaceState(null,'',location.pathname+location.search);
setLoginMode(adminPage);
preview();run(async()=>{try{const result=qrAccess?await api('/api/qr-login',{method:'POST',body:JSON.stringify({token:qrAccess})}):await api('/api/session');if(adminPage&&result.role!=='admin'){logoutView();return}await enter(result);if(qrAccess&&qrView==='subtitles'){show('style');appearanceTab(false)}if(qrAccess&&qrView==='payment')await openPayment()}catch(e){logoutView();if(!e.message.includes('Entre na sua conta')&&!e.message.includes('sessão terminou'))toast(e.message)}});
window.addEventListener('pagehide',()=>{stopPoll();wipeInputs()});

// IPTV_PUSH_DELIVERY_DIAGNOSTICS_V1

// IPTV_FCM_OAUTH_DETAIL_V2

// IPTV_FETCH_DEVICE_LISTS_V1
