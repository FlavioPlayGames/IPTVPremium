from pathlib import Path
import sqlite3,re
w=Path('/workspace/xtream-on-demand-fix');root=Path('/workspace/restored-navigation-build');code=(root/'app/src/main/java/br/com/flaviotorres/iptvpremium/data/cache/dao/PlaylistCacheDao.kt').read_text()
ddl=re.findall(r'connection.execSQL\("([^"\n]+)"\)',(root/'app/build/generated/ksp/desempenho/kotlin/br/com/flaviotorres/iptvpremium/data/cache/PlaylistCacheDatabase_Impl.kt').read_text())
c=sqlite3.connect(':memory:')
for q in ddl:
 if q.startswith(('CREATE TABLE','CREATE INDEX')):c.execute(q)
queries={m.group(2):m.group(1) for m in re.finditer(r'@Query\("([^"\n]+)"\)\s*(?:suspend )?fun (\w+)',code)}
def row(table,playlist,gen,id,cat):
 fields={r[1]:None for r in c.execute('PRAGMA table_info('+table+')')}
 fields.update(playlistId=playlist,generationId=gen,name='Test',categoryId=cat,providerOrder=0)
 fields['movieId' if table.endswith('movies') else 'seriesId']=id
 for _,name,typ,notnull,*_ in c.execute('PRAGMA table_info('+table+')'):
  if notnull and fields[name] is None:fields[name]=0 if typ=='INTEGER' else ''
 c.execute('INSERT INTO '+table+' ('+','.join(fields)+') VALUES('+','.join('?'*len(fields))+')',list(fields.values()))
for type,table,kind in [('MOVIES','cached_movies','Movies'),('SERIES','cached_series','Series')]:
 for playlist,gen,id,cat in [('p','active','old','1'),('p','active','keep','2'),('other','active','other','1'),('p','stage','new','1')]:row(table,playlist,gen,id,cat)
 c.commit();params=dict(playlist='p',stage='stage',generation='active',category='1')
 before=c.execute('SELECT * FROM '+table).fetchall()
 c.execute('BEGIN');c.execute(queries['deleteLazy'+kind],params);c.execute(queries['copyLazy'+kind],params);c.rollback()
 assert c.execute('SELECT * FROM '+table).fetchall()==before
 with c:
  c.execute(queries['deleteLazy'+kind],params);c.execute(queries['copyLazy'+kind],params)
 col='movieId' if type=='MOVIES' else 'seriesId'
 assert set(c.execute('SELECT playlistId,'+col+' FROM '+table+' WHERE generationId="active"'))=={('p','new'),('p','keep'),('other','other')}
 params['category']=None
 with c:
  c.execute(queries['deleteLazy'+kind],params);c.execute(queries['copyLazy'+kind],params)
 assert set(c.execute('SELECT playlistId,'+col+' FROM '+table+' WHERE generationId="active"'))=={('p','new'),('other','other')}
 print('PASS exact DAO SQL '+type+': category isolation, playlist isolation, full replacement and transaction rollback')
assert c.execute('PRAGMA integrity_check').fetchone()[0]=='ok'
print('PASS SQLite integrity_check')
