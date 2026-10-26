package br.com.flaviotorres.iptvpremium.ui.kids
import br.com.flaviotorres.iptvpremium.ui.theme.awaitSeriesOpeningReady
import kotlinx.coroutines.*
fun main()=runBlocking {
    val gate=KidsLoadingCompletion()
    check(!gate.completeIfReady(false,false))
    check(!gate.completeIfReady(true,true))
    check(gate.completeIfReady(true,false))
    check(!gate.completeIfReady(true,false))
    val abandoned=KidsLoadingCompletion();abandoned.cancel()
    check(!abandoned.completeIfReady(true,false))
    println("PASS ready-only, failure/retry, once-only completion and late-completion cancellation")
    check(withTimeout(500){awaitSeriesOpeningReady(60_000,{true},{false})})
    check(!awaitSeriesOpeningReady(500,{true},{true}))
    var ready=false
    val pending=async {awaitSeriesOpeningReady(1500,{ready},{false})}
    yield();check(!pending.isCompleted);ready=true
    check(withTimeout(500){pending.await()})
    val closing=launch{awaitSeriesOpeningReady(60_000,{false},{false})}
    yield();closing.cancelAndJoin();check(closing.isCancelled)
    println("PASS immediate ready without forced wait; pending data, failure and coroutine cancellation")
    for(w in listOf(34,96,220,900)) {
        val region=clayFocusRegion(456,192,w,38)!!
        check(region.x>0 && region.y>0)
        check(region.x+region.width<456 && region.y+region.height<192)
        check(region.width==228 && region.height==96)
    }
    check(clayFocusRegion(456,192,96,38)!!.radius==clayFocusRegion(456,192,900,38)!!.radius)
    check(clayFocusRegion(456,192,0,38)==null)
    println("PASS one interior texture region without patch boundaries, fixed corner radius")
}
