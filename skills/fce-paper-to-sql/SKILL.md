---
name: fce-paper-to-sql
description: 将 FCE 试卷 PDF 与对应答案 PDF（含扫描件）解析为符合 questionBankWebsite 当前 PostgreSQL 十表设计的数据库插入脚本，支持页面渲染、视觉读取或 OCR、答案逐题核验及静态网站解析补充。适用于新增试卷和生成导入 SQL 或 Flyway 数据迁移，不用于 Word、建表或导入听力、口语、独立写作。
---

# FCE 试卷到数据库插入脚本

交付可审阅、可追溯的结构化数据、PostgreSQL 插入脚本和校验报告。默认只生成文件；用户明确要求执行时才连接目标数据库。原卷、静态网站、既有建表迁移保持原样。

## 定位输入与数据库契约

先定位项目根目录：它应包含 `题目/FCE题目/` 和 `fceQuestionBank/`；`FCE网站最终版/` 是可选辅助材料。当前基准根目录为 `E:/PersonalDocument/project/questionBankWebsite`，迁移到别处时按目录结构定位，不依赖固定盘符。试卷在 `题目/FCE题目/标准版N/标N-TM.pdf` 或 `校园版N/青N-TM.pdf`；答案在 `题目/FCE题目/FCE-ANSWERS/标准版/SNN-TM.pdf` 或 `校园版/FNN-TM.pdf`（NN 两位补零，扩展名大小写不敏感）。只打开用户指定卷及匹配答案，不遍历读取其他卷。第 2 册实际 Test 号有 5–8，不把 Test 范围硬编码为 1–4，也不按目录中的第几个文件重编号。

读取当前 `fceQuestionBank/src/main/resources/plan/database-design.md`、`flyway-migrations.md` 和 `db/migration/001` 至 `010` 的实际 SQL。设计表达业务意图，SQL 表达现有可执行结构；两者冲突时报告，不能偷偷改表或生成不兼容 SQL。再读 [references/mapping.md](references/mapping.md)；核对这两套来源时读 [references/source-baseline.md](references/source-baseline.md)。

从原卷、封面、目录或用户指示确定 edition、book_no、test_no、slug。青少版对应 `campus`，标准版对应 `standard`；JSON 的 `edition` 是说明文字，不能用作版本代码。新试卷定位存在歧义且影响自然键时询问用户，其余解析可以继续。

## 原卷解析与来源核对

读取 [references/extraction.md](references/extraction.md)。附带脚本提取 PDF 页级文字、哈希、页面尺寸并渲染扫描页，可同时读取答案 PDF 和可选 HTML；需 pypdf，渲染另需 pypdfium2，可使用 Codex bundled Python。脚本不包含 OCR 引擎，也不自动理解题目，空文本必须走视觉读取或可用 OCR：

```powershell
python "<skill-dir>/scripts/inspect_sources.py" --pdf "<root>/题目/FCE题目/标准版1/标1-T1.pdf" --answers "<root>/题目/FCE题目/FCE-ANSWERS/标准版/S01-T1.pdf" --pages 2-13 --answer-pages 1 --render --out "<output>/standard-1-test-1/sources"
```

先核验封面、Test 标题和答案册标题的配对，再确定页面范围；上述页范围仅对已核验的这两卷有效。只处理 Reading and Use of English。在进入 Writing/Listening/Speaking 章节前截断；这些章节会重复 Part 编号和题号，答案册后续页也含听力答案/录音稿。示例题 0 不入 question，可按原卷示例填入正文并记录变换。逐页读取相关扫描页，保留标题、段落、双栏阅读顺序、题干换行、Part 3 行末提示词及 Part 4 原句/关键词/改写句。空格统一为 `{{题号}}` 时记录 PDF 页、区域与变换。PDF 页序与印刷页码分开记录。

来源优先级按字段确定：原卷 PDF 决定原文、题干、选项、提示词与时长；对应答案 PDF 决定参考答案及变体；可选网站 `script#examData` 补充翻译和题目级解析。逐题核对答案，不能只验证 52 个题号。已确认 PDF 与网站不同，以 PDF 为准，在 provenance 和差异报告保留网站原值及修正依据；扫描模糊或配对不确定时保留候选并阻止发布。不能仅复制网站 JSON 就声称扫描原卷或答案已经读取。修正答案/正文后要重新检查已有解析、证据、译文和逻辑引用，不保留已与正确答案矛盾的解析。

没有网站时，由模型直接按本 skill 组织中间数据，不要求先生成静态网站；原卷+答案 PDF 足以生成核心内容。缺少可靠答案可以生成草稿插入脚本；不给推断答案标 `official`。核验本地匹配答案册后可用 `official` 表示该参考答案来源已定位，不能声称对出版真实性作了额外认证。默认不补写缺失翻译和解析，确需补写时明确生成来源。

## 组织中间数据并验证

每卷建立 `paper.json`，包含 `catalog`（edition_code/name、book_no、test_no、slug）、`paper`（目标试卷字段）和有序 `sections`。section 使用 `code/part_no/question_format`、有序 paragraphs、option_sets、questions；问题用 `question_no`，证据与逻辑端点仍以本 Part 的 source_key/option label 表达，数据库 ID 尚不分配。PDF 无段落 ID 时在 Part 内按阅读顺序生成稳定 source_key，并记录来源区域；不依据扫描换行把每行当段落。另建 `provenance.json` 保存试卷与答案文件 SHA-256、PDF 页序/印刷页码/区域、视觉或 OCR 读取方式、核验状态、答案原始抄录、网站 JSON 路径与改动理由。不要把整份原始 JSON 或暂缓字段塞进业务 JSONB。

答案 PDF 通常每题是一段带 `/`（可选变体）或 `|`（评分切分）的文字，raw_answer 默认存完整抄录字符串，display_text 保持该字符串，不把评分分隔符当多个独立答案。只有源明确给出独立完整答案数组时才保留数组类型。accepted_answers 仍为 SQL NULL；不自行展开局部替换或新增评分字段。

按 mapping 做白名单映射，区分缺失值、空字符串、空数组和 SQL NULL；未知字段列入报告，不能静默丢弃。检查：

- 七个 Part 的代码、题型、顺序与题号完整性；本期题号 1–52，分配为 1–8、9–16、17–24、25–30、31–36、37–42、43–52。
- 源 expected_question_ids（如存在）与实际题号一致；同卷不重复题号，同 Part 不重复段落标识、排序和选项标签。
- Part 1/5 每题选项组；Part 6/7 一个 shared 组且各题选项内容完全一致；Part 2/3/4 无选项组。声明数量与实际数量分别保留并报告。
- `{{题号}}` 引用属于当前 Part；不强制所有题必须出现在原文占位符中。source_groups、evidence、logic_links 的段落/选项端点全部可解析；distractors 键须属于选项组。
- 字段 JSON 类型符合 DDL，字符串数组元素也是字符串。选择题答案标签存在，填空题答案不能误当选项。
- PDF 扫描读取不能把空文本当作零题；每卷逐题核验 52 道参考答案（当前两卷共 104 道），保留答案册的所有可选词和 Part 4 评分切分符，不混入听力 1–30 的答案。
- evidence.quote 逐字匹配对应原文；logic_links 端点如带 quote，也检查对应段落或选项文本。规范化匹配只能用于诊断，不能替换原值。引用无法匹配则阻止发布，可交付明确标为 draft 的脚本。

不要把两套网站的 69 段/97 条证据等基线套到新试卷。缺答案的草稿不生成空答案记录，accepted_answers 默认 SQL NULL。answer_status 复制来源明确提供的值；需要自定义状态时在报告说明，不能凭空认证官方出处。

## 生成 SQL 与交付

生成 SQL 前读取 [references/sql-generation.md](references/sql-generation.md)。使用 identity 默认主键、自然键定位、整卷事务、同数据重跑无操作及内容冲突报错；不要使用固定 ID、覆盖式 upsert 或删除重建。不存在的目录创建实际卷所需记录；48 个目录位置的全量初始化仅在用户要求初始化目录时生成，不给空位置造 Part/题目。

默认输出到 `fceQuestionBank/data-import/<slug>/`：

- `paper.json`、`provenance.json`：白名单结构化内容与来源。
- `insert.sql`：独立脚本，含 BEGIN/COMMIT；脚本头标明需先应用 001–010、目标版本与 slug、draft/published 及阻塞事项。
- `verify.sql`：只读统计、引用核对和与中间数据逐项回读比较；比较必须覆盖完整行集，不能只比数量。
- `validation-report.md`：源文件、哈希、统计、未知/排除字段、差异、缺失答案、发布阻塞及实际验证范围。

若用户要求 Flyway 数据迁移，读取当前迁移目录选择下一个未使用版本，遵守空前缀、单下划线命名；由 Flyway 管理事务，脚本内不加 BEGIN/COMMIT。不修改已应用迁移。只生成数据文件不会连接业务数据库；把文件放入自动执行迁移目录应符合用户请求。

运行附带脚本的提取检查，再静态检查生成 SQL 的表、列、类型、依赖顺序、自然键及引用。只有获得数据库执行授权且环境允许时，才在指定测试数据库验证导入、原样重跑、冲突回滚和回读；没有实际运行就写“静态检查通过，尚未在 PostgreSQL 执行”，不能写“导入成功”。完成后给出输出文件链接、内容数量、draft/published 状态及未解决差异。
