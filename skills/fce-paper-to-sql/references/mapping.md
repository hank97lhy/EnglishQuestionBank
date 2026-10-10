# 白名单字段与数据库映射

本文件是现有设计的导入提示；执行时重新读取 plan 和实际 DDL，字段变化不能只靠这份快照。主来源为试卷 PDF 与答案 PDF；下面的 JSON 字段映射用于可选网站或按 PDF 新建的中间数据，不要求 PDF 先转换成网站。

PDF 直接映射：按封面/路径确定 edition、book、test 和 slug；按原卷标题与时长组织 paper；按 Part 标题建立 section；按阅读顺序建立 paragraph（source_key 在 Part 内稳定生成）；按页面题号、题干和标签建立 question/option；按答案册 Part+题号建立 question_answer。PDF 未提供翻译/解析时文本缺省空串、数组缺省 []、对象缺省 {}、knowledge 为 SQL NULL，不伪造证据。paper.source_metadata 仅保存来源文件名、页码/链接等说明，完整核验记录另放 provenance。

从 PDF 读取的 expected_option_count 按原卷说明/标签确定，标准卷 P6 为 7；website 仅作为辅助时不以其错误声明覆盖 PDF。原卷和答案的优先级及差异处置见 SKILL.md。

## 表与自然键

| 表 | 自然键（所有父对象先按自然键解析 ID） | 来源及内容 |
| --- | --- | --- |
| edition | code | 从卷版本或目录确定 code、name、sort_order |
| book | edition_id, book_no | 册号、title、sort_order |
| exam_paper | book_id, test_no；slug 全局唯一 | title、subtitle、duration_minutes、status、edition_note、source_metadata |
| exam_section | paper_id, code | code、part_no、title、question_format、paper_reference、expected_option_count、sort_order、source_groups |
| passage_paragraph | section_id, source_key | paragraphs[].id→source_key，role（缺省 paragraph）、text、translation（缺省空串）、sort_order |
| option_set | section_id, code | Part 1/5 为 question-题号，Part 6/7 为 shared |
| question_option | option_set_id, label | options 的标签→label，值→content，字母升序→sort_order |
| question | section_id, question_no | question.id 转正整数；stem、question_type→question_type_label 和解析字段 |
| question_answer | question_id | answer、answer_status、answer_source |
| question_evidence | question_id, sort_order | evidence[].paragraph_id 按当前 Part 的 source_key 解析；quote 保留；section_id 由题目派生 |

所有表有 identity bigint id 与默认 created_at/updated_at。不要把源题号或 P1-p1 当作全局 ID。每个数组的 sort_order 从 1 开始，按源顺序保存，不凭源 ID 的词典序重排段落或题目。

## JSON 根层

- `title/subtitle` → exam_paper 同名字段。
- `edition` → edition_note，缺省空串；与目录 edition.code 不同。
- `origin` → source_metadata，缺省对象；保留原来源说明、页码与链接，额外详细溯源放输出 provenance。
- 75 分钟只能在当前两卷或原卷明确给出 1 hour 15 minutes 时采用，未知时为 SQL NULL。
- `sections` 是 Part 数据入口。`expected_question_ids`、`exam_summary` 只作校验，不入库。
- `features`、`audio_note`、`audio_delivery`、`section_order_note`、`generation_scope`、`generation_preferences` 为功能/生成配置，不入库；`dictionary/legacy_dictionary` 不入库。未知根字段列入报告。

## Part

| code | part_no | question_format | 题号 | 选项 |
| --- | ---: | --- | --- | --- |
| P1 | 1 | multiple_choice_cloze | 1–8 | 每题独立组 |
| P2 | 2 | open_cloze | 9–16 | 无 |
| P3 | 3 | word_formation | 17–24 | 无 |
| P4 | 4 | key_word_transformation | 25–30 | 无 |
| P5 | 5 | reading_multiple_choice | 31–36 | 每题独立组 |
| P6 | 6 | gapped_text | 37–42 | 同 Part 共用组 |
| P7 | 7 | multiple_matching | 43–52 | 同 Part 共用组 |

section.id→code；不直接用 kind（P2–P4 不能可靠区分）。part 原始声明用于交叉校验。保留 title、paper_reference、expected_option_count 和 source_groups 原有语义；source_groups 只投影 title 和有序 paragraph_ids，逐个验证所属 Part 及段落相对顺序。未提供时为 []，不强制涵盖全部段落。

Part 级 `quick_words/vocabulary/sentences/structure/inquiry/logic_steps/writing_bank` 跳过且在报告列出，不放入其他 JSONB。paragraphs 允许 id、role、text、translation；出现额外字段要报告。

P6 把 shared_options 与所有 q.options 做标签与内容精确比较；P7 比较所有 q.options；不一致时阻止生成可执行导入 SQL，而不是取第一题或并集。没有足够数据核对时交付候选 JSON 和问题报告。P2–P4 若出现真实选项应报告与本期模型不兼容，不静默清空。

## 题目与答案

question 字段：源 id→question_no，source question_type→question_type_label；源 stem 原样；analysis/type_note/strategy/pitfall 缺省空串；solve_steps/logic_links 缺省 []；distractors 缺省 {}；knowledge 缺失或明确空值→SQL NULL，非空则必须是对象。有源 null 的非空字段不能一律转默认值，先报告或核实。question_type 未提供时可用已确认 Part 的普通题型文字，注明是映射生成，不捏造知识点。

q.options→option_set/question_option；q.answer→question_answer.raw_answer，使用 JSON 字符串或字符串数组。display_text：字符串原样，数组以 ` / ` 连接。accepted_answers 默认 SQL NULL；没有依据不生成评分用答案枚举，尤其不按 `/` 拆分局部替换。

答案 PDF 已配对且逐题核验时，answer_status 可为 official（表示来自本地参考答案册，未额外认证出版真实性），answer_source 写实际 PDF 文件名、PDF 页序、Part、题号。raw_answer 保存完整抄录及 /、| 标记，display_text 同该字符串，accepted_answers 保持 SQL NULL。网站来源的 answer_status/answer_source 仅为源声明，不把 website official 当作答案册核验结果。没有答案时草稿可以没有 question_answer；已有记录不能因输入缺答案被删除。

question.knowledge 和 logic_links 是题目级解析，要保留；与 Part 教学模块不同。logic_links 的 color/label/explanation/endpoints 原样保留；endpoint.paragraph_id 按本 Part source_key 核验，endpoint.option 按本题组标签核验；含 quote 则核验对应文字。不要把这些 JSON 源标识改成数据库 ID。未知 endpoint 结构报告并阻止自动发布。

evidence 保存源顺序，paragraph_id 在 SQL 中变为 passage_paragraph.id；使用 question_id + sort_order 定位同一证据行，不用 quote 去重，多条相同引用也可能有独立顺序。
