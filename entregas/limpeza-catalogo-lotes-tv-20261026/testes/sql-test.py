from pathlib import Path
import re,sqlite3,random
w=Path('/workspace/cleanup-index-fix');r=Path('/workspace/backup-reviewed-build/app')
before=(w/'before/data/cache/dao/PlaylistCacheDao.kt').read_text();after=(r/'src/main/java/br/com/flaviotorres/iptvpremium/data/cache/dao/PlaylistCacheDao.kt').read_text()
assert before.replace('            ORDER BY rowid\n','')==after
schema=(r/'build/kspCaches/desempenho/backups/kotlin/br/com/flaviotorres/iptvpremium/data/cache/PlaylistCacheDatabase_Impl.kt').read_text()
ddl=re.findall(r'connection.execSQL\("(CREATE (?:TABLE|(?:UNIQUE )?INDEX)[^"\n]+)"\)',schema)
if not ddl:
 ddl=re.findall(r'"(CREATE (?:TABLE|(?:UNIQUE )?INDEX)[^"\n]+)"',schema)
assert ddl
c=sqlite3.connect(':memory:')
for sql in ddl:c.execute(sql)
for name in ['categories','channels','movies','series','episodes']:
 table='cached_'+name
 pattern=r'@Query\(\s*"""\s*(DELETE FROM '+table+r'\s+WHERE rowid IN \(.*?LIMIT :limit\s*\))\s*"""'
 sql=re.search(pattern,after,re.S)[1]
 cols=list(c.execute('pragma table_info('+table+')'))
 for playlist,generation in [('p','old'),('p','active'),('other','old')]:
  rows=[]
  for i in range(1200):
   row=[]
   for _,col,typ,nn,default,pk in cols:
    if col=='playlistId':v=playlist
    elif col=='generationId':v=generation
    elif 'INT' in typ:v=i
    elif 'REAL' in typ:v=float(i)
    else:v=f'{col}-{i:06}'
    row.append(v)
   rows.append(row)
  random.Random(11).shuffle(rows)
  c.executemany('INSERT INTO '+table+' VALUES('+','.join('?' for _ in cols)+')',rows)
 plan=list(c.execute('EXPLAIN QUERY PLAN '+sql,dict(playlistId='p',generationId='old',limit=500)))
 assert not any('TEMP B-TREE' in row[-1] for row in plan),plan
 untouched=list(c.execute('SELECT * FROM '+table+" WHERE NOT (playlistId='p' AND generationId='old') ORDER BY playlistId,generationId,rowid"))
 assert c.execute(sql,dict(playlistId='p',generationId='old',limit=500)).rowcount==500
 c.commit() # pause/resume persisted progress
 assert c.execute(sql,dict(playlistId='p',generationId='old',limit=500)).rowcount==500
 assert c.execute(sql,dict(playlistId='p',generationId='old',limit=500)).rowcount==200
 assert c.execute(sql,dict(playlistId='p',generationId='old',limit=500)).rowcount==0
 assert list(c.execute('SELECT * FROM '+table+" WHERE NOT (playlistId='p' AND generationId='old') ORDER BY playlistId,generationId,rowid"))==untouched
 print('PASS',table,'actual Room schema / exact DAO SQL: bounded deletes, resumable progress, other generation+playlist intact, no sort')
print('PASS source diff: only five cleanup ORDER BY removed; all other DAO queries unchanged')
