import sqlite3,time,random
c=sqlite3.connect(':memory:')
c.executescript('CREATE TABLE episodes(playlistId TEXT,generationId TEXT,episodeId TEXT,seriesId TEXT,seasonNumber INTEGER,name TEXT,PRIMARY KEY(playlistId,generationId,episodeId));CREATE INDEX series_idx ON episodes(playlistId,generationId,seriesId);CREATE INDEX season_idx ON episodes(playlistId,generationId,seriesId,seasonNumber);')
random.seed(5);n=100000
ids=list(range(n));random.shuffle(ids)
c.executemany('INSERT INTO episodes VALUES(?,?,?,?,?,?)', [('p','old',f'{i:010x}',f's{i%1000}',i%10,'Episode') for i in ids]);c.commit()
for ordered in [True,False]:
 sql='DELETE FROM episodes WHERE rowid IN (SELECT rowid FROM episodes WHERE playlistId=? AND generationId=? '+('ORDER BY rowid ' if ordered else '')+'LIMIT ?)'
 print('ordered',ordered, list(c.execute('EXPLAIN QUERY PLAN '+sql,('p','old',500))))
 c.execute('SAVEPOINT test');start=time.perf_counter()
 for _ in range(40):c.execute(sql,('p','old',500))
 print('40 batches ms',round((time.perf_counter()-start)*1000));c.execute('ROLLBACK TO test');c.execute('RELEASE test')
