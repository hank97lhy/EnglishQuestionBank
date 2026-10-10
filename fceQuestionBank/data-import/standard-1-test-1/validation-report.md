# standard-1-test-1 导入校验报告

状态：published（脚本目标状态）；发布阻塞：无。仅生成文件，未连接数据库。

## 来源和核验

- `题目/FCE题目/标准版1/标1-T1.pdf`；SHA-256 `daf7c96d99549b9fcc2426e6f147cc22c280905307ea499efd13fbb70e418e6c`
- `题目/FCE题目/FCE-ANSWERS/标准版/S01-T1.pdf`；SHA-256 `63ea560e23c29168f8c3a4b9e827204036a8be619981e1b0d02335126c4032aa`
- `FCE网站最终版/standard-1-test-1/index.html`；SHA-256 `da8481aedfb26053154741c1c31f8959aa0d1f8a3b21cd47358eeecfdc93c0b1`

逐页视觉读取原卷 PDF 第 1–13 页，封面与 Test 1 配对；阅读与语用为 PDF 2–13 / 印刷 8–19。答案 PDF 第 1 页 / 印刷 120，逐题转录并核对 1–52。时长 75 分钟。未读取其他试卷，不导入 Writing、Listening、Speaking。

## 数量

| 表 | 行数 |
|---|---:|
| edition | 1 |
| book | 1 |
| exam_paper | 1 |
| exam_section | 7 |
| passage_paragraph | 34 |
| option_set | 16 |
| question_option | 67 |
| question | 52 |
| question_answer | 52 |
| question_evidence | 24 |

已保留网站译文 0 段，题目解析 52 题。标准网站原文段落 translation 全为空，因此仍为空；未自动补写翻译。新增 PDF 副标题无网站译文时为空。knowledge 缺失时为 SQL NULL；accepted_answers 全部 SQL NULL。

## 差异与变换

PDF 决定原文/选项/题干；答案 PDF 决定答案。网站只补充已有翻译与题目级解析。所有差异见 provenance.json 的 differences；以下列出答案与内容差异（统一空格投影详见该文件）。

- `P6.expected_option_count`：网站 `0` → `7`；PDF 10–11 页说明及 A–G 七个选项。
- `P5.paragraphs.caption`：网站 `None` → `Paul Williams interviews the famous pianist Alfred Brendel.`；补入 PDF 第 8 页原有副标题。
- `P6.paragraphs.caption`：网站 `None` → `Paul Hardy reports on a blind runner called Simon Wheatcroft who enjoys taking part in marathon and ultra-marathon races, running distances between 42 km and 160 km.`；补入 PDF 第 10 页原有副标题。
- `P7.paragraphs.caption`：网站 `None` → `Four graduates talk about their experiences.`；补入 PDF 第 13 页原有副标题。
- `P4.P4-p0.text`：网站 `Complete the second sentence so that it has a similar meaning to the first sentence. Use the word given and write between two and five words.` → `For questions 25–30, complete the second sentence so that it has a similar meaning to the first sentence, using the word given. Do not change the word given. You must use between two and five words, including the word given.`；恢复 PDF 第 6 页完整要求（不含示例与答题卡操作说明）。
- `P2.Q9.answer`：网站 `can` → `can/may`；对应答案 PDF 第 1 页逐题视觉转录；保留大小写、斜杠及评分竖线。
- `P2.Q12.answer`：网站 `not / hardly / scarcely` → `not/hardly/scarcely`；对应答案 PDF 第 1 页逐题视觉转录；保留大小写、斜杠及评分竖线。
- `P4.Q25.answer`：网站 `FEW programmes were sold` → `FEW programmes | were sold`；对应答案 PDF 第 1 页逐题视觉转录；保留大小写、斜杠及评分竖线。
- `P4.Q26.answer`：网站 `INSTEAD of taking / catching / getting` → `INSTEAD of | taking/catching/getting`；对应答案 PDF 第 1 页逐题视觉转录；保留大小写、斜杠及评分竖线。
- `P4.Q27.answer`：网站 `had NEVER broken` → `had/’d NEVER | broken`；对应答案 PDF 第 1 页逐题视觉转录；保留大小写、斜杠及评分竖线。
- `P4.Q28.answer`：网站 `would LOOK into / at` → `would | LOOK into/at`；对应答案 PDF 第 1 页逐题视觉转录；保留大小写、斜杠及评分竖线。
- `P4.Q29.answer`：网站 `was / got postponed BECAUSE it rained` → `was/got postponed | BECAUSE it rained`；对应答案 PDF 第 1 页逐题视觉转录；保留大小写、斜杠及评分竖线。
- `P4.Q30.answer`：网站 `to CARRY on working` → `to CARRY on | working`；对应答案 PDF 第 1 页逐题视觉转录；保留大小写、斜杠及评分竖线。

- PDF scan line wraps merged within paragraphs; double columns read left then right; website source_keys retained as stable paragraph identifiers after visual correspondence review.
- Straight/curly apostrophe glyph differences and typesetting hyphens retained using existing website transcription except official answer strings; printed line/page markers excluded.
- Examples 0 filled using PDF supplied example answers; no question 0 record.
- Cloze stems are projections of the exact original sentence, not separate printed questions; P6 stem is gap marker; P7 combines common question lead-in with each prompt.
- Website paragraph translations and question analyses retained; missing translations remain empty.
- Pending placeholders are treated conservatively as conflicts; no unapproved catalogue title overwrite.
- Q30 保留原卷原句 Jack / 目标句 John 的不一致，不自行改名。

## 白名单与排除

未知字段：无。根级排除：audio_note, features, section_order_note。Part 级排除：quick_words, sentences, structure, vocabulary, writing_bank。expected_question_ids、exam_summary 仅校验；part/kind 用于题型映射；原网站 answer_source/answer_status 替换为已定位答案册出处，原值保留于 sources/exam-data.json。未把原 JSON 或暂缓模块塞入业务 JSONB。

## 验证与执行边界

静态检查通过：7 Part / 52 题与分配、选项组共享逐字一致、提示词、答案标签/变体、段落和题目顺序、JSON 类型、source_groups 与 logic_links 引用、所有 evidence.quote 和带 quote 的逻辑端点逐字匹配；每一业务列与 001–010 DDL 对齐，SQL 内嵌完整期望数据转义往返一致。使用本机 Flyway 12.11.0 PostgreSQL 脚本解析器检查语句边界：insert.sql 18 条、verify.sql 6 条；此解析器检查分句/引号，不等同服务器 SQL/PLpgSQL 编译。insert.sql 采用 identity 默认 ID、自然键定位、整卷事务及十表同序锁；已有整卷在插入前做双向 EXCEPT 全行集比较，冲突报错回滚；同数据重跑不写 ID/时间戳。目录仅创建实际两卷所需记录，不初始化 48 个位置。pending 占位卷保守拒绝，需明确目录默认值后才能单独制定提升逻辑。

verify.sql 为只读事务，双向比较完整十表业务行集并输出统计；差异查询返回零行才代表内容完全相同。未在 PostgreSQL 执行，未实测首次导入、重跑和冲突回滚，不能称导入成功。默认 schema public，需先应用 001–010；可用 `psql -v ON_ERROR_STOP=1 -f insert.sql`，再执行 verify.sql。
