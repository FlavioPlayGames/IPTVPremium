package br.com.flaviotorres.iptvpremium.data.cache
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.test.*
@OptIn(ExperimentalCoroutinesApi::class)
fun main()=runTest {
 fun ok(n:String,b:Boolean){check(b){n};println("PASS $n")}
 val input=MutableStateFlow("");val queries=mutableListOf<String>()
 backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)){input.settledCatalogSearch().collect{queries+=it}}
 runCurrent();ok("busca inicial vazia nao espera teclado",queries==listOf(""))
 var typed="";var lastInput=currentTime
 for(c in "Spider-Man") {typed+=c;input.value=typed;lastInput=currentTime;runCurrent();advanceTimeBy(25)}
 ok("dez caracteres aceitos sem executar dez buscas",typed=="Spider-Man" && queries==listOf(""))
 advanceTimeBy(350-(currentTime-lastInput)-1);runCurrent()
 ok("nao consulta antes da pausa de digitacao",queries==listOf(""))
 advanceTimeBy(1);runCurrent()
 ok("pausa consulta apenas texto completo",queries==listOf("","Spider-Man"))
 input.value="";runCurrent();ok("limpar busca tem resposta imediata",queries.last()=="")
 input.value="Star";runCurrent();advanceTimeBy(100);input.value="Stargate";runCurrent();advanceTimeBy(350);runCurrent()
 ok("consulta intermediaria e descartada",queries.last()=="Stargate" && "Star" !in queries)
 val queue=CatalogVerticalIntent(5)
 repeat(8){queue.request(2,100,true)}
 ok("oito comandos rapidos mantem terceira coluna enquanto foco aguarda",queue.desired==42)
 queue.focused(7);ok("foco atrasado nao sobrescreve destino mais recente",queue.desired==42)
 repeat(3){queue.request(7,100,false)}
 ok("inverter direcao durante fila mantem a terceira coluna",queue.desired==27)
 queue.focused(27);ok("confirmar destino libera intencao pendente",queue.desired==null)
 queue.request(27,100,true);queue.clear();ok("comando horizontal cancela movimento antigo",queue.desired==null)
 ok("ultima linha incompleta nao desvia terceira coluna para primeira",verticalCatalogTarget(47,5,52,true)==null)
 ok("primeira linha nao vira indice negativo",verticalCatalogTarget(2,5,100,false)==null)
 ok("fim da lista nao faz wrap",verticalCatalogTarget(52,5,53,true)==null)
 ok("resposta de seis colunas tambem preserva coluna",verticalCatalogTarget(8,6,80,true)==14)
 ok("catalogo vazio nao cria destino",verticalCatalogTarget(0,5,0,true)==null)
 ok("cache paginado tem janela limitada",CatalogMemoryPolicy.pagingWindow==180)
 val managedMb=CatalogMemoryPolicy.postersKb/1024+CatalogMemoryPolicy.detailsMb+CatalogMemoryPolicy.carouselMb+40+16+CatalogMemoryPolicy.playerBufferBytes/(1024*1024)
 ok("orcamento reserva mais de 180 MB para runtime GPU e video nativo",CatalogMemoryPolicy.totalTargetMb-managedMb>180)
 ok("pressao e detectada com pelo menos 70 MB de margem",CatalogMemoryPolicy.totalTargetMb-CatalogMemoryPolicy.pressureMb>=70)
}
