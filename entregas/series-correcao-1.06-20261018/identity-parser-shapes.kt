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
}
