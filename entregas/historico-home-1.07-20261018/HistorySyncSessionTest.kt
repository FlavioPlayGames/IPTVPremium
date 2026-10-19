package br.com.flaviotorres.iptvpremium.data.history.sessiontests
import br.com.flaviotorres.iptvpremium.data.history.HistorySyncSession
import kotlinx.coroutines.*
fun main() = runBlocking {
 fun test(name: String, block: () -> Unit) { block(); println("PASS $name") }
 val session = HistorySyncSession()
 val cold = session.request("A")
 test("abertura fria silenciosa") { check(!cold.announce && cold.phase == HistorySyncSession.Phase.PENDING) }
 test("mesma lista nao cria outro trabalho enquanto pendente") { check(session.request("A").id == cold.id) }
 test("apenas um executor pode iniciar a busca") { check(session.start(cold.id)); check(!session.start(cold.id)) }
 test("retorno do segundo plano preserva trabalho em andamento") { check(session.request("A").id == cold.id && session.state.value?.phase == HistorySyncSession.Phase.RUNNING) }
 test("finalizacao silenciosa nao emite confirmacao") { check(session.complete(cold.id)); check(!session.consumeCompletion(cold.id)) }
 test("navegar ou retomar apos concluir nao repete a busca") { check(session.request("A").id == cold.id); check(!session.start(cold.id)) }
 val switched = session.request("B", announce=true, restart=true)
 test("troca de lista agenda mensagem na Home") { check(switched.id != cold.id && switched.announce && session.start(switched.id)) }
 test("nao anuncia antes da conclusao") { check(!session.consumeCompletion(switched.id)) }
 test("lista antiga nao pode concluir importacao nova") { check(!session.complete(cold.id)); session.release(cold.id); check(session.state.value?.phase == HistorySyncSession.Phase.RUNNING) }
 test("conclusao entrega uma unica mensagem independente da rota") { check(session.complete(switched.id)); check(session.consumeCompletion(switched.id)); check(!session.consumeCompletion(switched.id)) }
 val back = session.request("A", announce=true, restart=true)
 test("voltar a lista anterior realiza nova atualizacao") { check(back.id != cold.id && back.announce) }
 test("cancelamento por recriacao pode retomar sem criar outra selecao") { check(session.start(back.id));session.release(back.id);check(session.state.value?.phase==HistorySyncSession.Phase.PENDING);check(session.start(back.id)) }
 test("falha nao confirma sucesso nem repete automaticamente") { check(session.fail(back.id));check(!session.consumeCompletion(back.id));check(session.request("A").id==back.id);check(!session.start(back.id)) }
 session.reset()
 val reopened = session.request("A")
 test("fechar ou novo processo reinicia busca silenciosamente") { check(reopened.id != back.id && !reopened.announce && session.start(reopened.id)) }
 test("tarefa da sessao anterior nao modifica a nova") { session.release(back.id);check(!session.complete(back.id));check(session.state.value?.id==reopened.id && session.state.value?.phase==HistorySyncSession.Phase.RUNNING) }
 val handoff = HistorySyncSession(); val ticket = handoff.request("C")
 check(handoff.start(ticket.id))
 val replacement = async { handoff.awaitStart(ticket.id) }
 yield()
 test("nova tela aguarda executor anterior liberar sem duplicar chamadas") { check(!replacement.isCompleted) }
 handoff.release(ticket.id)
 check(replacement.await())
 test("recriacao assume trabalho pendente sem ficar preso") { check(handoff.state.value?.phase==HistorySyncSession.Phase.RUNNING) }
 val supersededWaiter = async { handoff.awaitStart(ticket.id) }
 yield()
 handoff.request("D",announce=true)
 val obsoleteClaim = supersededWaiter.await()
 test("troca de lista encerra espera antiga sem apropriar trabalho novo") { check(!obsoleteClaim && handoff.state.value?.playlist=="D") }

 val cacheDir = java.nio.file.Files.createTempDirectory("history-session-cache").toFile()
 val cacheSession=HistorySyncSession()
 test("primeira observacao de cache nao duplica abertura fria") { check(!cacheSession.cacheWasCleared(cacheDir)) }
 test("retomar com cache intacto nao agenda importacao") { check(!cacheSession.cacheWasCleared(cacheDir));check(!cacheSession.cacheWasCleared(cacheDir)) }
 java.io.File(cacheDir,"history-session.marker").delete()
 test("limpeza externa de cache e reconhecida no mesmo processo") { check(cacheSession.cacheWasCleared(cacheDir)) }
 test("mesma limpeza de cache nao e notificada duas vezes") { check(!cacheSession.cacheWasCleared(cacheDir)) }
 cacheSession.reset()
 java.io.File(cacheDir,"history-session.marker").delete()
 test("fechar e reabrir com cache vazio mantem uma unica abertura silenciosa") { check(!cacheSession.cacheWasCleared(cacheDir)) }
 val directoryIsFile=java.io.File(cacheDir,"not-a-directory").apply { writeText("x") }
 test("falha de gravacao do marcador nao provoca repeticoes de busca") { check(!cacheSession.cacheWasCleared(directoryIsFile));check(!cacheSession.cacheWasCleared(directoryIsFile)) }
 cacheDir.deleteRecursively()
 Unit

}
