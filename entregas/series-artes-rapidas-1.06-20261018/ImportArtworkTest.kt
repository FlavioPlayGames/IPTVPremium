package br.com.flaviotorres.iptvpremium.data.parser
fun main() {
 val parser=DefaultXtreamParser()
 fun tmdb(fields:String)=parser.parseSeries("""[{"series_id":77,"name":"A Armadilha (2026)",$fields}]""").single()
 fun test(name:String, value:Boolean) {check(value){name};println("PASS $name")}
 test("campo tmdb numerico preserva ID oficial",tmdb("\"tmdb\":123").tmdbId=="123")
 test("campo tmdb string preserva ID oficial",tmdb("\"tmdb\":\"123\"").tmdbId=="123")
 test("tmdb_id valido tem prioridade",tmdb("\"tmdb_id\":456,\"tmdb\":123").tmdbId=="456")
 test("tmdb_id invalido permite alternativa valida",tmdb("\"tmdb_id\":\"null\",\"tmdb\":123").tmdbId=="123")
 test("zero e ID negativo rejeitados",tmdb("\"tmdb_id\":0,\"tmdb\":-1").tmdbId==null)
 test("booleano e URL nao se tornam IDs",tmdb("\"tmdb_id\":true,\"tmdb\":\"https://invalid/123\"").tmdbId==null)
 test("ID acima do limite rejeitado",tmdb("\"tmdb\":9999999999999999").tmdbId==null)
 val art=tmdb("\"tmdb\":123,\"title_logo_url\":\"https://image.example/logo.png\"")
 test("logo explicita do provedor e importada",art.titleLogoUrl=="https://image.example/logo.png")
 test("ID de reproducao do provedor nao muda",art.id=="77")
}
