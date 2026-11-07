package br.com.flaviotorres.iptvpremium.core.player

import kotlinx.coroutines.runBlocking
import org.json.JSONArray
import org.json.JSONObject
import java.net.URI
import java.io.IOException

private var checks=0
private fun verify(ok:Boolean,label:String){check(ok){label};checks++;println("PASS $label")}
private class Accounts:OpenSubtitleAccounts {
    var value:OpenSubtitleSession?=null;var gen=0L
    override fun generation()=gen
    override fun read()=value
    override fun save(token:String,base:String,expiresAt:Long,appFingerprint:String,expectedGeneration:Long,accountScope:String):Boolean {
        if(gen!=expectedGeneration)return false
        gen++;value=OpenSubtitleSession(token,base,accountScope,expiresAt,appFingerprint);return true
    }
    override fun disconnect(expectedToken:String?):Boolean {
        if(expectedToken!=null && value?.token!=expectedToken)return false
        value=null;gen++;return true
    }
}
private data class Request(val url:String,val method:String,val headers:Map<String,String>,val body:String?)
private class Transport:OpenSubtitleTransport {
    val requests=ArrayList<Request>()
    var handler:(Request)->OpenSubtitleResponse={OpenSubtitleResponse(200,"{}".toByteArray())}
    override suspend fun request(url:String,method:String,headers:Map<String,String>,body:String?,limit:Int):OpenSubtitleResponse {
        val request=Request(url,method,headers,body);requests.add(request);return handler(request)
    }
}
private fun json(value:JSONObject,code:Int=200)=OpenSubtitleResponse(code,value.toString().toByteArray())
private fun fixture(lang:String="pt-br",id:Long=123,file:Long=11,year:Int=2002,season:Int?=null,episode:Int?=null):JSONObject {
    val feature=JSONObject().put("feature_type",if(season==null)"Movie" else "Episode")
        .put("title","Harry Potter").put("parent_title","Example Series").put("year",year)
        .put("tmdb_id",id).put("parent_tmdb_id",id)
    if(season!=null)feature.put("season_number",season).put("episode_number",episode)
    return JSONObject().put("attributes",JSONObject().put("language",lang).put("release","edition")
        .put("feature_details",feature).put("files",JSONArray().put(JSONObject().put("file_id",file))))
}
private suspend fun expect(problem:OpenSubtitleProblem,label:String,block:suspend()->Unit) {
    try{block();error("expected $problem: $label")}catch(e:OpenSubtitleException){verify(e.problem==problem,label)}
}
fun main()=runBlocking {
    val baseQuery=OpenSubtitleQuery("Harry Potter",2002,"123")
    val many=JSONObject().put("data",JSONArray().put(fixture()).put(fixture("en",file=12))
        .put(fixture("es",file=13)).put(fixture("fr",file=14)).put(fixture(id=999,file=15))
        .put(fixture(file=16,year=2003)).put(fixture(file=11)))
    val hits=decodeOpenSubtitleHits(many,baseQuery)
    verify(hits.map{it.language}==listOf("pt-BR","en","es"),"three languages with Brazilian Portuguese first")
    verify(hits.map{it.fileId}==listOf("11","12","13"),"wrong works, wrong years and duplicate files excluded")
    val episodic=OpenSubtitleQuery("Example Series",2020,"123",2,3)
    val episodes=JSONObject().put("data",JSONArray().put(fixture(season=2,episode=3)).put(fixture(file=22,season=2,episode=4))
        .put(fixture(file=23,season=1,episode=3)).put(fixture(id=999,file=24,season=2,episode=3)))
    verify(decodeOpenSubtitleHits(episodes,episodic).map{it.fileId}==listOf("11"),"episode search requires correct parent series, season and episode")
    val params=episodic.parameters()
    verify("parent_tmdb_id=123" in params && "season_number=2" in params && "episode_number=3" in params,"series ID is never sent as an episode TMDB ID")
    verify("languages=pt-br%2Cen%2Ces" in params,"three search languages in one request")
    verify(decodeOpenSubtitleHits(JSONObject().put("data",JSONArray().put(fixture())),OpenSubtitleQuery("Different Work",2002,null)).isEmpty(),"text lookup rejects unrelated titles")
    verify(openSubtitleApiBase("vip-api.opensubtitles.com")=="https://vip-api.opensubtitles.com/api/v1","official VIP API base accepted")
    for(url in listOf("api.opensubtitles.com.evil.invalid","http://api.opensubtitles.com","https://api.opensubtitles.com@evil.invalid","https://api.opensubtitles.com:8080","https://api.opensubtitles.com/extra"))
        verify(openSubtitleApiBase(url)==null,"unsafe API base rejected")
    for(url in listOf("http://dl.opensubtitles.com/a.srt","https://opensubtitles.com.evil.invalid/a.srt","https://user@dl.opensubtitles.com/a.srt","https://127.0.0.1/a.srt"))
        verify(!openSubtitleDownloadUrl(url),"unsafe download URL rejected")
    val absent=Transport();val absentClient=OpenSubtitlesClient({OpenSubtitleAppConfig("","App v1")},Accounts(),absent)
    expect(OpenSubtitleProblem.APP_UNAVAILABLE,"missing application registration performs no login") {absentClient.login("u","p")}
    verify(absent.requests.isEmpty(),"no request with missing app key")
    val app=OpenSubtitleAppConfig("fake-test-app-key-1","IPTVPremium v1.13")
    val accounts=Accounts();val transport=Transport();var clock=1000L
    val client=OpenSubtitlesClient({app},accounts,transport){clock}
    transport.handler={json(JSONObject().put("token","fake-test-token").put("base_url","api.opensubtitles.com"))}
    client.login(" viewer "," password with spaces ")
    val login=transport.requests.single()
    verify(JSONObject(login.body!!).getString("password")==" password with spaces " && JSONObject(login.body).getString("username")=="viewer","login preserves password and trims user")
    verify(accounts.read()?.token=="fake-test-token" && accounts.read()?.apiBase=="https://api.opensubtitles.com/api/v1","only token session is passed to account storage")
    val scope=client.scope()!!
    transport.handler={json(many)}
    val result=client.search(baseQuery)
    verify(result.first==scope && result.second.size==3,"search returns three verified language options")
    verify(transport.requests.none{it.url.endsWith("/download")},"login and search never mint a download link")
    verify(transport.requests.last().headers["Authorization"]=="Bearer fake-test-token","search uses viewer session")
    val countBeforeLanguages=transport.requests.size
    transport.handler={r->
        val data=when {
            "languages=pt-br%2Cen%2Ces" in r.url->JSONObject().put("total_pages",4).put("data",JSONArray().put(fixture("en",file=12)))
            "languages=pt-br&" in r.url->JSONObject().put("data",JSONArray().put(fixture()))
            else->JSONObject().put("data",JSONArray().put(fixture("es",file=13)))
        }
        json(data)
    }
    verify(client.search(baseQuery).second.map{it.language}==listOf("pt-BR","en","es"),"missing languages recovered when English crowds out a multi-page search")
    verify(transport.requests.size-countBeforeLanguages==3,"language recovery queries only the missing languages")
    transport.handler={r->when {
        r.url.endsWith("/download")->json(JSONObject().put("link","https://dl.opensubtitles.com/file.srt?signature=opaque"))
        else->OpenSubtitleResponse(200,"1\n00:00:01,000 --> 00:00:02,000\nHello".toByteArray())
    }}
    val bytes=client.download("11",scope)
    verify(bytes.isNotEmpty() && transport.requests.count{it.url.endsWith("/download")}==1,"selection mints exactly one download link")
    verify(JSONObject(transport.requests.first{it.url.endsWith("/download")}.body!!).getLong("file_id")==11L,"selected file ID is sent, not subtitle ID")
    val fileRequest=transport.requests.last()
    verify(fileRequest.headers.keys==setOf("User-Agent"),"public subtitle download never receives credentials or API key")
    transport.handler={json(JSONObject().put("link","https://evil.invalid/steal"))}
    val before=transport.requests.size
    expect(OpenSubtitleProblem.INVALID_RESPONSE,"untrusted download link rejected before GET") {client.download("11",scope)}
    verify(transport.requests.size==before+1,"untrusted host receives no request")
    transport.handler={OpenSubtitleResponse(200,"{}".toByteArray())}
    expect(OpenSubtitleProblem.INVALID_RESPONSE,"malformed successful search is not an empty search result") {client.search(baseQuery)}
    transport.handler={throw IOException("secret token and URL must never escape")}
    expect(OpenSubtitleProblem.UNAVAILABLE,"transport exceptions become fixed safe messages") {client.search(baseQuery)}
    transport.handler={accounts.disconnect();json(many)}
    expect(OpenSubtitleProblem.LOGIN_REQUIRED,"logout during search discards late results") {client.search(baseQuery)}
    verify(client.scope()==null,"logout removes active session")
    transport.handler={json(JSONObject().put("token","fake-test-token").put("base_url","evil.invalid"))}
    expect(OpenSubtitleProblem.INVALID_RESPONSE,"login cannot install a malicious API host") {client.login("viewer","password")}
    verify(accounts.read()==null,"invalid host is never saved")
    transport.handler={json(JSONObject().put("token","fake-test-token").put("base_url","api.opensubtitles.com"))}
    client.login("viewer","password");val oldScope=client.scope()!!
    client.login("viewer","password")
    verify(client.scope()==oldScope,"same account keeps a stable cache identity after login renewal")
    transport.handler={accounts.save("same-account-new-token","https://api.opensubtitles.com/api/v1",900000,app.fingerprint,accounts.generation(),oldScope);OpenSubtitleResponse(401,byteArrayOf())}
    expect(OpenSubtitleProblem.SESSION_EXPIRED,"stale 401 from the same account is rejected") {client.search(baseQuery)}
    verify(accounts.read()?.token=="same-account-new-token" && client.scope()==oldScope,"stale 401 cannot clear a newer session of the same account")
    transport.handler={accounts.save("new-session-token","https://api.opensubtitles.com/api/v1",900000,app.fingerprint,accounts.generation(),"different-account");OpenSubtitleResponse(401,byteArrayOf())}
    expect(OpenSubtitleProblem.SESSION_EXPIRED,"old request reports expired authentication") {client.search(baseQuery)}
    verify(accounts.read()?.token=="new-session-token","old 401 cannot disconnect a newer login")
    expect(OpenSubtitleProblem.SESSION_EXPIRED,"old account selection cannot mint a new account download") {client.download("11",oldScope)}
    val limitApp=OpenSubtitleAppConfig("fake-test-app-key-2","App v1")
    val limited=Transport();val limitedClient=OpenSubtitlesClient({limitApp},Accounts(),limited){clock}
    limited.handler={OpenSubtitleResponse(429,byteArrayOf(),120)}
    expect(OpenSubtitleProblem.LIMITED,"429 reported as limit, not invalid password") {limitedClient.login("u","p")}
    expect(OpenSubtitleProblem.LIMITED,"Retry-After prevents immediate repeated requests") {limitedClient.login("u","p")}
    verify(limited.requests.size==1,"cooldown actually suppresses network traffic")
    clock+=121000
    limited.handler={json(JSONObject().put("token","fake-test-token"))}
    limitedClient.login("u","p")
    verify(limited.requests.size==2,"requests resume after provider cooldown")
    val raceAccounts=Accounts();val raceTransport=Transport();val raceClient=OpenSubtitlesClient({app},raceAccounts,raceTransport){clock}
    raceTransport.handler={raceAccounts.disconnect();json(JSONObject().put("token","fake-test-token"))}
    expect(OpenSubtitleProblem.STORAGE,"logout prevents a concurrent login from restoring the session") {raceClient.login("u","p")}
    verify(raceAccounts.read()==null,"concurrent logout remains effective")
    println("$checks OpenSubtitles checks passed; mocked provider, no real API access.")
}
