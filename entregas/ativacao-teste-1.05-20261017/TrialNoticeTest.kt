package br.com.flaviotorres.iptvpremium.core.device
import android.content.Context
import android.os.SystemClock
import org.json.JSONObject
import java.io.File
fun main() {
 var tests=0
 fun test(name:String,body:()->Unit){body();tests++;println("PASS $name")}
 val now=System.currentTimeMillis();val until=now+7*86400000L
 fun json(state:String="trial",ack:Boolean=false,mac:String="02:00:00:00:00:01",expires:Long=until,protocol:Int=1)=JSONObject()
  .put("mac",mac).put("code","123456").put("licenseState",state).put("trialUntil",expires).put("licenseUntil",0)
  .put("trialNotice",JSONObject().put("protocol",protocol).put("expiresAt",expires).put("acknowledged",ack))
 fun context(name:String):Context {val file=File("/workspace/storage-toolchain/tests/ativacao-teste/preferences/$name");file.deleteRecursively();file.mkdirs();return Context(file)}
 test("fresh server-confirmed seven-day trial shows welcome once") {
  PortalTrialState.receive(context("fresh"),json(),now,SystemClock.elapsed)
  check(PortalTrialState.snapshot.value!!.welcome(now));check(!PortalTrialState.snapshot.value!!.expired(now))
 }
 test("no notice before server protocol support; no fabricated seven-day deadline") {
  PortalTrialState.receive(context("legacy"),json(protocol=0),now,SystemClock.elapsed)
  check(!PortalTrialState.snapshot.value!!.welcome(now));check(!TrialNoticePolicy.showWelcome("trial",0,now,1,false))
 }
 test("due survives process recreation/update; acknowledgement and network retry are persisted") {
  val c=context("persist");PortalTrialState.receive(c,json(),now,SystemClock.elapsed);val snapshot=PortalTrialState.snapshot.value!!
  TrialAcknowledgement.mark(c,snapshot,"due");PortalTrialState.acknowledge(snapshot)
  val another=Context(c.root);PortalTrialState.receive(another,json(),now,SystemClock.elapsed)
  check(!PortalTrialState.snapshot.value!!.welcome(now));check(TrialAcknowledgement.pending(another)==TrialChoice(snapshot.mac,until,"due"))
 }
 test("successful network acknowledgement clears retry while keeping initial notice suppressed") {
  val c=context("confirmed");PortalTrialState.receive(c,json(),now,SystemClock.elapsed);TrialAcknowledgement.mark(c,PortalTrialState.snapshot.value!!,"now")
  TrialAcknowledgement.clearPending(c,TrialAcknowledgement.pending(c)!!)
  check(TrialAcknowledgement.pending(c)==null);check(TrialAcknowledgement.acknowledged(c,"02:00:00:00:00:01",until))
 }
 test("server acknowledgement suppresses welcome after reinstall without local preferences") {
  PortalTrialState.receive(context("reinstalled"),json(ack=true),now,SystemClock.elapsed)
  check(!PortalTrialState.snapshot.value!!.welcome(now))
 }
 test("local acknowledgement belongs only to matching MAC and exact trial expiry") {
  val c=context("bound");PortalTrialState.receive(c,json(),now,SystemClock.elapsed);TrialAcknowledgement.mark(c,PortalTrialState.snapshot.value!!,"due")
  PortalTrialState.receive(c,json(mac="02:00:00:00:00:02"),now,SystemClock.elapsed);check(PortalTrialState.snapshot.value!!.welcome(now))
  PortalTrialState.receive(c,json(expires=until+86400000L),now,SystemClock.elapsed);check(PortalTrialState.snapshot.value!!.welcome(now))
 }
 test("day7 reaches original deadline even when deferred; paid/blocked access never treated as trial") {
  check(!TrialNoticePolicy.trialExpired("trial",until,until-1,0));check(TrialNoticePolicy.trialExpired("trial",until,until,0))
  check(TrialNoticePolicy.trialExpired("expired",until,until+1,0))
  for(state in listOf("licensed","blocked")){check(!TrialNoticePolicy.trialExpired(state,until,until+1,0));check(!TrialNoticePolicy.showWelcome(state,until,now,1,false))}
  check(!TrialNoticePolicy.trialExpired("expired",until,until+1,until-1))
 }
 test("license activation clears trial state and stale UI cannot block the licensed device") {
  val c=context("activated");PortalTrialState.receive(c,json(),now,SystemClock.elapsed);val stale=PortalTrialState.snapshot.value!!
  PortalTrialState.receive(c,json(state="licensed"),now,SystemClock.elapsed);check(PortalTrialState.snapshot.value==null)
  PortalAccess.message.value=null;PortalTrialState.expireIfDue(stale);check(PortalAccess.message.value==null)
 }
 test("preference failure leaves acknowledgement unset and throws instead of losing the choice") {
  val c=context("failure");PortalTrialState.receive(c,json(),now,SystemClock.elapsed);c.failCommit=true
  check(runCatching{TrialAcknowledgement.mark(c,PortalTrialState.snapshot.value!!,"due")}.isFailure)
  check(!TrialAcknowledgement.acknowledged(c,"02:00:00:00:00:01",until))
 }
 test("monotonic server anchor reaches expiry without relying on wall clock changes") {
  val c=context("clock");PortalTrialState.receive(c,json(expires=now+1000),now,SystemClock.elapsed)
  val s=PortalTrialState.snapshot.value!!;SystemClock.elapsed+=2000
  check(s.expired());PortalTrialState.expireIfDue(s);check(PortalTrialState.snapshot.value!!.state=="expired");check(PortalAccess.message.value!=null)
  SystemClock.elapsed-=2000
 }
 test("offline acknowledgement retry backs off across process recreation without resending catalogues") {
  val c=context("retry");PortalTrialState.receive(c,json(),now,SystemClock.elapsed)
  TrialAcknowledgement.mark(c,PortalTrialState.snapshot.value!!,"due");val choice=TrialAcknowledgement.pending(c)!!
  TrialAcknowledgement.failed(c,choice,1000);check(!TrialAcknowledgement.readyToRetry(Context(c.root),60999));check(TrialAcknowledgement.readyToRetry(c,61000))
  TrialAcknowledgement.failed(c,choice,61000);check(!TrialAcknowledgement.readyToRetry(c,360999));check(TrialAcknowledgement.readyToRetry(c,361000))
  check(TrialAcknowledgement.pending(c)==choice);check(TrialAcknowledgement.acknowledged(c,choice.mac,until))
 }
 println("$tests trial policy/persistence scenarios passed")
}
