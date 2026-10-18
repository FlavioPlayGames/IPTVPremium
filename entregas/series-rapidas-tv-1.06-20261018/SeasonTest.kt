package br.com.flaviotorres.iptvpremium.data.repository
fun main() {
 fun pass(n:String,b:Boolean){check(b){n};println("PASS $n")}
 fun ep(id:String,s:Int)=SeriesEpisodeInfo(id,s,1,"Fixture",null,null,null,null)
 val episodes=listOf(ep("one",1),ep("two",2),ep("three",3),ep("special",0))
 pass("abre na temporada do episodio assistido quando o ID existe nesta lista",preferredSeriesSeason(episodes,null,"three")==3)
 pass("escolha manual da rota prevalece sobre historico",preferredSeriesSeason(episodes,2,"three")==2)
 pass("sem historico deixa a tela escolher primeira temporada disponivel",preferredSeriesSeason(episodes,null,null)==null)
 pass("episodio de outra lista nao inventa temporada",preferredSeriesSeason(episodes,null,"foreign")==null)
 pass("temporada removida usa episodio confirmado se existir",preferredSeriesSeason(episodes,9,"two")==2)
 pass("ID repetido entre temporadas nao escolhe vinculo ambiguo",preferredSeriesSeason(episodes+ep("three",1),null,"three")==null)
 pass("temporada zero escolhida continua valida quando tem episodios",preferredSeriesSeason(episodes,0,"three")==0)
 pass("sem episodios reais nao seleciona temporada",preferredSeriesSeason(emptyList(),1,"three")==null)
}
