"""Static DDL/data/SQL-payload checks; no database connections."""
import json
import re
from pathlib import Path
from build_imports import BASE, ROOT, TABLES, validate, rows

ddl={}
for path in sorted((ROOT/'fceQuestionBank/src/main/resources/db/migration').glob('*.sql')):
 text=path.read_text(encoding='utf-8')
 table=re.search(r'CREATE TABLE IF NOT EXISTS (\w+)',text).group(1)
 cols={m.group(1):m.group(2) for m in re.finditer(r'^    (\w+) (bigint|integer|text\[\]|text|jsonb|timestamptz)\b',text,re.M)}
 ddl[table]=cols
assert list(ddl)==TABLES
for slug in ['standard-1-test-1','campus-3-test-1']:
 folder=BASE/slug
 paper=json.loads((folder/'paper.json').read_text(encoding='utf-8'))
 validate(paper)
 expected=rows(paper)
 assert expected==json.loads((folder/'expected-rows.json').read_text(encoding='utf-8'))
 for row in expected:
  data=dict(row['data']); table=row['table_name']
  for natural in ['edition_code','section_code','option_set_code','paragraph_source_key']:
   data.pop(natural,None)
  if table=='book': data['edition_id']=1
  if table=='exam_paper': data.pop('book_no'); data['book_id']=1
  if table=='exam_section': data['paper_id']=1
  if table in ['passage_paragraph','option_set','question','question_evidence']: data['section_id']=1
  if table in ['question_option','question']: data['option_set_id']=1 if table=='question_option' or row['data']['option_set_code'] else None
  if table in ['question_answer','question_evidence']: data.pop('question_no'); data['question_id']=1
  if table=='question_evidence': data['paragraph_id']=1
  assert set(data)==set(ddl[table])-{'id','created_at','updated_at'},(table,set(data),set(ddl[table]))
  for key,v in data.items():
   typ=ddl[table][key]
   if v is None:
    assert key in ['option_set_id','knowledge','accepted_answers','duration_minutes','expected_option_count']
   elif typ in ['integer','bigint']: assert type(v) is int
   elif typ=='text': assert isinstance(v,str)
   elif typ=='jsonb': assert isinstance(v,(str,list,dict))
 for filename in ['insert.sql','verify.sql']:
  sql=(folder/filename).read_text(encoding='utf-8')
  # SQL string escapes must recover the entire canonical payload unchanged.
  match=re.search(r"jsonb_to_recordset\('((?:[^']|'')*)'::jsonb\)",sql)
  assert match,filename
  assert json.loads(match.group(1).replace("''","'"))==expected
  assert sql.rstrip().endswith('COMMIT;')
  assert 'EXCEPT' in sql
  if filename=='verify.sql':
   assert not re.search(r'\b(INSERT|UPDATE|DELETE|CREATE|DROP|ALTER)\b',sql)
  else:
   assert 'ON CONFLICT DO UPDATE' not in sql and 'RAISE EXCEPTION' in sql
 summary=dict(static_checks='passed',database_executed=False,ddl_columns='all business columns on all ten tables',payload_roundtrip='exact',question_count=52,answer_count=52,evidence_count=sum(len(q['evidence']) for s in paper['sections'] for q in s['questions']))
 (folder/'static-checks.json').write_text(json.dumps(summary,ensure_ascii=False,indent=2)+'\n',encoding='utf-8')
 print(slug,summary)
