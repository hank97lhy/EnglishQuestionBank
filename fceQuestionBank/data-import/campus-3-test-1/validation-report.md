# campus-3-test-1 导入校验报告

状态：published（脚本目标状态）；发布阻塞：无。仅生成文件，未连接数据库。

## 来源和核验

- `题目/FCE题目/校园版3/青3-T1.pdf`；SHA-256 `48e24619858c97801809425f94adb73d03649c13795ec84f7331c3a73f812368`
- `题目/FCE题目/FCE-ANSWERS/校园版/F03-T1.pdf`；SHA-256 `fc3492b3ea90d20bf0af6c77cdf2610b81517f9d9015b50c50fd5315eea1128b`
- `FCE网站最终版/campus-3-test-1/index.html`；SHA-256 `f188efacc5b8d935308a58f39844476e5da56f7eb8ffe28f7589b8d9a6dcd508`

逐页视觉读取原卷 PDF 第 1–13 页，封面与 Test 1 配对；阅读与语用为 PDF 2–13 / 印刷 8–19。答案 PDF 第 1 页 / 印刷 120，逐题转录并核对 1–52。时长 75 分钟。未读取其他试卷，不导入 Writing、Listening、Speaking。

## 数量

| 表 | 行数 |
|---|---:|
| edition | 1 |
| book | 1 |
| exam_paper | 1 |
| exam_section | 7 |
| passage_paragraph | 38 |
| option_set | 16 |
| question_option | 68 |
| question | 52 |
| question_answer | 52 |
| question_evidence | 73 |

已保留网站译文 38 段，题目解析 52 题。新增 PDF 副标题无网站译文时为空。knowledge 缺失时为 SQL NULL；accepted_answers 全部 SQL NULL。

## 差异与变换

PDF 决定原文/选项/题干；答案 PDF 决定答案。网站只补充已有翻译与题目级解析。所有差异见 provenance.json 的 differences；以下列出答案与内容差异（统一空格投影详见该文件）。

- `P3.P3-p1.text`：网站 `I've discovered an (0) amazing sport: kayaking. It looks very {{17}} (IMPRESS) when you see it on TV, and apparently it's been increasing in {{18}} (POPULAR) over the past few years. I'm not actually a very sporty person but when my sister, a keen kayaker herself, bought me a lesson for my birthday that was my opportunity to have a go.` → `I've discovered an (0) amazing sport: kayaking. It looks very {{17}} when you see it on TV, and apparently it's been increasing in {{18}} over the past few years. I'm not actually a very sporty person but when my sister, a keen kayaker herself, bought me a lesson for my birthday that was my opportunity to have a go.`；提示词在 PDF 行末独立列；从正文移到题干末行。
- `P3.P3-p2.text`：网站 `It was a {{19}} (RELIEVE) to discover I wasn't the only beginner – everyone else was also very {{20}} (EXPERIENCE) like me. At first we were all pretty {{21}} (HOPE), and some of us even fell in the water, but we learnt quickly and our confidence began to grow.` → `It was a {{19}} to discover I wasn't the only beginner – everyone else was also very {{20}} like me. At first we were all pretty {{21}}, and some of us even fell in the water, but we learnt quickly and our confidence began to grow.`；提示词在 PDF 行末独立列；从正文移到题干末行。
- `P3.P3-p3.text`：网站 `I loved being on the river. The {{22}} (SURROUND) were so beautiful and relaxing that I was really reluctant to get out of the kayak when the lesson finished! It was a truly {{23}} (MEMORY) day. I signed up for a one-week course without a moment's hesitation. Now I'm just as {{24}} (PASSION) about kayaking as my sister.` → `I loved being on the river. The {{22}} were so beautiful and relaxing that I was really reluctant to get out of the kayak when the lesson finished! It was a truly {{23}} day. I signed up for a one-week course without a moment's hesitation. Now I'm just as {{24}} about kayaking as my sister.`；提示词在 PDF 行末独立列；从正文移到题干末行。
- `P7.P7-C.text`：网站 `Though Dealt got together to compete with other popular boy bands, their first single, What I Lost, had few of the elements that defined boy band music at the time: none of the sophisticated disco beats that might have been expected. Instead, it's understated rhythm and blues with a trace of folk, and accented by that least cool of instruments, the accordion. It was a fantastic debut; four years later, it's still unique, as different from the usual boy-band style as accordions are from electronic synthesizers, though both were used on the song. And although the subject matter, romance, is familiar, there's a clever twist. What I Lost is set in a courtroom. It was a track that was hard to improve on, and they never did.` → `Though Dealt got together to compete with other popular boy bands, their first single, What I Lost, had few of the elements that defined boy band music at the time: none of the sophisticated disco beats that might have been expected. Instead, it's understated rhythm and blues with a trace of folk, and accented by that least cool of instruments, the accordion. It was a fantastic debut; four years later, it's still unique, as different from the usual boy-band style as accordions are from electronic synthesizers, though both were used on the song. And although the subject matter, romance, is familiar, there's a clever twist: What I Lost is set in a courtroom. It was a track that was hard to improve on, and they never did.`；PDF 第 13 页原文使用冒号。
- `P2.P2-p1.text`：网站 `(0) have you ever tried a new sport or learnt to play a musical instrument? {{9}} so, you'll know that once you figure {{10}} how to do it and get good at it, you won't lose your skills, even when you haven't practised for a long time. Most experts put this down to 'muscle memory', which means the brain remembers an action and can recall it when needed. Now some researchers believe there's another important factor: errors that occur while learning a task.` → `(0) Have you ever tried a new sport or learnt to play a musical instrument? {{9}} so, you'll know that once you figure {{10}} how to do it and get good at it, you won't lose your skills, even when you haven't practised for a long time. Most experts put this down to 'muscle memory', which means the brain remembers an action and can recall it when needed. Now some researchers believe there's another important factor: errors that occur while learning a task.`；例题 HAVE 填入句首，采用句首大写。
- `P6.P6-p4.text`：网站 `The school is the first to use this simple technique to try to improve students' performance. Light tells the brain to halt production of melatonin – the hormone that makes you sleepy. {{39}} Researcher Dr Mariana Figueiro believes that, during the winter, the effects of the lack of light can slowly build up and make your 'body clock' confused. Exposure to light of the correct wavelength and intensity helps the body to know when to switch off in the evening. So you sleep more and feel better the next morning.` → `The school is the first to use this simple technique to improve students' performance. Light tells the brain to halt production of melatonin – the hormone that makes you sleepy. {{39}} Researcher Dr Mariana Figueiro believes that, during the winter, the effects of the lack of light can slowly build up and make your 'body clock' confused. Exposure to light of the correct wavelength and intensity helps the body to know when to switch off in the evening. So you sleep more and feel better the next morning.`；PDF 第 10 页没有网站补入的 try to。
- `P6.P6-p7.text`：网站 `For all these reasons, it's not surprising that only a few head teachers have experimented with the lighting in their schools. At Dragonskolan, head teacher Stellan Andersson initially understood this reluctance. After all, some studies suggested that although people claimed the brighter lights were having a positive effect on them, there was no measurable evidence to support this. Equally though, there was no evidence that they actually caused any harm. {{42}} So, Anderson decided to go ahead with installing them hoping for better academic performance. But, whether this happens or not, the students are certainly enjoying the bright new teaching environment.` → `For all these reasons, it's not surprising that only a few head teachers have experimented with the lighting in their schools. At Dragonskolan, head teacher Stellan Andersson initially understood this reluctance. After all, some studies suggested that although people claimed the brighter lights were having a positive effect on them, there was no measurable evidence to support this. Equally though, there was no evidence that they actually caused any harm. {{42}} So Andersson decided to go ahead with installing them hoping for better academic performance. But, whether this happens or not, the students are certainly enjoying the bright new teaching environment.`；PDF 第 10 页后文为 So Andersson，无逗号；按扫描原卷修正。
- `P6.Q42.analysis`：网站 `no evidence of harm（前提）→ Therefore nothing to lose（结论）→ So Anderson decided to go ahead（行动）：三个环节层层递进，G 是唯一能同时衔接前后因果的选项。` → `no evidence of harm（前提）→ Therefore nothing to lose（结论）→ So Andersson decided to go ahead（行动）：三个环节层层递进，G 是唯一能同时衔接前后因果的选项。`；原文人名与标点修正后同步更新引用及解析，避免保留旧引用。
- `P6.Q42.solve_steps`：网站 `["看空前：'同样地，没有证据表明这些灯造成了任何伤害。'——好处待证实，坏处无证据。", "锁定 G：'Therefore, it seemed on the face of it that there was nothing to lose.'（因此，表面上看来装它们并无损失。）——Therefore 承接'无害'，nothing to lose 推出'可以装'。", "看空后：'So, Anderson decided to go ahead...'——So 与 Therefore 构成'无害→无损失→决定装'的双重因果链，逻辑闭合。选 G。"]` → `["看空前：'同样地，没有证据表明这些灯造成了任何伤害。'——好处待证实，坏处无证据。", "锁定 G：'Therefore, it seemed on the face of it that there was nothing to lose.'（因此，表面上看来装它们并无损失。）——Therefore 承接'无害'，nothing to lose 推出'可以装'。", "看空后：'So Andersson decided to go ahead...'——So 与 Therefore 构成'无害→无损失→决定装'的双重因果链，逻辑闭合。选 G。"]`；原文人名与标点修正后同步更新引用及解析，避免保留旧引用。
- `P6.Q42.evidence`：网站 `[{'paragraph_id': 'P6-p7', 'quote': 'there was no evidence that they actually caused any harm'}, {'paragraph_id': 'P6-p7', 'quote': 'So, Anderson decided to go ahead with installing them hoping for better academic performance'}]` → `[{'paragraph_id': 'P6-p7', 'quote': 'there was no evidence that they actually caused any harm'}, {'paragraph_id': 'P6-p7', 'quote': 'So Andersson decided to go ahead with installing them hoping for better academic performance'}]`；原文人名与标点修正后同步更新引用及解析，避免保留旧引用。
- `P4.Q25.answer`：网站 `hadn't/had not EXPECTED to see` → `hadn’t / had not EXPECTED | to see`；对应答案 PDF 第 1 页逐题视觉转录；保留大小写、斜杠及评分竖线。
- `P4.Q26.answer`：网站 `put us up` → `PUT us | up`；对应答案 PDF 第 1 页逐题视觉转录；保留大小写、斜杠及评分竖线。
- `P4.Q27.answer`：网站 `cannot/can't be BOTHERED` → `cannot/can’t | be BOTHERED`；对应答案 PDF 第 1 页逐题视觉转录；保留大小写、斜杠及评分竖线。
- `P4.Q28.answer`：网站 `keep an eye on` → `keep | an EYE on`；对应答案 PDF 第 1 页逐题视觉转录；保留大小写、斜杠及评分竖线。
- `P4.Q29.answer`：网站 `to DISCOURAGE Jo/her from eating/having` → `to DISCOURAGE Jo/her | from eating/having`；对应答案 PDF 第 1 页逐题视觉转录；保留大小写、斜杠及评分竖线。
- `P4.Q30.answer`：网站 `to take ACCOUNT of/into ACCOUNT` → `to take | ACCOUNT of/into ACCOUNT`；对应答案 PDF 第 1 页逐题视觉转录；保留大小写、斜杠及评分竖线。

- PDF scan line wraps merged within paragraphs; double columns read left then right; website source_keys retained as stable paragraph identifiers after visual correspondence review.
- Straight/curly apostrophe glyph differences and typesetting hyphens retained using existing website transcription except official answer strings; printed line/page markers excluded.
- Examples 0 filled using PDF supplied example answers; no question 0 record.
- Cloze stems are projections of the exact original sentence, not separate printed questions; P6 stem is gap marker; P7 combines common question lead-in with each prompt.
- Website paragraph translations and question analyses retained; missing translations remain empty.
- Pending placeholders are treated conservatively as conflicts; no unapproved catalogue title overwrite.
- P6 保留原卷 Stellan Andersson / 后文 Andersson 的原写法；网站后文 Anderson 已更正（见补充核验记录）。

## 白名单与排除

未知字段：无。根级排除：audio_delivery, audio_note, dictionary, features, generation_preferences, generation_scope, legacy_dictionary, section_order_note。Part 级排除：inquiry, logic_steps, quick_words, sentences, structure, vocabulary, writing_bank。expected_question_ids、exam_summary 仅校验；part/kind 用于题型映射；原网站 answer_source/answer_status 替换为已定位答案册出处，原值保留于 sources/exam-data.json。未把原 JSON 或暂缓模块塞入业务 JSONB。

## 验证与执行边界

静态检查通过：7 Part / 52 题与分配、选项组共享逐字一致、提示词、答案标签/变体、段落和题目顺序、JSON 类型、source_groups 与 logic_links 引用、所有 evidence.quote 和带 quote 的逻辑端点逐字匹配；每一业务列与 001–010 DDL 对齐，SQL 内嵌完整期望数据转义往返一致。使用本机 Flyway 12.11.0 PostgreSQL 脚本解析器检查语句边界：insert.sql 18 条、verify.sql 6 条；此解析器检查分句/引号，不等同服务器 SQL/PLpgSQL 编译。insert.sql 采用 identity 默认 ID、自然键定位、整卷事务及十表同序锁；已有整卷在插入前做双向 EXCEPT 全行集比较，冲突报错回滚；同数据重跑不写 ID/时间戳。目录仅创建实际两卷所需记录，不初始化 48 个位置。pending 占位卷保守拒绝，需明确目录默认值后才能单独制定提升逻辑。

verify.sql 为只读事务，双向比较完整十表业务行集并输出统计；差异查询返回零行才代表内容完全相同。未在 PostgreSQL 执行，未实测首次导入、重跑和冲突回滚，不能称导入成功。默认 schema public，需先应用 001–010；可用 `psql -v ON_ERROR_STOP=1 -f insert.sql`，再执行 verify.sql。
