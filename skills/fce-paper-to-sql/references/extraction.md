# PDF 原卷与答案读取

## 文件选择与页码

只打开指定试卷及对应答案，不批量提取全部卷。当前配对和页范围见 source-baseline.md；新卷按封面、Test 标题、文章锚点核验。标准版答案为 SNN-TM.pdf，校园版为 FNN-TM.pdf，NN 是两位册号，M 是原 Test 号；第 2 册 T5–T8 不重编号。

通过 load_workspace_dependencies 定位 bundled Python。脚本需要 pypdf，渲染另需 pypdfium2；两者已在本机可用。脚本不含 OCR 引擎，也不自动理解题目。

```powershell
& "<bundled-python>" "<skill-dir>/scripts/inspect_sources.py" --pdf "<root>/题目/FCE题目/校园版3/青3-T1.pdf" --answers "<root>/题目/FCE题目/FCE-ANSWERS/校园版/F03-T1.pdf" --pages 2-13 --answer-pages 1 --render --out "<output>/campus-3-test-1/sources"
```

可选 --html <matching-index.html>，网站不是前提。--pages/--answer-pages 支持 2-13 或 1,3-5，不指定则读取该文件全部页；使用从 1 开始的 PDF 页序，不是印刷页码。输出 paper-extracted.json、answers-extracted.json，渲染图在 paper-pages/、answers-pages/。printed_page 初始为空，visually_verified 初始 false；查看后在 provenance 记录核验结果，不能由渲染成功自动标为已核验。

## 扫描件和视觉/OCR

这两卷及答案均为扫描件，标准卷仅封面有“标1”文本，其余几乎无文本层。空文本不能当空试卷。逐页看渲染图，或用已有 OCR 转录再逐页对照原图；无 OCR 时可用模型视觉读取。若只能提取文字而无法查看扫描页，则报告阻塞，不拿旧 Word/网站替代 PDF 并声称已经核验。

默认 scale=2；模糊字符可提高到 3–4，必要时按区域查看，也可用已有 Poppler 渲染。needs_visual_or_ocr 只是低文本量提示，不证明其他页面无需核验。

双栏文章先读完整左栏再接右栏，不按相同纵坐标拼句。跨栏段落要合并，扫描换行不是段落。Part 3 提示词来自同一行右侧，不能混入正文；Part 4 保留原句、关键词和目标句换行。题号绑定空格后用 {{n}} 表示，并记录原区域及处理。页码和行号不入正文；题干原有行号引用保留。

示例题 0 不建题目，可按原卷示例填入正文并记录变换。Reading and Use of English 后的 Writing/Listening/Speaking 不导入。

## 答案 PDF

核验 Test 标题和 Reading and Use of English 标题，按 Part + 题号读取 1–52，不只数答案条目。两答案册阅读答案在 PDF 第 1 页（印刷页 120），同页底部的 Writing 与后续听力答案/录音稿不入库。

逐字保留斜杠变体、关键词大小写、缩写、撇号和竖线评分切分。can/may 是完整展示；hadn't / had not EXPECTED | to see 包含局部变体和评分切分，不按 / 或 | 机械拆成独立完整答案。raw_answer 默认为完整转录字符串，display_text 同原文；accepted_answers 为 SQL NULL。只有源明确是完整独立答案数组时才保留数组类型。

OCR 的 I/l/1、C/G、撇号、竖线/斜杠混淆必须看原图，不清楚则保留候选并阻止发布。answer_source 写实际文件名、PDF 页序、Part、题号，可附印刷页码；不能用印刷页 120 定位当前 PDF 的第 120 页。

## 可选网站

只解析 script#examData JSON，不执行 JS。exam-data.json 是辅助原始材料；html-report.json 提供计数、证据字面匹配、共享选项与声明差异，不代替 PDF 和完整导入校验。

网站补充翻译与题目解析；采用旧 source_key 时记录对应 PDF 区域。PDF 题干/原文及答案册优先，差异保留双方原值与采用理由。改变正文或答案后重新核对译文、解析、证据和逻辑端点。旧网站 69 段/97 条证据不强制作为 PDF 解析的计数目标。
