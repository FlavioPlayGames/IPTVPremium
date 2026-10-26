package br.com.flaviotorres.iptvpremium.data.cache.importer
import br.com.flaviotorres.iptvpremium.data.cache.importer.M3uEntryClassifier.Kind.*
fun main() {
 fun checkType(expected:M3uEntryClassifier.Kind,url:String,group:String,name:String,xui:Boolean=false,epg:Boolean=false) {
  check(M3uEntryClassifier.classify(url,group,name,xui,epg)==expected){"type mismatch for $group / $name"}
 }
 checkType(MOVIE,"http://example.test/play?id=1","♦ Ação ✔","Filme",true,false)
 checkType(MOVIE,"http://example.test/1.ts","Documentário","Filme",true,false)
 checkType(MOVIE,"http://example.test/1.m3u8","Categoria genérica","Filme",true,false)
 checkType(MOVIE,"http://example.test/play?id=1","Categoria genérica","Temporada de Caça",true,false)
 checkType(SERIES,"http://example.test/1.ts","Plataforma","Série S01E01",true,false)
 checkType(SERIES,"http://example.test/play?id=1","Programas de TV","Programa S08E01",true,false)
 checkType(SERIES,"http://example.test/play?id=1","SERIES | Diversos","Especial",true,false)
 checkType(LIVE_TV,"http://example.test/1.ts","Filmes e Séries","Canal AMC",true,true)
 checkType(LIVE_TV,"http://example.test/play?id=1","Seriados 24H","Canal",true,true)
 checkType(LIVE_TV,"http://example.test/play?id=1","Infantil","Canal",true,true)
 checkType(LIVE_TV,"http://example.test/live/u/p/1.ts","Filmes","Canal",true,false)
 checkType(MOVIE,"http://example.test/movie/u/p/1.ts","Categoria genérica","Filme",true,true)
 checkType(SERIES,"http://example.test/series/u/p/1.ts","Categoria genérica","Série",true,true)
 checkType(LIVE_TV,"http://example.test/play?next=/movie/abc.mp4","Categoria genérica","Canal")
 checkType(MOVIE,"http://example.test/u/p/1.MP4?token=abc#live","Ação","Filme")
 checkType(LIVE_TV,"http://example.test/u/p/1.ts","Categoria genérica","Conteúdo")
 checkType(LIVE_TV,"http://example.test/u/p/1.m3u8","Categoria genérica","Conteúdo")
 checkType(LIVE_TV,"http://example.test/u/p/1","Categoria genérica","Conteúdo")
 checkType(MOVIE,"http://example.test/u/p/1.ts","FILMES | AÇÃO","Filme")
 checkType(SERIES,"http://example.test/u/p/1.ts","Séries","Série S01E02")
 checkType(MOVIE,"http://example.test/play?id=1","FILMES | Ação","Filme",true,true)
 checkType(SERIES,"http://example.test/play?id=1","SERIES | Diversos","Show S01E01",true,true)
 checkType(SERIES,"http://example.test/play?id=1","SERIES | Diversos","Especial",true,true)
 checkType(LIVE_TV,"http://example.test/play?id=1","Canais","Programa S01E01",true,true)
 checkType(LIVE_TV,"http://example.test/play?id=1","Filmes e Séries","Canal",true,true)
 checkType(LIVE_TV,"http://example.test/play?id=1","Categoria genérica","Canal",false,true)
 println("PASS 26 classification cases: qualified XUI/EPG metadata, nameless paths, TS/HLS, explicit path priority, live 24H, movies with season words, generic M3U fallback and query isolation")
}
