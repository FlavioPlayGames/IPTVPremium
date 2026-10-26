import sqlite3
c=sqlite3.connect(':memory:')
c.executescript('CREATE TABLE cached_episodes(playlistId TEXT,generationId TEXT,id TEXT,payload TEXT,PRIMARY KEY(playlistId,generationId,id)); CREATE INDEX episode_lookup ON cached_episodes(playlistId,generationId);')
c.executemany('INSERT INTO cached_episodes VALUES(?,?,?,?)', [('a','g1',str(i),'x'*300) for i in range(1200)]+[('b','g2',str(i),'x'*300) for i in range(500)])
c.commit();changes=c.total_changes
assert c.execute('SELECT COUNT(*) FROM cached_episodes WHERE playlistId=? AND generationId=?',('a','g1')).fetchone()[0]==1200
assert c.execute('SELECT COUNT(*) FROM cached_episodes WHERE playlistId=? AND generationId=?',('b','g2')).fetchone()[0]==500
q="SELECT s.tbl_name, s.type, s.name, SUM(d.pgsize) FROM dbstat AS d JOIN sqlite_master AS s ON s.name=d.name WHERE s.type IN ('table','index') GROUP BY s.tbl_name,s.type,s.name"
rows=list(c.execute(q));assert any(r[1]=='index' and r[3]>0 for r in rows);assert any(r[1]=='table' and r[3]>0 for r in rows)
assert c.total_changes==changes
print('PASS per-list generation counts isolated; table/index page bytes returned separately; diagnostics perform no writes (SQLite local, dbstat supported).')
