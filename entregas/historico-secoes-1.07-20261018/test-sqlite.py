from pathlib import Path
import re,sqlite3,time,unicodedata
src=Path('/workspace/storage-work/app/src/main/java/br/com/flaviotorres/iptvpremium/data/cache/dao/PlaylistCacheDao.kt').read_text()
queries={kind:re.search(r'@Query\("([^"\n]+)"\)\s+suspend fun shared'+name+'Candidates',src).group(1) for kind,name in [('movies','Movie'),('series','Series')]}
c=sqlite3.connect(':memory:')
for kind in queries:
 ident='movieId' if kind=='movies' else 'seriesId'
 c.execute(f'create table cached_{kind}(playlistId text,generationId text,{ident} text,name text,year integer,posterUrl text,providerOrder integer,imdbId text,tmdbId text,primary key(playlistId,generationId,{ident}))')
 c.executemany(f'insert into cached_{kind} values(?,?,?,?,?,?,?,?,?)', [('to','g1',f'x{i:05}','Titulo diferente '+str(i),2020,None,i,None,None) for i in range(45000)])
 c.execute(f'insert into cached_{kind} values(?,?,?,?,?,?,?,?,?)',('to','g1','hit','CORAZÓN: VALIENTE [4K]',2020,None,50000,'tt1234567','777'))
 c.execute(f'insert into cached_{kind} values(?,?,?,?,?,?,?,?,?)',('other','g1','other','CORAZON VALIENTE',2020,None,50000,'tt1234567','777'))
 c.execute(f'insert into cached_{kind} values(?,?,?,?,?,?,?,?,?)',('to','old','old','CORAZON VALIENTE',2020,None,50000,'tt1234567','777'))
classes={}
for n in range(0x41,0x2000):
 ch=chr(n)
 if not ch.isalpha():continue
 folded=''.join(x for x in unicodedata.normalize('NFD',ch) if unicodedata.category(x)!='Mn').lower()
 if len(folded)==1:classes.setdefault(folded,[]).append(ch)
for key,letters in classes.items():
 for ch in (key.upper(),key.lower()):
  if len(ch)==1 and ch not in letters:letters.append(ch)
pattern='*'+'*'.join(''.join('['+''.join(classes[ch])+']' if ch in classes else ch for ch in token) for token in 'corazon valiente'.split())+'*'
results=[]
def test(name,condition):
 assert condition,name
 line='PASS '+name;results.append(line);print(line)
for kind,q in queries.items():
 args=dict(playlist='to',generation='g1',after='',pattern=pattern,ids='',imdbs='',tmdbs='')
 t=time.monotonic();rows=c.execute(q,args).fetchall()
 test(kind+' filtra acento maiusculo pontuacao e qualidade entre 45000 itens',len(rows)==1 and rows[0][0]=='hit')
 print(f'MEDICAO {kind} consultaSQLiteMs={(time.monotonic()-t)*1000:.1f} (nuvem, nao TV)')
 args.update(pattern='',ids='hit');test(kind+' consulta ID sem depender do titulo',len(c.execute(q,args).fetchall())==1)
 args.update(ids='',pattern='',imdbs='',tmdbs='');test(kind+' nenhum item solicitado nao retorna catalogo',not c.execute(q,args).fetchall())
 args.update(pattern=pattern,after='hit');test(kind+' paginacao respeita limite de ID e geracao',not c.execute(q,args).fetchall())
 if kind=='series':
  args.update(pattern='',after='',tmdbs='777');test('series ID externo localiza titulo diferente',len(c.execute(q,args).fetchall())==1)
  c.execute("update cached_series set imdbId=' TT1234567 ',tmdbId='000777' where seriesId='hit'")
  args.update(tmdbs='777');test('series normaliza zeros do TMDB sem varrer catalogo',len(c.execute(q,args).fetchall())==1)
  args.update(tmdbs='',imdbs='tt1234567');test('series normaliza caixa e espacos do IMDb',len(c.execute(q,args).fetchall())==1)
Path('/workspace/storage-toolchain/tests/historico-secoes-107/sqlite-results.txt').write_text('\n'.join(results)+'\n')
