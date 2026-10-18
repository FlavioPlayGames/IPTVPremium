package br.com.flaviotorres.iptvpremium.data.repository
import br.com.flaviotorres.iptvpremium.data.remote.PlaylistHttpClient
import org.json.JSONObject
import org.json.JSONArray
private fun parse(body: String): SeriesEpisodesResult? {
 val client=object:PlaylistHttpClient {
  override suspend fun get(url:String)=error("parser test must not request network")
  override suspend fun readLines(url:String,onLine:suspend(String)->Unit) {}
 }
 val repository=SeriesEpisodesRepository(client)
 val method=repository.javaClass.declaredMethods.single { it.name == "parseResponse" }
 method.isAccessible=true
 return (if (method.parameterCount == 3) method.invoke(repository,"123",body,null) else method.invoke(repository,"123",body)) as SeriesEpisodesResult?
}
fun main() {
 fun pass(label:String,body:String,expect:(SeriesEpisodesResult?)->Boolean) {
  check(expect(parse(body))) {label};println("PASS $label")
 }
 pass("canonical season map", """{"episodes":{"1":[{"id":"10","season":1,"episode_num":2}]}}""") { it?.episodes?.single()?.let{e->e.id=="10" && e.seasonNumber==1 && e.episodeNumber==2}==true }
 pass("flat array", """{"episodes":[{"id":"10","season":2,"episode_num":3}]}""") {it?.episodes?.single()?.seasonNumber==2}
 pass("PHP grouped arrays", """{"episodes":[[],[{"id":"10","episode_num":1}]]}""") {it?.episodes?.single()?.seasonNumber==1}
 pass("PHP season zero retained", """{"episodes":[[{"id":"10","episode_num":1}],[{"id":"11","episode_num":1}]]}""") {it?.episodes?.map{e->e.seasonNumber}==listOf(0,1)}
 pass("keyed episode objects", """{"episodes":{"2":{"0":{"id":"10","episode_num":1},"1":{"episode_id":"11","episode_num":2}}}}""") {it?.episodes?.map{e->e.id to e.seasonNumber}==listOf("10" to 2,"11" to 2)}
 pass("explicit season overrides object bucket", """{"episodes":{"1":{"0":{"id":"10","season":3,"episode_num":1}}}}""") {it?.episodes?.single()?.seasonNumber==3}
 pass("explicit season overrides array bucket", """{"episodes":[[],[{"id":"10","season":4,"episode_num":1}]]}""") {it?.episodes?.single()?.seasonNumber==4}
 pass("keyed flat episode map defaults first season", """{"episodes":{"100":{"id":"10","episode_num":1}}}""") {it?.episodes?.single()?.seasonNumber==1}
 pass("wrapped season episode list", """{"episodes":[{"season_number":2,"episodes":[{"id":"10","episode_num":1}]}]}""") {it?.episodes?.single()?.seasonNumber==2}
 pass("existing id-only flat records preserved", """{"episodes":[{"id":"10"}]}""") {it?.episodes?.single()?.id=="10"}
 pass("numeric IDs preserved", """{"episodes":{"1":[{"id":9876543210,"episode_num":1}]}}""") {it?.episodes?.single()?.id=="9876543210"}
 pass("ID never guessed from key", """{"episodes":{"1":{"100":{"title":"Missing actual ID","episode_num":1}}}}""") {it?.episodes?.isEmpty()==true}
 pass("malformed ID object rejected", """{"episodes":{"1":[{"id":{"value":"10"},"episode_num":1}]}}""") {it?.episodes?.isEmpty()==true}
 pass("metadata outside episodes ignored", """{"info":{"id":"99","episode_num":1},"seasons":[{"id":"9","episode_num":1}],"episodes":{"info":{"id":"88","episode_num":1},"1":[{"id":"10","episode_num":1,"info":{"id":"77","episode_num":2}}]}}""") {it?.episodes?.map{e->e.id}==listOf("10")}
 pass("season metadata not episode", """{"episodes":{"1":{"id":"9","season_number":1,"episode_count":10,"name":"Season1"}}}""") {it?.episodes?.isEmpty()==true}
 pass("negative episode or season rejected", """{"episodes":{"1":[{"id":"10","season":-1,"episode_num":1},{"id":"11","season":1,"episode_num":0}]}}""") {it?.episodes?.isEmpty()==true}
 pass("duplicates removed without dropping distinct seasons", """{"episodes":{"1":[{"id":"10","episode_num":1},{"id":"10","episode_num":1}],"2":[{"id":"10","episode_num":1}]}}""") {it?.episodes?.size==2}
 var deep:Any=JSONObject().put("id","10").put("episode_num",1)
 repeat(12) {deep=JSONArray().put(deep)}
 pass("depth bounded",JSONObject().put("episodes",deep).toString()) {it?.episodes?.isEmpty()==true}
 val many=JSONArray();repeat(10_100){many.put(JSONObject().put("id",it+1).put("episode_num",it+1))}
 pass("accepted episode count bounded",JSONObject().put("episodes",many).toString()) {it?.episodes?.size==10_000}
 pass("malformed root rejected","<html>Unavailable</html>") {it==null}
 pass("empty response produces no invented episodes", """{"info":{"name":"Series"},"episodes":{}}""") {it?.episodes?.isEmpty()==true && it.seasons.isEmpty()}
 val good="""{"episodes":{"1":[{"id":"10","season":1,"episode_num":1}]}}"""
 pass("leading BOM accepted", "\uFEFF$good") {it?.episodes?.single()?.id=="10"}
 pass("whitespace before BOM accepted", " \r\n\uFEFF$good") {it?.episodes?.single()?.id=="10"}
 pass("repeated BOM with interleaved whitespace accepted", "\uFEFF \r\n\uFEFF\uFEFF\t$good") {it?.episodes?.single()?.id=="10"}
 pass("Unicode leading whitespace accepted", "\u00A0\r\n\uFEFF$good") {it?.episodes?.single()?.id=="10"}
 pass("ordinary surrounding whitespace accepted", " \r\n\t$good \r\n") {it?.episodes?.single()?.id=="10"}
 pass("HTML error is not stripped into JSON", "<html>access denied</html>$good") {it==null}
 pass("PHP warning is not stripped into JSON", "Warning: Undefined array key on line 1\n$good") {it==null}
 pass("array root remains rejected", "[$good]") {it==null}
 val privateValue="secret-user-4829:secret-pass-9137:https://private.invalid/video"
 val syntax=seriesJsonFailureSummary("{\"password\":\"$privateValue\"", org.json.JSONException("Unterminated object at character 37 of $privateValue"))
 check(syntax.contains("tipo=objeto_incompleto") && syntax.contains("pos=37"));println("PASS Android parser category and offset captured")
 check(!syntax.contains("secret") && !syntax.contains("password") && !syntax.contains("private.invalid") && !syntax.contains("9137"));println("PASS JSON diagnostic redacts keys values URLs and exception message")
 val javaSyntax=seriesJsonFailureSummary("{\"episodes\":", org.json.JSONException("Missing value at 12 [character 13 line 1]"))
 check(javaSyntax.contains("pos=12") && javaSyntax.contains("tipo=valor_ausente"));println("PASS JSON java parser offset captured")
 val unknown=seriesJsonFailureSummary("", org.json.JSONException("$privateValue"))
 check(unknown.contains("tipo=sintaxe") && unknown.contains("pos=-1") && !unknown.contains("secret"));println("PASS unknown private parser message never logged")
 val oversize=seriesJsonFailureSummary("{}", org.json.JSONException("Unterminated object at character 999999999 of $privateValue"))
 check(oversize.contains("pos=2"));println("PASS malformed parser position bounded without substring exception")

 val intact="""{"1":[{"id":"10","episode_num":2,"season":1,"title":"Actual episode"}]}"""
 pass("malformed optional synopsis does not block intact episodes", """{"info":{"plot":"He said "hello" today"},"episodes":$intact}""") {it?.episodes?.single()?.id=="10" && it.plot==null}
 pass("invalid optional escape does not block intact episodes", """{"info":{"plot":"bad\q"},"episodes":$intact}""") {it?.episodes?.single()?.episodeNumber==2}
 pass("complete episodes survive only missing outer brace", """{"info":{},"episodes":$intact""") {it?.episodes?.single()?.id=="10"}
 pass("malformed episode text is never guessed", """{"episodes":{"1":[{"id":"10","title":"bad "quote": here","episode_num":2}]}}""") {it==null}
 pass("truncated episode node remains rejected", """{"info":{"plot":"bad\q"},"episodes":{"1":[{"id":"10"""") {it==null}
 pass("recovery keeps actual IDs and explicit seasons", """{"info":{"plot":"bad\q"},"episodes":{"1":[{"id":"9988","season":3,"episode_num":4}]}}""") {it?.episodes?.single()?.let{e->e.id=="9988" && e.seasonNumber==3 && e.episodeNumber==4}==true}
 pass("recovery never invents ID from numeric key", """{"info":{"plot":"bad\q"},"episodes":{"1":{"999":{"title":"No ID"}}}}""") {it?.episodes?.isEmpty()==true}
 check(recoverCompleteSeriesEpisodes("""{"info":{"episodes":$intact},"invalid":}""")==null);println("PASS nested metadata episodes cannot be recovered as real series")
 check(recoverCompleteSeriesEpisodes("""{"info":{"plot":"bad\q"},"episodes":$intact,"episodes":$intact}""")==null);println("PASS ambiguous duplicate root episode nodes rejected")
 check(recoverCompleteSeriesEpisodes("""{"info":{"plot":"bad\q"},"episodes":$intact,"other":1}""")==null);println("PASS recovery rejects trailing fields rather than discarding unknown structure")
 check(recoverCompleteSeriesEpisodes("""{"episodes":$intact}garbage""")==null);println("PASS recovery rejects trailing garbage")
 check(recoverCompleteSeriesEpisodes("""{"episodes":$intact}}""")==null);println("PASS recovery rejects extra closing braces")
 check(recoverCompleteSeriesEpisodes("""{"plot":"text with \"episodes\":{} and more","invalid":}""")==null);println("PASS quoted metadata marker never treated as episode node")
 val http=object:PlaylistHttpClient {
  var calls=0
  override suspend fun get(url:String):String {calls++;return """{"info":{"plot":"bad\q"},"episodes":$intact}"""}
  override suspend fun readLines(url:String,onLine:suspend(String)->Unit) {}
 }
 val recovered=kotlinx.coroutines.runBlocking {SeriesEpisodesRepository(http).load(br.com.flaviotorres.iptvpremium.domain.model.PlaylistSource.XtreamCodes("fixture","Fixture",serverUrl="http://example.invalid",username="fixture",password="fixture"),"7")}
 check(recovered?.episodes?.single()?.id=="10" && http.calls==1);println("PASS malformed metadata loads episodes in one HTTP call without manual retry")

}
