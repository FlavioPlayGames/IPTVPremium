// Phone entry sends only a verified session to the bound TV, never a queued password.
export function createSubtitlePortal({$,api,target,run,toast,getAccount,sendCommand,loadAccount,requestSnapshot,startPoll}) {
  let current='',editingKey=false,editingOpen=false,keyBusy=false,openBusy=false,generation=0;
  const requested=new Set();
  const wipe=()=>{generation++;['subdl-key','open-user','open-password'].forEach(id=>{if($(id))$(id).value=''});$('open-password').type='password';editingKey=false;editingOpen=false;keyBusy=false;openBusy=false;};
  function render(account) {
    if(!account)return;
    if(current!==account.id){wipe();current=account.id;}
    $('subtitle-device-label').textContent='Você está configurando: '+account.label;
    const pending=(provider)=>account.commands.some(c=>c.provider===provider&&['subtitle_key','subtitle_session'].includes(c.kind)&&['pending','delivering'].includes(c.state));
    const subdl=!!account.snapshot?.providers?.subdl,open=!!account.snapshot?.providers?.opensubtitles;
    $('subdl-status').textContent=pending('subdl')?'Aguardando recebimento na TV':subdl?'Configurado na TV':account.snapshot?'Não configurado':'Aguardando informação da TV';
    $('subdl-form').hidden=subdl&&!editingKey;$('subdl-manage').hidden=!subdl||editingKey;
    $('subdl-cancel').hidden=!editingKey;
    $('subdl-key').disabled=keyBusy; $('subdl-send').disabled=keyBusy;
    $('open-status').textContent=pending('opensubtitles')?'Conta validada · aguardando a TV':open?'Conectado na TV':'Não conectado';
    $('open-form').hidden=open&&!editingOpen;$('open-manage').hidden=!open||editingOpen;$('open-cancel').hidden=!editingOpen;
    const ready=account.openSubtitlesAvailable===true;
    for(const id of ['open-user','open-password','open-send','open-toggle'])$(id).disabled=!ready||openBusy;
    $('open-help').textContent=ready?'Use sua conta do OpenSubtitles.com. A senha é usada no login e não fica salva.':
      (account.snapshot?.subtitleProtocol>=2?'O OpenSubtitles ainda não está disponível para este aparelho. O SubDL continua funcionando.':'Abra a etapa 2 na TV e atualize o status para verificar a conexão.');
    $('open-send').textContent=openBusy?'Conectando…':'Entrar e conectar à TV';
    $('subdl-send').textContent=keyBusy?'Enviando…':'Enviar chave para a TV';
  }
  $('subdl-toggle').onclick=()=>{$('subdl-key').type=$('subdl-key').type==='password'?'text':'password'};
  $('subdl-edit').onclick=()=>{editingKey=true;render(getAccount());$('subdl-key').focus()};
  $('subdl-cancel').onclick=()=>{editingKey=false;$('subdl-key').value='';render(getAccount())};
  $('subdl-form').onsubmit=e=>{
    e.preventDefault();if(keyBusy)return;
    const key=$('subdl-key').value.trim(),ticket=generation;
    keyBusy=true;$('subdl-key').value='';$('subdl-key').type='password';render(getAccount());
    run(async()=>{try{await sendCommand('subtitle_key',{provider:'subdl',key});if(ticket===generation){editingKey=false;toast('Chave enviada. A TV atualizará o status ao receber.');}}
      finally{if(ticket===generation){keyBusy=false;render(getAccount());}}});
  };
  $('subdl-remove').onclick=()=>run(async()=>{if(!confirm('Desconectar o SubDL desta TV?'))return;await sendCommand('subtitle_key',{provider:'subdl',key:null});toast('Pedido de desconexão enviado.');});
  $('open-toggle').onclick=()=>{const field=$('open-password');field.type=field.type==='password'?'text':'password';$('open-toggle').setAttribute('aria-label',field.type==='password'?'Mostrar senha':'Ocultar senha');};
  $('open-edit').onclick=()=>{editingOpen=true;render(getAccount());$('open-user').focus();};
  $('open-cancel').onclick=()=>{editingOpen=false;$('open-user').value='';$('open-password').value='';render(getAccount());};
  $('open-form').onsubmit=e=>{
    e.preventDefault();const account=getAccount();if(openBusy||!account?.openSubtitlesAvailable)return;
    const username=$('open-user').value.trim(),password=$('open-password').value;
    const endpoint=target('/api/opensubtitles/connect'),id=account.id,ticket=generation;
    $('open-password').value='';$('open-password').type='password';openBusy=true;render(account);
    run(async()=>{
      try{
        await api(endpoint,{method:'POST',body:JSON.stringify({username,password,requestId:crypto.randomUUID()})});
        if(ticket!==generation||getAccount()?.id!==id)return;
        editingOpen=false;await loadAccount(true);startPoll();toast('Conta validada. Aguardando a TV receber a conexão.');
      }finally{if(ticket===generation){openBusy=false;render(getAccount());}}
    });
  };
  $('open-remove').onclick=()=>run(async()=>{if(!confirm('Desconectar o OpenSubtitles desta TV?'))return;await sendCommand('subtitle_key',{provider:'opensubtitles',key:null});toast('Pedido de desconexão enviado.');});
  $('subtitle-refresh').onclick=()=>run(async()=>{await requestSnapshot();toast('Status solicitado. Mantenha a etapa de conexão aberta na TV.');});
  return {render,wipe,opened(){const a=getAccount();if(a?.id&&!requested.has(a.id)){requested.add(a.id);run(requestSnapshot);}}};
}
