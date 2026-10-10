# 两套已读取 PDF 的基线

2026-10-10 按用户更正，以以下四份 PDF 为来源。已逐页读取两套原卷阅读与语用的全部相关扫描页及各自答案第 1 页；其他卷只用于文件名定位，未读取内容。旧 Word 不再是输入。

| slug | 原卷（相对项目根） | 答案 | 原卷总页 | 答案总页 |
| --- | --- | --- | ---: | ---: |
| standard-1-test-1 | 题目/FCE题目/标准版1/标1-T1.pdf | 题目/FCE题目/FCE-ANSWERS/标准版/S01-T1.pdf | 22 | 12 |
| campus-3-test-1 | 题目/FCE题目/校园版3/青3-T1.pdf | 题目/FCE题目/FCE-ANSWERS/校园版/F03-T1.pdf | 21 | 12 |

四份均为扫描件；标准卷只有封面“标1”的文字层，其余提取几乎为空。要进行视觉读取/OCR，不能按空文本推断无内容。

两卷封面为 PDF 第 1 页，Reading and Use of English 第 2–13 页（印刷页 8–19），Writing 从第 14 页、Listening 从第 16 页开始。标准卷第 22 页为 Speaking。两答案册 PDF 第 1 页（印刷页 120）含完整阅读答案 1–52 和 Writing 说明，后续材料不入本期模型。时长 75 分钟。

| Part | 题号 | 两卷 PDF 页序 | 结构 |
| --- | --- | --- | --- |
| P1 | 1–8 | 2–3 | 正文和每题 A–D 选项 |
| P2 | 9–16 | 4 | 开放填空 |
| P3 | 17–24 | 5 | 词形转换及行末提示词 |
| P4 | 25–30 | 6–7 | 原句/关键词/改写句 |
| P5 | 31–36 | 8–9 | 双栏文章与 A–D 选项 |
| P6 | 37–42 | 10–11 | 双栏文章与 A–G 共用句子 |
| P7 | 43–52 | 12–13 | 问题与双栏分组文章；标准 A–D、校园 A–E |

标准文章锚点：Why we need to play；A bicycle you can fold up；Tea；A musician and his pupil；Blind Runner；Why go to university?。
校园文章锚点：Chocolate teapots really are useful；Making mistakes helps you to succeed；Have a go at kayaking；Young writer；Lighting up the winter darkness；Reviews of songs by teenage boy bands。

## 答案核对锚点

- 标准 P1：A C A B C D B D；P5：B D D B A C；P6：D G F A C E；P7：C C A B A C B D B D。
- 校园 P1：C B D A D C D B；P5：B B A D C C；P6：E C A F D G；P7：C A E D B C A B D E。
- 标准 Q9 为 can/may，网站仅 can；Q12 为 not/hardly/scarcely，不能遗漏变体；校园 Q15 为 who/that。重新转录时逐题看原页，不只复制锚点。
- 校园 Q25 的答案册为 hadn't / had not EXPECTED | to see，网站为 hadn't/had not EXPECTED to see。竖线是评分切分，保留在源字符串，不当完整答案分隔。
- 标准 Q30 原句人名为 Jack，目标句为 John，是扫描原卷自己的写法；不凭语义统一人名。

## 网站辅助基线

标准网站为 7 Part、52 题、31 段、24 条证据；校园网站为 7 Part、52 题、38 段、73 条证据。97 条 evidence 的 source_key 可解析且 quote 在网站对应原文中逐字匹配；这不表示 PDF 所有字段与网站逐字相同。PDF 导入段落和证据数不强制等于网站。

HTML 根字段为 sections；standard.section.part 为字符串，campus 为整数，按 P1–P7 映射 part_no；JSON edition 是说明，不是版本代码。

标准网站 P6 expected_option_count=0，但原卷说明明确 A–G，且列出七句。PDF 主来源中间数据应声明 7，同时记录旧网站 0 的修正依据；若用户专门要求原样迁移网站则保留网站 0 并报告，不混淆来源模式。

网站 P7 source_groups 允许“题目”组及公共标题，不强制与选项逐一对应。原卷结构可按版面组织，并保持生成的 source_key 稳定。

目录共有 48 个实际试卷文件，但第 2 册 Test 号为 5–8，不能把每册四套等同于每册 Test 1–4。当前请求只读取上述两卷及匹配答案，不初始化其他卷。
