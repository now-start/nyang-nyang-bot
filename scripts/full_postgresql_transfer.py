"""One-time full transfer. Run only in the isolated, protected migration container.

No row values or credentials are logged. Export includes every source table and column.
Import refuses a populated target and commits only after full canonical row comparison.
"""
import argparse
import datetime as dt
import decimal
import hashlib
import json
import os
import pathlib
import re
import sys
import urllib.parse
import urllib.request

sys.path.insert(0, '/tmp/migration-libs')
import pymysql
import psycopg
from psycopg import sql


def config():
    result = {}
    with urllib.request.urlopen('http://config:8888/nyang-nyang-bot/default', timeout=10) as response:
        for source in reversed(json.load(response)['propertySources']):
            result.update(source['source'])
    return result


def encode(value):
    if isinstance(value, dt.datetime):
        return {'instant': value.replace(tzinfo=dt.timezone.utc).isoformat(timespec='microseconds') if value.tzinfo is None else value.astimezone(dt.timezone.utc).isoformat(timespec='microseconds')}
    if isinstance(value, decimal.Decimal):
        return {'decimal': str(value)}
    if isinstance(value, bytes):
        return {'bytes': value.hex()}
    if isinstance(value, bool):
        return int(value)
    return value


def decode(value):
    if isinstance(value, dict):
        if 'instant' in value: return dt.datetime.fromisoformat(value['instant'])
        if 'decimal' in value: return decimal.Decimal(value['decimal'])
        if 'bytes' in value: return bytes.fromhex(value['bytes'])
        raise ValueError('unknown value type')
    return value


def digest(value):
    return hashlib.sha256(json.dumps(value, ensure_ascii=True, sort_keys=True, separators=(',', ':')).encode()).hexdigest()


def row_digest(rows):
    return digest(sorted(json.dumps(row, ensure_ascii=True, sort_keys=True, separators=(',', ':')) for row in rows))


def source_snapshot():
    p = config()
    url = urllib.parse.urlsplit(p['spring.datasource.url'].removeprefix('jdbc:'))
    if url.scheme != 'mariadb' or url.path != '/nyang_nyang_bot':
        raise ValueError('unexpected source')
    conn = pymysql.connect(host=url.hostname, port=url.port or 3306, user=p['spring.datasource.username'], password=p['spring.datasource.password'], database='nyang_nyang_bot', charset='utf8mb4', connect_timeout=10, read_timeout=60)
    result = {'format': 1, 'database': 'nyang_nyang_bot', 'tables': {}}
    try:
        with conn.cursor() as c:
            c.execute("SET SESSION time_zone='+00:00'")
            c.execute('SET TRANSACTION ISOLATION LEVEL REPEATABLE READ')
            c.execute('SET TRANSACTION READ ONLY')
            c.execute('START TRANSACTION WITH CONSISTENT SNAPSHOT')
            c.execute('SELECT TABLE_NAME, TABLE_TYPE, ENGINE, AUTO_INCREMENT FROM information_schema.TABLES WHERE TABLE_SCHEMA=DATABASE() ORDER BY TABLE_NAME')
            tables = c.fetchall()
            if len(tables) != 17 or any(kind != 'BASE TABLE' or engine != 'InnoDB' for _, kind, engine, _ in tables):
                raise ValueError('unexpected table inventory')
            for name, _, _, next_id in tables:
                q = '`' + name.replace('`', '``') + '`'
                c.execute('SELECT COLUMN_NAME,COLUMN_TYPE,EXTRA FROM information_schema.COLUMNS WHERE TABLE_SCHEMA=DATABASE() AND TABLE_NAME=%s ORDER BY ORDINAL_POSITION', (name,))
                columns = [{'name': a, 'type': b, 'extra': e} for a,b,e in c.fetchall()]
                c.execute('SHOW CREATE TABLE ' + q)
                ddl = c.fetchone()[1]
                c.execute('SELECT * FROM ' + q)
                rows = [[encode(value) for value in row] for row in c.fetchall()]
                result['tables'][name] = {'columns': columns, 'rows': rows, 'sha256': row_digest(rows), 'next_id': next_id, 'ddl': ddl}
            conn.rollback()
    finally:
        conn.close()
    result['sha256'] = digest(result)
    return result


def load(path):
    value = json.loads(pathlib.Path(path).read_text())
    checksum = value.pop('sha256')
    if digest(value) != checksum: raise ValueError('snapshot checksum mismatch')
    value['sha256'] = checksum
    for table in value['tables'].values():
        if row_digest(table['rows']) != table['sha256']: raise ValueError('table checksum mismatch')
    return value


def target(database):
    if database not in ('nyang_nyang_bot', 'nyang_migration_rehearsal'):
        raise ValueError('unexpected destination')
    p = config()
    return psycopg.connect(host='postgres_haproxy', port=5432, dbname=database, user=p['spring.datasource.username'], password=p['spring.datasource.password'], sslmode='require', connect_timeout=3, target_session_attrs='read-write', options='-c timezone=UTC')


def compare(c, snapshot):
    for name, table in snapshot['tables'].items():
        columns = [column['name'] for column in table['columns']]
        c.execute(sql.SQL('SELECT {} FROM {}').format(sql.SQL(',').join(map(sql.Identifier, columns)), sql.Identifier(name)))
        rows = [[encode(value) for value in row] for row in c.fetchall()]
        if len(rows) != len(table['rows']) or row_digest(rows) != table['sha256']:
            raise ValueError('row mismatch: ' + name)
        for index, column in enumerate(table['columns']):
            if 'auto_increment' in column['extra']:
                expected = max(table['next_id'] or 1, max((row[index] for row in table['rows']), default=0) + 1)
                c.execute('SELECT pg_get_serial_sequence(%s,%s)', (name,column['name']))
                sequence = c.fetchone()[0]
                c.execute(sql.SQL('SELECT last_value,is_called FROM {}').format(sql.Identifier(*sequence.split('.'))))
                if c.fetchone() != (expected, False): raise ValueError('sequence mismatch: ' + name)
        print(json.dumps({'table': name, 'rows': len(rows), 'sha256': table['sha256'], 'equal': True}), flush=True)


def import_all(args):
    snapshot = load(args.path)
    schema = pathlib.Path(args.schema).read_text(encoding='utf-8')
    names = re.findall(r'CREATE TABLE (\w+)', schema)
    if set(names) | {'flyway_schema_history'} != set(snapshot['tables']):
        raise ValueError('source/schema inventory mismatch')
    history = snapshot['tables']['flyway_schema_history']
    hrows = [dict(zip((c['name'] for c in history['columns']), row)) for row in history['rows']]
    if len(hrows) != 1 or hrows[0]['version'] != '1' or hrows[0]['type'] != 'BASELINE' or hrows[0]['success'] != 1:
        raise ValueError('unexpected Flyway baseline')
    with target(args.database) as conn:
        with conn.cursor() as c:
            c.execute("SELECT pg_advisory_xact_lock(1314475591, 1)")
            c.execute("SELECT tablename FROM pg_tables WHERE schemaname='public'")
            if c.fetchall(): raise ValueError('target must have no application tables')
            c.execute(schema)
            # Fresh-install seeds are replaced by the exact source command rows.
            c.execute('DELETE FROM command')
            c.execute('''CREATE TABLE flyway_schema_history (
                installed_rank INTEGER PRIMARY KEY, version VARCHAR(50), description VARCHAR(200) NOT NULL,
                type VARCHAR(20) NOT NULL, script VARCHAR(1000) NOT NULL, checksum INTEGER,
                installed_by VARCHAR(100) NOT NULL, installed_on TIMESTAMPTZ(6) NOT NULL DEFAULT now(),
                execution_time INTEGER NOT NULL, success BOOLEAN NOT NULL
            )''')
            c.execute('CREATE INDEX flyway_schema_history_s_idx ON flyway_schema_history(success)')
            for name in names + ['flyway_schema_history']:
                table = snapshot['tables'][name]
                c.execute('SELECT column_name,data_type,is_generated FROM information_schema.columns WHERE table_schema=current_schema() AND table_name=%s ORDER BY ordinal_position', (name,))
                target_columns = {n:(t,g) for n,t,g in c.fetchall()}
                if set(target_columns) != {column['name'] for column in table['columns']}:
                    raise ValueError('column mismatch: ' + name)
                selected = [(i,col['name']) for i,col in enumerate(table['columns']) if target_columns[col['name']][1] == 'NEVER']
                query = sql.SQL('INSERT INTO {} ({}) VALUES ({})').format(sql.Identifier(name), sql.SQL(',').join(sql.Identifier(n) for _,n in selected), sql.SQL(',').join(sql.Placeholder() for _ in selected))
                values = []
                for row in table['rows']:
                    value = [decode(row[i]) for i,_ in selected]
                    for j,(_,column) in enumerate(selected):
                        if target_columns[column][0] == 'boolean' and value[j] is not None:
                            if value[j] not in (0,1): raise ValueError('invalid boolean')
                            value[j] = bool(value[j])
                    values.append(value)
                if values: c.executemany(query, values)
                for col in table['columns']:
                    if 'auto_increment' in col['extra']:
                        c.execute(sql.SQL('SELECT COALESCE(MAX({}),0)+1 FROM {}').format(sql.Identifier(col['name']),sql.Identifier(name)))
                        next_id = max(table['next_id'] or 1, c.fetchone()[0])
                        c.execute('SELECT pg_get_serial_sequence(%s,%s)', (name,col['name']))
                        sequence = c.fetchone()[0]
                        c.execute(sql.SQL('ALTER SEQUENCE {} RESTART WITH {}').format(sql.Identifier(*sequence.split('.')),sql.Literal(next_id)))
            compare(c,snapshot)
            c.execute("SELECT count(*) FROM pg_constraint WHERE connamespace='public'::regnamespace AND NOT convalidated")
            if c.fetchone()[0]: raise ValueError('unvalidated constraints')
    print('IMPORT_COMMITTED', flush=True)


def main():
    parser=argparse.ArgumentParser()
    parser.add_argument('action',choices=['export','import','verify','verify-source'])
    parser.add_argument('path')
    parser.add_argument('--schema')
    parser.add_argument('--database',default='nyang_migration_rehearsal')
    args=parser.parse_args()
    if args.action == 'export':
        value=source_snapshot()
        with os.fdopen(os.open(args.path,os.O_WRONLY|os.O_CREAT|os.O_EXCL,0o600),'w') as f: json.dump(value,f,ensure_ascii=True)
        print(json.dumps({'exported_tables':len(value['tables']),'rows':sum(len(t['rows']) for t in value['tables'].values()),'sha256':value['sha256']}))
    elif args.action == 'import': import_all(args)
    elif args.action == 'verify':
        with target(args.database) as conn:
            with conn.cursor() as c: compare(c,load(args.path))
    else:
        old=load(args.path);new=source_snapshot()
        if set(old['tables']) != set(new['tables']): raise ValueError('source inventory changed')
        for name,table in old['tables'].items():
            if any(table[key] != new['tables'][name][key] for key in ('sha256', 'next_id', 'columns', 'ddl')):
                raise ValueError('source changed: '+name)
        print('SOURCE_UNCHANGED')

if __name__ == '__main__':
    try: main()
    except Exception as error:
        # Driver exceptions may contain row values; expose only the exception type.
        print(json.dumps({'error_type':type(error).__name__}),file=sys.stderr)
        sys.exit(1)
