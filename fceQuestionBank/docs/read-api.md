# FCE 题库读取 API

本期接口由已迁移的十张业务表提供数据，供 `FCE网站最终版` 后续接入。网页和已应用的 Flyway 脚本均未修改。

## 接口

| 方法 | 路径 | 返回 |
| --- | --- | --- |
| GET | `/api/v1/catalog` | 版本 → 册 → 试卷目录 |
| GET | `/api/v1/papers/{slug}` | 已发布试卷及其全部 Part、题目与解析 |
| GET | `/api/v1/papers/{slug}/sections/{code}` | 已发布试卷的单个 Part |

示例：

```text
GET /api/v1/catalog
GET /api/v1/papers/standard-1-test-1
GET /api/v1/papers/campus-3-test-1
GET /api/v1/papers/campus-3-test-1/sections/P7
```

目录中展示 `pending` 和 `published`，`available` 只在 `published` 时为 true；`draft` 不进入公开目录。内容接口同时按 slug 和 `published` 查询，未知、待录入或草稿试卷均返回 404。Part code 区分大小写，使用 `P1`—`P7`；不存在的 Part 返回 404。

## 响应约定

目录字段：`editions[].id/code/name/books`、`books[].id/book_no/title/tests`、`tests[].id/test_no/slug/title/subtitle/status/duration_minutes/available`。目录从数据库读取，不在代码中固定册数或试卷数。版本、册按 `sort_order` 排序，试卷按 `test_no` 排序。

整卷字段：`database_id`、`slug`、`title`、`subtitle`、`duration_minutes`、`edition`、`origin`、`exam_summary`、`sections`。`edition` 为源内容版本说明，`origin` 为迁移保存的来源信息；`exam_summary.sections/questions` 根据实际读取内容统计。

Part 字段：`database_id`、`id`、`part`、`part_no`、`kind`、`question_format`、`title`、`paper_reference`、`expected_option_count`、`source_groups`、`shared_options`、`paragraphs`、`questions`。

- 数据库 bigint ID 一律以十进制字符串返回：目录字段为 `id`，试卷正文及其子对象字段为 `database_id`。
- Part 的 `id` 是源标识（如 `P6`）；段落的 `id` 是 `source_key`（如 `P6-p1`）；题目的 `id` 是源题号字符串（如 `37`），另提供整数 `question_no`。
- `part` 统一为 `Part N` 字符串，`part_no` 为整数。源网页两套数据的 `part` 类型不同，接入时可使用稳定的 `part_no`。
- `kind` 使用网页渲染代码：Part 1 为 `cloze`，Part 2—4 为 `grammar`，Part 5/7 为 `reading`，Part 6 为 `seven`。`question_format` 保留数据库的具体题型代码。
- 段落返回 `role/text/translation`，保留换行和 `{{题号}}` 占位符。Part、段落、题目、选项和证据均按数据库保存顺序返回。
- 单题选项组和共用选项组都展开为每题 `options`，格式为 `{ "A": "选项文本" }`。共用组另外提供 Part 级 `shared_options`，无选项题返回空对象。
- 题目返回 `stem/options/answer/answer_display/answer_status/answer_source/question_type/analysis/type_note/strategy/pitfall/solve_steps/distractors/knowledge/logic_links/evidence`。
- `answer` 保留数据库 `raw_answer` 的字符串或字符串数组类型；`answer_display` 为保存的展示文本。不会按斜杠拆分答案，也不提供自动判分或未核验的等价答案。
- `evidence[].paragraph_id` 还原为所属 Part 的源段落标识；`quote` 保持原样。`source_groups` 和 `logic_links` 原样返回。
- 无答案记录时答案相关字段为 null，无证据时 `evidence` 为空数组；不会生成推测答案。
- 教学资料、词典、账号、学习记录等暂缓字段完全不出现在响应中。后续网页接入需保留原有 `quick_words/vocabulary/sentences/structure/inquiry/logic_steps/writing_bank/dictionary/legacy_dictionary` 等静态数据；当前接口不能整体替换网页的全部 `examData`。

错误使用框架 RFC 9457 `application/problem+json`，例如：

```json
{
  "type": "about:blank",
  "title": "Not Found",
  "status": 404,
  "detail": "Published paper not found",
  "instance": "/api/v1/papers/unknown"
}
```

## 实现和验证

`model` 为十张表及公共审计列的只读 JPA 映射，JSONB 使用 Hibernate JSON 类型；`repo` 仅暴露读取方法，`service` 在只读事务中批量读取并组装 DTO，`api` 负责 HTTP 路由及错误响应。整卷查询不会为每道题单独查询选项、答案或证据。生产环境仍使用现有 PostgreSQL 配置和 `ddl-auto: none`。

执行项目 Maven wrapper 或本机 Maven：

```powershell
.\mvnw.cmd test
```

每个测试方法均有中文注释说明目的。服务 UT 使用 Mockito，API UT 使用独立 MockMvc，repo 和应用启动检查使用 `test` profile 下的 H2 内存数据库，并关闭 Flyway。测试不会连接或迁移业务 PostgreSQL。

H2 测试验证 JPA 映射、查询隔离、排序及 JSON/数组读取；不替代 PostgreSQL 的迁移约束验收。后续部署验收可对上述两个已发布 slug 做只读请求核对，每卷应为 7 个 Part、52 道题。

本次已执行完整 Maven 测试：33 项，0 失败、0 错误、0 跳过。环境验证使用本机缓存的 Spring Boot 4.2.0-SNAPSHOT 和 Java 21；本机 Maven 的默认镜像未识别已有 snapshot 缓存，因此测试时使用临时空 settings 指向默认本地缓存，未更改项目或用户的 Maven 设置。
