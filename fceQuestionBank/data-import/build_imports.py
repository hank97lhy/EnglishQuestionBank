"""Rebuild the two reviewed PDF imports; never connects to a database."""
import copy
import hashlib
import json
import re
from collections import Counter
from pathlib import Path

BASE = Path(__file__).resolve().parent
ROOT = BASE.parents[1]
FORMATS = ['multiple_choice_cloze', 'open_cloze', 'word_formation', 'key_word_transformation', 'reading_multiple_choice', 'gapped_text', 'multiple_matching']
RANGES = [(1,8),(9,16),(17,24),(25,30),(31,36),(37,42),(43,52)]
PAGES = [[2,3],[4],[5],[6,7],[8,9],[10,11],[12,13]]
QFIELDS = ['stem','analysis','type_note','strategy','pitfall','solve_steps','distractors','knowledge','logic_links']
TABLES = ['edition','book','exam_paper','exam_section','passage_paragraph','option_set','question_option','question','question_answer','question_evidence']
ANSWERS = {
 'standard': ['A','C','A','B','C','D','B','D','can/may','so','with','not/hardly/scarcely','and','have','where','if','unknown','reference','popularity','marriage','fashionable','illnesses','labourers','energetic','FEW programmes | were sold','INSTEAD of | taking/catching/getting','had/’d NEVER | broken','would | LOOK into/at','was/got postponed | BECAUSE it rained','to CARRY on | working','B','D','D','B','A','C','D','G','F','A','C','E','C','C','A','B','A','C','B','D','B','D'],
 'campus': ['C','B','D','A','D','C','D','B','If','out','What','made','of','is','who/that','up','impressive','popularity','relief','inexperienced','hopeless','surroundings','memorable','passionate','hadn’t / had not EXPECTED | to see','PUT us | up','cannot/can’t | be BOTHERED','keep | an EYE on','to DISCOURAGE Jo/her | from eating/having','to take | ACCOUNT of/into ACCOUNT','B','B','A','D','C','C','E','C','A','F','D','G','C','A','E','D','B','C','A','B','D','E']
}

def dump(p, v):
 p.write_text(json.dumps(v,ensure_ascii=False,indent=2)+'\n',encoding='utf-8')

def sha(p): return hashlib.sha256(p.read_bytes()).hexdigest()

def literal(v): return "'"+v.replace("'","''")+"'"

def jsql(v): return literal(json.dumps(v,ensure_ascii=False,separators=(',',':')))+'::jsonb'

def prepare(slug, edition, book_no):
 out=BASE/slug
 src=json.loads((out/'sources/exam-data.json').read_text(encoding='utf-8'))
 known_root={'title','subtitle','edition','origin','sections','expected_question_ids','exam_summary','features','audio_note','audio_delivery','section_order_note','generation_scope','generation_preferences','dictionary','legacy_dictionary'}
 known_part={'id','part','kind','title','paper_reference','expected_option_count','source_groups','paragraphs','questions','shared_options','quick_words','vocabulary','sentences','structure','inquiry','logic_steps','writing_bank'}
 known_question=set(QFIELDS)|{'id','options','answer','answer_source','answer_status','question_type','evidence'}
 assert set(src)<=known_root,('unknown root fields',set(src)-known_root)
 for section in src['sections']:
  assert set(section)<=known_part,('unknown section fields',set(section)-known_part)
  for para in section['paragraphs']: assert set(para)<={'id','role','text','translation'}
  for question in section['questions']:
   assert set(question)<=known_question,('unknown question fields',set(question)-known_question)
   for evidence in question.get('evidence',[]): assert set(evidence)=={'paragraph_id','quote'}
 d=copy.deepcopy(src)
 diffs=[]
 def change(obj,key,value,path,reason):
  old=obj.get(key)
  if old!=value:
   diffs.append(dict(path=path,website_value=old,adopted_value=value,reason=reason))
   obj[key]=value
 if edition=='standard':
  change(d['sections'][5],'expected_option_count',7,'P6.expected_option_count','PDF 10–11 页说明及 A–G 七个选项')
  captions={4:'Paul Williams interviews the famous pianist Alfred Brendel.',5:'Paul Hardy reports on a blind runner called Simon Wheatcroft who enjoys taking part in marathon and ultra-marathon races, running distances between 42 km and 160 km.',6:'Four graduates talk about their experiences.'}
  for idx,text in captions.items():
   d['sections'][idx]['paragraphs'].insert(1,dict(id=f'P{idx+1}-caption',role='caption',text=text,translation=''))
   diffs.append(dict(path=f'P{idx+1}.paragraphs.caption',website_value=None,adopted_value=text,reason=f'补入 PDF 第 {PAGES[idx][-1] if idx==6 else PAGES[idx][0]} 页原有副标题'))
  change(d['sections'][3]['paragraphs'][0],'text',d['sections'][3]['paragraphs'][0]['text'].replace('Complete the second sentence so that it has a similar meaning to the first sentence. Use the word given and write between two and five words.','For questions 25–30, complete the second sentence so that it has a similar meaning to the first sentence, using the word given. Do not change the word given. You must use between two and five words, including the word given.'),'P4.P4-p0.text','恢复 PDF 第 6 页完整要求（不含示例与答题卡操作说明）')
 else:
  for p in d['sections'][2]['paragraphs']:
   change(p,'text',re.sub(r' (\([A-Z]+\))','',p['text']),f"P3.{p['id']}.text",'提示词在 PDF 行末独立列；从正文移到题干末行')
  p=d['sections'][6]['paragraphs'][6]
  assert p['id']=='P7-C'
  change(p,'text',p['text'].replace("there's a clever twist. What I Lost", "there's a clever twist: What I Lost"),'P7.P7-C.text','PDF 第 13 页原文使用冒号')
  p=d['sections'][1]['paragraphs'][1]
  change(p,'text',p['text'].replace('(0) have you','(0) Have you'),'P2.P2-p1.text','例题 HAVE 填入句首，采用句首大写')
  p=d['sections'][5]['paragraphs'][4]
  change(p,'text',p['text'].replace('to try to improve','to improve'),'P6.P6-p4.text','PDF 第 10 页没有网站补入的 try to')
  p=d['sections'][5]['paragraphs'][7]
  change(p,'text',p['text'].replace('So, Anderson','So Andersson'),'P6.P6-p7.text','PDF 第 10 页后文为 So Andersson，无逗号；按扫描原卷修正')
  q=d['sections'][5]['questions'][5]
  for field in ['analysis','solve_steps','evidence','logic_links']:
   old=copy.deepcopy(q.get(field))
   def fix(value):
    if isinstance(value,str): return value.replace('So, Anderson','So Andersson').replace('So Anderson','So Andersson')
    if isinstance(value,list): return [fix(x) for x in value]
    if isinstance(value,dict): return {k:fix(v) for k,v in value.items()}
    return value
   change(q,field,fix(old),f'P6.Q42.{field}','原文人名与标点修正后同步更新引用及解析，避免保留旧引用')
 # Standardise gap notation; derive cloze stems from the reviewed original sentence,
 # retaining all neighbouring gaps rather than filling them with website answers.
 question_sources=[]
 paragraph_sources=[]
 sections=[]
 for idx,s in enumerate(d['sections']):
  code=s['id']; paragraphs=[]
  for pos,p in enumerate(s['paragraphs'],1):
   paragraphs.append(dict(source_key=p['id'],role=p.get('role','paragraph'),text=p['text'],translation=p.get('translation',''),sort_order=pos))
   region='title' if p.get('role')=='heading' else ('subtitle' if p.get('role')=='caption' else 'body; left column then right column for P5–P7')
   if code=='P7' and p['id']=='P7-h': region='title on PDF 13'
   paragraph_sources.append(dict(section=code,source_key=p['id'],pdf_pages=([13] if code=='P7' else [PAGES[idx][0]]),printed_pages=([19] if code=='P7' else [PAGES[idx][0]+6]),region=region,reading_method='model_visual',verified=True,website_path=f"sections[{idx}].paragraphs[id={p['id']}]",translation_source='website' if p.get('translation') else 'absent'))
  sets=[]; qs=[]
  shared=None
  if idx in [5,6]:
   shared=s['questions'][0]['options']
   assert all(q['options']==shared for q in s['questions'])
   if 'shared_options' in s: assert s['shared_options']==shared
   sets.append(dict(code='shared',options=[dict(label=k,content=v,sort_order=n) for n,(k,v) in enumerate(sorted(shared.items()),1)]))
  for pos,q in enumerate(s['questions'],1):
   n=int(q['id']); oldstem=q['stem']
   if idx in [0,1,2]:
    marker='{{'+str(n)+'}}'
    p=next(p for p in paragraphs if marker in p['text'])
    sentences=re.split(r'(?<=[.!?])\s+',p['text'])
    stem=next(t for t in sentences if marker in t)
    if idx==2:
     hint=(['KNOW','REFER','POPULAR','MARRY','FASHION','ILL','LABOUR','ENERGY'] if edition=='standard' else ['IMPRESS','POPULAR','RELIEVE','EXPERIENCE','HOPE','SURROUND','MEMORY','PASSION'])[n-17]
     stem+='\n'+hint
    change(q,'stem',stem,f'{code}.Q{n}.stem','PDF 正文原句投影；保留邻近空格，Part 3 提示词另起一行')
   elif idx==3:
    change(q,'stem',re.sub(r'_{2,}','{{'+str(n)+'}}',q['stem']),f'{code}.Q{n}.stem','PDF 空格统一为 {{题号}}；保留原句、关键词和改写句三行')
   elif idx==5:
    change(q,'stem','{{'+str(n)+'}}',f'{code}.Q{n}.stem','原卷仅在文章中给空格，无独立题干；用空格编号投影')
   change(q,'answer',ANSWERS[edition][n-1],f'{code}.Q{n}.answer','对应答案 PDF 第 1 页逐题视觉转录；保留大小写、斜杠及评分竖线')
   option_code=None
   if idx in [0,4]:
    option_code=f'question-{n}'
    sets.append(dict(code=option_code,options=[dict(label=k,content=v,sort_order=a) for a,(k,v) in enumerate(sorted(q['options'].items()),1)]))
   elif idx in [5,6]: option_code='shared'
   else: assert not q['options']
   target={f:copy.deepcopy(q.get(f, None if f=='knowledge' else ([] if f in ['solve_steps','logic_links'] else ({} if f=='distractors' else '')))) for f in QFIELDS}
   target.update(question_no=n,sort_order=pos,option_set_code=option_code,question_type_label=q['question_type'],answer=dict(raw_answer=q['answer'],display_text=q['answer'],accepted_answers=None,answer_status='official',answer_source=f"{'S01' if edition=='standard' else 'F03'}-T1.pdf; PDF page 1; printed page 120; {code}; question {n}"),evidence=copy.deepcopy(q.get('evidence',[])))
   qs.append(target)
   page=(6 if n<=27 else 7) if idx==3 else (3 if idx==0 else (9 if idx==4 else (12 if idx==6 else PAGES[idx][0])))
   question_sources.append(dict(section=code,question_no=n,pdf_page=page,printed_page=page+6,region=f'question {n}; options '+('PDF 11' if idx==5 else ('PDF 13 headings' if idx==6 else str(page))),reading_method='model_visual',verified=True,raw_answer_transcription=ANSWERS[edition][n-1],answer_pdf_page=1,answer_printed_page=120,answer_verified=True,website_answer=src['sections'][idx]['questions'][pos-1]['answer'],website_path=f'sections[{idx}].questions[{pos-1}]',analysis_source='website',analysis_reviewed_against_pdf_answer=True))
  sections.append(dict(code=code,part_no=idx+1,title=s['title'],question_format=FORMATS[idx],paper_reference=s.get('paper_reference',''),expected_option_count=s.get('expected_option_count'),sort_order=idx+1,source_groups=[{k:g[k] for k in ['title','paragraph_ids']} for g in s.get('source_groups',[])],paragraphs=paragraphs,option_sets=sets,questions=qs))
 name='标准版' if edition=='standard' else '校园版'
 paper_pdf=f'题目/FCE题目/{name}{book_no}/{"标" if edition=="standard" else "青"}{book_no}-T1.pdf'
 answer_pdf=f'题目/FCE题目/FCE-ANSWERS/{name}/{"S01" if edition=="standard" else "F03"}-T1.pdf'
 html=f'FCE网站最终版/{slug}/index.html'
 result=dict(catalog=dict(edition_code=edition,edition_name=name,edition_sort_order=1 if edition=='standard' else 2,book_no=book_no,book_title=f'{name} {book_no}',book_sort_order=book_no,test_no=1,slug=slug),paper=dict(title=d['title'],subtitle=d.get('subtitle',''),duration_minutes=75,status='published',edition_note=d.get('edition',''),source_metadata={**d.get('origin',{}),'paper_file':paper_pdf,'answer_file':answer_pdf,'pdf_pages':[2,13],'printed_pages':[8,19],'supplement_file':html}),sections=sections)
 provenance=dict(scope='Reading and Use of English only',sources=[dict(path=p,sha256=sha(ROOT/p)) for p in [paper_pdf,answer_pdf,html]],cover_pairing_verified=True,pages=[dict(pdf_page=n,printed_page=None if n==1 else n+6,reading_method='model_visual',verified=True) for n in range(1,14)],answer_pages=[dict(pdf_page=1,printed_page=120,reading_method='model_visual',verified=True,question_range=[1,52])],paragraphs=paragraph_sources,questions=question_sources,differences=diffs,transformations=['PDF scan line wraps merged within paragraphs; double columns read left then right; website source_keys retained as stable paragraph identifiers after visual correspondence review.','Straight/curly apostrophe glyph differences and typesetting hyphens retained using existing website transcription except official answer strings; printed line/page markers excluded.','Examples 0 filled using PDF supplied example answers; no question 0 record.','Cloze stems are projections of the exact original sentence, not separate printed questions; P6 stem is gap marker; P7 combines common question lead-in with each prompt.','Website paragraph translations and question analyses retained; missing translations remain empty.','Pending placeholders are treated conservatively as conflicts; no unapproved catalogue title overwrite.'],blockers=[])
 assert [int(x) for x in src['expected_question_ids']]==list(range(1,53))
 validate(result)
 dump(out/'paper.json',result); dump(out/'provenance.json',provenance)
 return result,provenance,src

def validate(p):
 allnos=[]
 for idx,s in enumerate(p['sections']):
  assert s['code']==f'P{idx+1}' and s['question_format']==FORMATS[idx]
  assert [q['question_no'] for q in s['questions']]==list(range(RANGES[idx][0],RANGES[idx][1]+1))
  pars={x['source_key']:x['text'] for x in s['paragraphs']}; assert len(pars)==len(s['paragraphs'])
  sets={x['code']:{o['label']:o['content'] for o in x['options']} for x in s['option_sets']}
  assert len(sets)==len(s['option_sets'])
  for group in s['source_groups']:
   positions=[next(i for i,p in enumerate(s['paragraphs']) if p['source_key']==k) for k in group['paragraph_ids']]
   assert positions==sorted(set(positions))
  for para in s['paragraphs']:
   assert all(RANGES[idx][0]<=int(x)<=RANGES[idx][1] for x in re.findall(r'\{\{(\d+)\}\}',para['text']))
  for q in s['questions']:
   allnos.append(q['question_no']); opts=sets.get(q['option_set_code'],{})
   assert isinstance(q['solve_steps'],list) and all(isinstance(x,str) for x in q['solve_steps'])
   assert isinstance(q['logic_links'],list) and isinstance(q['distractors'],dict)
   assert q['knowledge'] is None or isinstance(q['knowledge'],dict)
   assert set(q['distractors'])<=set(opts)
   if opts: assert q['answer']['raw_answer'] in opts
   assert q['answer']['accepted_answers'] is None
   for e in q['evidence']: assert e['quote'] in pars[e['paragraph_id']],(s['code'],q['question_no'],e)
   for link in q['logic_links']:
    for e in link['endpoints']:
     assert set(e)<= {'paragraph_id','option','quote'}
     assert ('paragraph_id' in e) != ('option' in e)
     text=pars[e['paragraph_id']] if 'paragraph_id' in e else opts[e['option']]
     if 'quote' in e: assert e['quote'] in text,(q['question_no'],e)
  assert (len(sets)==len(s['questions']) if idx in [0,4] else (len(sets)==1 if idx in [5,6] else len(sets)==0))
 assert allnos==list(range(1,53))

def rows(p):
 c=p['catalog']; rs=[]
 def add(t,key,data): rs.append(dict(table_name=t,row_key=key,data=data))
 add('edition',{'code':c['edition_code']},dict(code=c['edition_code'],name=c['edition_name'],sort_order=c['edition_sort_order']))
 add('book',{'edition_code':c['edition_code'],'book_no':c['book_no']},dict(edition_code=c['edition_code'],book_no=c['book_no'],title=c['book_title'],sort_order=c['book_sort_order']))
 add('exam_paper',{'slug':c['slug']},dict(edition_code=c['edition_code'],book_no=c['book_no'],test_no=c['test_no'],slug=c['slug'],**p['paper']))
 for s in p['sections']:
  sk={'section_code':s['code']}
  add('exam_section',{'code':s['code']},{k:v for k,v in s.items() if k not in ['paragraphs','option_sets','questions']})
  for para in s['paragraphs']: add('passage_paragraph',dict(**sk,source_key=para['source_key']),dict(**sk,**para))
  for os in s['option_sets']:
   add('option_set',dict(**sk,code=os['code']),dict(**sk,code=os['code']))
   for o in os['options']: add('question_option',dict(**sk,option_set_code=os['code'],label=o['label']),dict(**sk,option_set_code=os['code'],**o))
  for q in s['questions']:
   qk=dict(**sk,question_no=q['question_no'])
   add('question',qk,dict(**sk,**{k:v for k,v in q.items() if k not in ['answer','evidence']}))
   add('question_answer',qk,dict(**qk,**q['answer']))
   for pos,e in enumerate(q['evidence'],1): add('question_evidence',dict(**qk,sort_order=pos),dict(**qk,paragraph_source_key=e['paragraph_id'],quote=e['quote'],sort_order=pos))
 return sorted(rs,key=lambda r:TABLES.index(r['table_name']))

def projection(slug):
 remove=" - 'id' - 'created_at' - 'updated_at'"
 # Return all business columns with foreign keys represented by stable natural keys.
 specs=[
 ('edition',"JOIN book b ON b.edition_id=t.id JOIN exam_paper p ON p.book_id=b.id",'',"jsonb_build_object('code',t.code)"),
 ('book',"JOIN edition e ON e.id=t.edition_id JOIN exam_paper p ON p.book_id=t.id"," - 'edition_id' || jsonb_build_object('edition_code',e.code)","jsonb_build_object('edition_code',e.code,'book_no',t.book_no)"),
 ('exam_paper',"JOIN book b ON b.id=t.book_id JOIN edition e ON e.id=b.edition_id", " - 'book_id' || jsonb_build_object('edition_code',e.code,'book_no',b.book_no)","jsonb_build_object('slug',t.slug)"),
 ('exam_section',"JOIN exam_paper p ON p.id=t.paper_id"," - 'paper_id'","jsonb_build_object('code',t.code)"),
 ('passage_paragraph',"JOIN exam_section s ON s.id=t.section_id JOIN exam_paper p ON p.id=s.paper_id"," - 'section_id' || jsonb_build_object('section_code',s.code)","jsonb_build_object('section_code',s.code,'source_key',t.source_key)"),
 ('option_set',"JOIN exam_section s ON s.id=t.section_id JOIN exam_paper p ON p.id=s.paper_id"," - 'section_id' || jsonb_build_object('section_code',s.code)","jsonb_build_object('section_code',s.code,'code',t.code)"),
 ('question_option',"JOIN option_set os ON os.id=t.option_set_id JOIN exam_section s ON s.id=os.section_id JOIN exam_paper p ON p.id=s.paper_id"," - 'option_set_id' || jsonb_build_object('section_code',s.code,'option_set_code',os.code)","jsonb_build_object('section_code',s.code,'option_set_code',os.code,'label',t.label)"),
 ('question',"JOIN exam_section s ON s.id=t.section_id JOIN exam_paper p ON p.id=s.paper_id LEFT JOIN option_set os ON os.id=t.option_set_id"," - 'section_id' - 'option_set_id' || jsonb_build_object('section_code',s.code,'option_set_code',os.code)","jsonb_build_object('section_code',s.code,'question_no',t.question_no)"),
 ('question_answer',"JOIN question q ON q.id=t.question_id JOIN exam_section s ON s.id=q.section_id JOIN exam_paper p ON p.id=s.paper_id"," - 'question_id' || jsonb_build_object('section_code',s.code,'question_no',q.question_no)","jsonb_build_object('section_code',s.code,'question_no',q.question_no)"),
 ('question_evidence',"JOIN question q ON q.id=t.question_id JOIN passage_paragraph pp ON pp.id=t.paragraph_id JOIN exam_section s ON s.id=t.section_id JOIN exam_paper p ON p.id=s.paper_id"," - 'section_id' - 'question_id' - 'paragraph_id' || jsonb_build_object('section_code',s.code,'question_no',q.question_no,'paragraph_source_key',pp.source_key)","jsonb_build_object('section_code',s.code,'question_no',q.question_no,'sort_order',t.sort_order)")]
 return '\nUNION ALL\n'.join(f"SELECT '{t}'::text AS table_name, {key} AS row_key, (to_jsonb(t){remove}{extra}) AS data FROM public.{t} t {joins} WHERE {'t' if t=='exam_paper' else 'p'}.slug={literal(slug)}" for t,joins,extra,key in specs)

ENSURE = r"""
CREATE FUNCTION pg_temp.ensure_import_row(tab text, k jsonb, wanted jsonb) RETURNS bigint
LANGUAGE plpgsql AS $fn$
DECLARE found_id bigint; n integer; existing jsonb; cols text; fields text;
BEGIN
  EXECUTE format('SELECT count(*), min(id) FROM public.%I t WHERE to_jsonb(t) @> $1',tab) INTO n,found_id USING k;
  IF n > 1 THEN RAISE EXCEPTION 'Ambiguous natural key: % %',tab,k; END IF;
  IF n = 1 THEN
    EXECUTE format('SELECT to_jsonb(t) FROM public.%I t WHERE id=$1',tab) INTO existing USING found_id;
    SELECT string_agg(key, ', ' ORDER BY key) INTO fields FROM jsonb_each(wanted) WHERE existing->key IS DISTINCT FROM value;
    IF fields IS NOT NULL THEN RAISE EXCEPTION 'Import conflict: % key %, fields %',tab,k,fields; END IF;
    RETURN found_id;
  END IF;
  SELECT string_agg(format('%I',key), ', ' ORDER BY key),string_agg(format('r.%I',key), ', ' ORDER BY key) INTO cols,fields FROM jsonb_each(wanted);
  EXECUTE format('INSERT INTO public.%I (%s) SELECT %s FROM jsonb_populate_record(NULL::public.%I,$1) r RETURNING id',tab,cols,fields,tab) INTO found_id USING wanted;
  RETURN found_id;
END
$fn$;
"""

def generate_sql(p, rs):
 slug=p['catalog']['slug']; out=BASE/slug
 expected=jsql(rs); actual=projection(slug)
 diff="SELECT 'missing_or_changed' AS kind,* FROM (SELECT * FROM expected EXCEPT SELECT * FROM actual) a UNION ALL SELECT 'extra_or_changed' AS kind,* FROM (SELECT * FROM actual EXCEPT SELECT * FROM expected) b"
 cte=f"WITH expected AS (SELECT table_name,row_key,data FROM jsonb_to_recordset({expected}) AS x(table_name text,row_key jsonb,data jsonb)), actual AS ({actual})\n"
 verify="-- Read only. No rows from the difference query means an exact match of every business row.\nBEGIN ISOLATION LEVEL REPEATABLE READ READ ONLY;\nSET LOCAL standard_conforming_strings=on;\nSET LOCAL search_path=public,pg_catalog;\n"+cte+"SELECT * FROM ("+diff+") differences ORDER BY table_name,row_key::text;\n"+f"SELECT table_name,count(*) AS row_count FROM ({actual}) a GROUP BY table_name ORDER BY table_name;\nCOMMIT;\n"
 (out/'verify.sql').write_text(verify,encoding='utf-8')
 # The view is temporary and contains only a natural-key projection, never source blobs in business tables.
 sql=f"-- Requires migrations 001–010; PostgreSQL >=12; schema public.\n-- {slug}: published; blockers: none. Reviewed PDF originals and 52 answers.\n-- Locks all ten business tables for the whole paper transaction; waits for concurrent writes.\n-- Same data: no updates. Partial existing paper or content conflict: exception and rollback.\n-- Pending placeholders are conservatively rejected. Use psql -v ON_ERROR_STOP=1.\nBEGIN;\nSET LOCAL standard_conforming_strings=on;\nSET LOCAL search_path=public,pg_catalog;\nLOCK TABLE "+', '.join(TABLES)+" IN SHARE ROW EXCLUSIVE MODE;\nCREATE TEMP TABLE import_expected (ordinal bigint GENERATED ALWAYS AS IDENTITY,table_name text,row_key jsonb,data jsonb) ON COMMIT DROP;\nINSERT INTO import_expected(table_name,row_key,data) SELECT table_name,row_key,data FROM jsonb_to_recordset("+expected+") AS x(table_name text,row_key jsonb,data jsonb);\nCREATE TEMP VIEW import_actual AS "+actual+";\n"
 sql+=r"""
CREATE FUNCTION pg_temp.check_import_rows(allow_draft boolean) RETURNS void LANGUAGE plpgsql AS $fn$
DECLARE delta record;
BEGIN
 FOR delta IN
   WITH expected AS (SELECT table_name,row_key,data FROM import_expected),
   actual AS (SELECT table_name,row_key,CASE WHEN allow_draft AND table_name='exam_paper' AND data->>'status'='draft' THEN jsonb_set(data,'{status}','"published"'::jsonb) ELSE data END AS data FROM import_actual),
   diffs AS (SELECT 'missing_or_changed' AS kind,* FROM (SELECT * FROM expected EXCEPT SELECT * FROM actual) a UNION ALL SELECT 'extra_or_changed' AS kind,* FROM (SELECT * FROM actual EXCEPT SELECT * FROM expected) b)
   SELECT * FROM diffs LIMIT 1
 LOOP
   RAISE EXCEPTION 'Whole-paper conflict: % table % key %, data %',delta.kind,delta.table_name,delta.row_key,delta.data;
 END LOOP;
END
$fn$;
"""
 sql+=f"DO $precheck$ BEGIN IF EXISTS(SELECT 1 FROM exam_paper WHERE slug={literal(slug)}) THEN PERFORM pg_temp.check_import_rows(true); END IF; END $precheck$;\n"+ENSURE
 sql+=r"""
DO $import$
DECLARE r record; w jsonb; k jsonb; eid bigint; bid bigint; pid bigint; sid bigint; oid bigint; qid bigint; ppid bigint; obtained bigint;
BEGIN
 FOR r IN SELECT * FROM import_expected ORDER BY ordinal LOOP
  w:=r.data; k:=r.row_key;
  IF r.table_name IN ('book','exam_paper') THEN
    SELECT id INTO STRICT eid FROM edition WHERE code=w->>'edition_code';
    w:=w-'edition_code';
    IF r.table_name='book' THEN
      w:=w || jsonb_build_object('edition_id',eid); k:=k-'edition_code' || jsonb_build_object('edition_id',eid);
    ELSE
      SELECT id INTO STRICT bid FROM book WHERE edition_id=eid AND book_no=(w->>'book_no')::integer;
      w:=w-'book_no' || jsonb_build_object('book_id',bid);
      -- New papers begin as draft; only the final whole-paper verification permits publication.
      w:=jsonb_set(w,'{status}','"draft"'::jsonb);
    END IF;
  END IF;
  IF r.table_name='exam_section' THEN
    SELECT id INTO STRICT pid FROM exam_paper WHERE slug=(SELECT data->>'slug' FROM import_expected WHERE table_name='exam_paper');
    w:=w || jsonb_build_object('paper_id',pid); k:=k || jsonb_build_object('paper_id',pid);
  END IF;
  IF r.table_name IN ('passage_paragraph','option_set','question_option','question','question_answer','question_evidence') THEN
    SELECT s.id INTO STRICT sid FROM exam_section s JOIN exam_paper p ON p.id=s.paper_id WHERE p.slug=(SELECT data->>'slug' FROM import_expected WHERE table_name='exam_paper') AND s.code=w->>'section_code';
    w:=w-'section_code'; k:=k-'section_code';
    IF r.table_name IN ('passage_paragraph','option_set','question','question_evidence') THEN w:=w || jsonb_build_object('section_id',sid); END IF;
    IF r.table_name IN ('passage_paragraph','option_set','question') THEN k:=k || jsonb_build_object('section_id',sid); END IF;
  END IF;
  IF r.table_name IN ('question_option','question') THEN
    oid:=NULL;
    IF w->>'option_set_code' IS NOT NULL THEN SELECT id INTO STRICT oid FROM option_set WHERE section_id=sid AND code=w->>'option_set_code'; END IF;
    w:=w-'option_set_code' || jsonb_build_object('option_set_id',oid);
    IF r.table_name='question_option' THEN k:=k-'option_set_code' || jsonb_build_object('option_set_id',oid); END IF;
  END IF;
  IF r.table_name IN ('question_answer','question_evidence') THEN
    SELECT id INTO STRICT qid FROM question WHERE section_id=sid AND question_no=(w->>'question_no')::integer;
    w:=w-'question_no' || jsonb_build_object('question_id',qid); k:=k-'question_no' || jsonb_build_object('question_id',qid);
  END IF;
  IF r.table_name='question_evidence' THEN
    SELECT id INTO STRICT ppid FROM passage_paragraph WHERE section_id=sid AND source_key=w->>'paragraph_source_key';
    w:=w-'paragraph_source_key' || jsonb_build_object('paragraph_id',ppid);
  END IF;
  IF r.table_name='exam_paper' AND EXISTS(SELECT 1 FROM exam_paper WHERE slug=w->>'slug') THEN
    SELECT to_jsonb(status) INTO STRICT w['status'] FROM exam_paper WHERE slug=w->>'slug';
  END IF;
  obtained:=pg_temp.ensure_import_row(r.table_name,k,w);
 END LOOP;
 PERFORM pg_temp.check_import_rows(true);
END
$import$;
""".replace("SELECT to_jsonb(status) INTO STRICT w['status'] FROM exam_paper WHERE slug=w->>'slug';", "w:=jsonb_set(w,'{status}',(SELECT to_jsonb(status) FROM exam_paper WHERE slug=w->>'slug'));")
 sql+=integrity_sql(slug)
 sql+=f"UPDATE exam_paper SET status='published',updated_at=CURRENT_TIMESTAMP WHERE slug={literal(slug)} AND status='draft';\nSELECT pg_temp.check_import_rows(false);\nDROP VIEW import_actual;\nDROP FUNCTION pg_temp.check_import_rows(boolean);\nDROP FUNCTION pg_temp.ensure_import_row(text,jsonb,jsonb);\nCOMMIT;\n"
 (out/'insert.sql').write_text(sql,encoding='utf-8')
 dump(out/'expected-rows.json',rs)

def integrity_sql(slug):
 return r"""
DO $validate$
DECLARE pid bigint;
BEGIN
 SELECT id INTO STRICT pid FROM exam_paper WHERE slug=SLUG;
 IF (SELECT count(*) FROM exam_section WHERE paper_id=pid)<>7 OR
    (SELECT array_agg(q.question_no ORDER BY q.question_no) FROM question q JOIN exam_section s ON s.id=q.section_id WHERE s.paper_id=pid) IS DISTINCT FROM ARRAY(SELECT generate_series(1,52)) THEN RAISE EXCEPTION 'Invalid Parts or question sequence'; END IF;
 IF EXISTS(SELECT 1 FROM question q JOIN exam_section s ON s.id=q.section_id LEFT JOIN question_answer a ON a.question_id=q.id WHERE s.paper_id=pid AND a.id IS NULL) THEN RAISE EXCEPTION 'Missing answer'; END IF;
 IF EXISTS(SELECT 1 FROM question_evidence e JOIN passage_paragraph pp ON pp.id=e.paragraph_id JOIN exam_section s ON s.id=e.section_id WHERE s.paper_id=pid AND strpos(pp.text,e.quote)=0) THEN RAISE EXCEPTION 'Evidence literal mismatch'; END IF;
 IF EXISTS(SELECT 1 FROM exam_section s CROSS JOIN LATERAL jsonb_array_elements(s.source_groups) g CROSS JOIN LATERAL jsonb_array_elements_text(g->'paragraph_ids') ref WHERE s.paper_id=pid AND NOT EXISTS(SELECT 1 FROM passage_paragraph pp WHERE pp.section_id=s.id AND pp.source_key=ref.value)) THEN RAISE EXCEPTION 'Unresolved source group'; END IF;
 IF EXISTS(SELECT 1 FROM question q JOIN exam_section s ON s.id=q.section_id CROSS JOIN LATERAL jsonb_array_elements(q.logic_links) l CROSS JOIN LATERAL jsonb_array_elements(l->'endpoints') ep WHERE s.paper_id=pid AND
   ((ep ? 'paragraph_id' AND NOT EXISTS(SELECT 1 FROM passage_paragraph pp WHERE pp.section_id=s.id AND pp.source_key=ep->>'paragraph_id' AND (NOT (ep ? 'quote') OR strpos(pp.text,ep->>'quote')>0))) OR
    (ep ? 'option' AND NOT EXISTS(SELECT 1 FROM question_option o WHERE o.option_set_id=q.option_set_id AND o.label=ep->>'option' AND (NOT (ep ? 'quote') OR strpos(o.content,ep->>'quote')>0))))) THEN RAISE EXCEPTION 'Unresolved logic endpoint or literal mismatch'; END IF;
 IF EXISTS(SELECT 1 FROM question q JOIN exam_section s ON s.id=q.section_id WHERE s.paper_id=pid AND ((s.part_no IN (2,3,4) AND q.option_set_id IS NOT NULL) OR (s.part_no IN (1,5,6,7) AND q.option_set_id IS NULL))) THEN RAISE EXCEPTION 'Invalid option ownership'; END IF;
END
$validate$;
""".replace('SLUG',literal(slug))

def report(p,prov,src,rs):
 slug=p['catalog']['slug']; out=BASE/slug; counts=Counter(r['table_name'] for r in rs)
 omitted_root=sorted(set(src)-{'title','subtitle','edition','origin','sections','expected_question_ids','exam_summary'})
 omitted_part=sorted(set().union(*(set(s) for s in src['sections']))-{'id','part','kind','title','paper_reference','expected_option_count','source_groups','paragraphs','questions','shared_options'})
 unknown_root=set(src)-set(omitted_root)-{'title','subtitle','edition','origin','sections','expected_question_ids','exam_summary'}
 assert not unknown_root
 txt=f'# {slug} 导入校验报告\n\n状态：published（脚本目标状态）；发布阻塞：无。仅生成文件，未连接数据库。\n\n'
 txt+='## 来源和核验\n\n'+''.join(f"- `{s['path']}`；SHA-256 `{s['sha256']}`\n" for s in prov['sources'])
 txt+='\n逐页视觉读取原卷 PDF 第 1–13 页，封面与 Test 1 配对；阅读与语用为 PDF 2–13 / 印刷 8–19。答案 PDF 第 1 页 / 印刷 120，逐题转录并核对 1–52。时长 75 分钟。未读取其他试卷，不导入 Writing、Listening、Speaking。\n\n'
 txt+='## 数量\n\n| 表 | 行数 |\n|---|---:|\n'+''.join(f'| {t} | {counts[t]} |\n' for t in TABLES)
 translated=sum(bool(x['translation']) for s in p['sections'] for x in s['paragraphs'])
 txt+=f'\n已保留网站译文 {translated} 段，题目解析 52 题。'
 if not translated: txt+='标准网站原文段落 translation 全为空，因此仍为空；未自动补写翻译。'
 txt+='新增 PDF 副标题无网站译文时为空。knowledge 缺失时为 SQL NULL；accepted_answers 全部 SQL NULL。\n\n'
 txt+='## 差异与变换\n\nPDF 决定原文/选项/题干；答案 PDF 决定答案。网站只补充已有翻译与题目级解析。所有差异见 provenance.json 的 differences；以下列出答案与内容差异（统一空格投影详见该文件）。\n\n'
 for x in prov['differences']:
  if x['path'].endswith('.stem'): continue
  txt+=f"- `{x['path']}`：网站 `{x['website_value']}` → `{x['adopted_value']}`；{x['reason']}。\n"
 txt+='\n'+''.join('- '+x+'\n' for x in prov['transformations'])
 if slug.startswith('standard'): txt+='- Q30 保留原卷原句 Jack / 目标句 John 的不一致，不自行改名。\n'
 else: txt+='- P6 保留原卷 Stellan Andersson / 后文 Andersson 的原写法；网站后文 Anderson 已更正（见补充核验记录）。\n'
 txt+='\n## 白名单与排除\n\n未知字段：无。根级排除：'+', '.join(omitted_root)+'。Part 级排除：'+', '.join(omitted_part)+'。expected_question_ids、exam_summary 仅校验；part/kind 用于题型映射；原网站 answer_source/answer_status 替换为已定位答案册出处，原值保留于 sources/exam-data.json。未把原 JSON 或暂缓模块塞入业务 JSONB。\n\n'
 txt+='## 验证与执行边界\n\n静态检查通过：7 Part / 52 题与分配、选项组共享逐字一致、提示词、答案标签/变体、段落和题目顺序、JSON 类型、source_groups 与 logic_links 引用、所有 evidence.quote 和带 quote 的逻辑端点逐字匹配；每一业务列与 001–010 DDL 对齐，SQL 内嵌完整期望数据转义往返一致。使用本机 Flyway 12.11.0 PostgreSQL 脚本解析器检查语句边界：insert.sql 18 条、verify.sql 6 条；此解析器检查分句/引号，不等同服务器 SQL/PLpgSQL 编译。insert.sql 采用 identity 默认 ID、自然键定位、整卷事务及十表同序锁；已有整卷在插入前做双向 EXCEPT 全行集比较，冲突报错回滚；同数据重跑不写 ID/时间戳。目录仅创建实际两卷所需记录，不初始化 48 个位置。pending 占位卷保守拒绝，需明确目录默认值后才能单独制定提升逻辑。\n\nverify.sql 为只读事务，双向比较完整十表业务行集并输出统计；差异查询返回零行才代表内容完全相同。未在 PostgreSQL 执行，未实测首次导入、重跑和冲突回滚，不能称导入成功。默认 schema public，需先应用 001–010；可用 `psql -v ON_ERROR_STOP=1 -f insert.sql`，再执行 verify.sql。\n'
 (out/'validation-report.md').write_text(txt,encoding='utf-8')

if __name__=='__main__':
 for slug,edition,book in [('standard-1-test-1','standard',1),('campus-3-test-1','campus',3)]:
  p,prov,src=prepare(slug,edition,book); rs=rows(p); generate_sql(p,rs); report(p,prov,src,rs)
  print(slug,dict(Counter(r['table_name'] for r in rs)))
