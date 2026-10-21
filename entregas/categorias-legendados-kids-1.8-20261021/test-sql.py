from pathlib import Path
import re,json,ast,sqlite3
r=Path('/workspace/kids-capas-work/java/br/com/flaviotorres/iptvpremium/data/cache/dao')
s=(r/'PlaylistCacheDao.kt').read_text();predicate=(r/'MovieSubtitleSql.kt').read_text().split('"""')[1]
def sql(method):
 at=s.index('fun '+method+'(');block=s[s.rfind('@Query(',0,at):at]
 expr=block[block.index('(')+1:block.rfind(')')]
 expr=re.sub(r'"""(.*?)"""',lambda m:json.dumps(m[1]),expr,flags=re.S).replace('MOVIE_IS_SUBTITLED_SQL',json.dumps(predicate))
 def evaluate(node):
  if isinstance(node,ast.Constant)and isinstance(node.value,str):return node.value
  if isinstance(node,ast.BinOp)and isinstance(node.op,ast.Add):return evaluate(node.left)+evaluate(node.right)
  raise ValueError(ast.dump(node))
 return evaluate(ast.parse(expr.strip(),mode='eval').body)
c=sqlite3.connect(':memory:');c.executescript('''CREATE TABLE cached_movies(playlistId TEXT,generationId TEXT,movieId TEXT,name TEXT,categoryId TEXT,providerOrder INT);CREATE TABLE cached_categories(playlistId TEXT,generationId TEXT,categoryId TEXT,type TEXT,name TEXT);''')
for cid,name in [('action','Ação'),('kids','Kids Infantil'),('leg','Filmes Legendados')]:c.execute('INSERT INTO cached_categories VALUES(?,?,?,?,?)',('p','g',cid,'MOVIES',name))
for n,title in enumerate(['Filme normal','Filme [L]','Filme [LEG]','Filme Legendado','Sem marca','Kids normal','Kids [L]','Sem categoria']):c.execute('INSERT INTO cached_movies VALUES(?,?,?,?,?,?)',('p','g',str(n),title,'leg'if n==4 else'kids'if n in[5,6]else None if n==7 else'action',n))
params=dict(playlistId='p',generationId='g',categoryId=None,searchQuery='',favoritesOnly=0,favoriteMovieIds='none',sortMode='DEFAULT',subtitledOnly=0,includeSubtitled=0,parentalExcludedIds='none',parentalAllowedIds='none',parentalRestrict=0,excluded='none',allowed='none',restrict=0)
def rows(method,**kw):return c.execute(sql(method),params|kw).fetchall()
checks=0
assert [r[2]for r in rows('getMoviesPagingSource')]==['0','5','7'];checks+=1
assert [r[2]for r in rows('getMoviesPagingSourceByCategory',categoryId='action')]==['0'];checks+=1
assert [r[2]for r in rows('getMoviesPagingSourceFiltered',searchQuery='Filme',categoryId='action',sortMode='A_Z')]==['0'];checks+=1
assert [r[2]for r in rows('getMoviesPagingSourceFiltered',subtitledOnly=1)]==['1','2','3','4','6'];checks+=1
assert [r[2]for r in rows('getMoviesPagingSourceFiltered',favoritesOnly=1,favoriteMovieIds='1')]==['1'];checks+=1
assert [r[2]for r in rows('getMoviesPagingSourceFiltered',includeSubtitled=1,searchQuery='[L]')]==['1','6'];checks+=1
assert rows('getMovieCount')==[(3,)];checks+=1
assert rows('getSubtitledMovieCount')==[(5,)];checks+=1
assert dict(rows('getMovieCountsByCategory'))=={None:1,'action':1,'kids':1};checks+=1
assert [r[2]for r in rows('getMoviesPagingSourceFiltered',favoritesOnly=1,favoriteMovieIds='6',parentalRestrict=1,parentalAllowedIds='kids')]==['6'];checks+=1
result=f'PASS: {checks} SQLite checks using the actual DAO annotation SQL and shared predicate. Simple grid, genre, search/order, subtitle markers, favorites, identity lookup, counts, Kids restriction. Does not execute Android Room instrumentation.\n'
Path('/workspace/storage-toolchain/tests/movie-categories/resultados-sql.txt').write_text(result);print(result)
