# 两套 FCE 阅读与语用导入

已按原卷扫描页和对应答案册核对标准版 1 Test 1、校园版 3 Test 1，网站仅用于补充已有翻译与题目级解析。每卷 7 Part、52 题、52 条参考答案，脚本目标状态为 published。

| 试卷 | 插入脚本 | 只读回读校验 | 校验报告 |
|---|---|---|---|
| 标准版 1 Test 1 | [insert.sql](standard-1-test-1/insert.sql) | [verify.sql](standard-1-test-1/verify.sql) | [validation-report.md](standard-1-test-1/validation-report.md) |
| 校园版 3 Test 1 | [insert.sql](campus-3-test-1/insert.sql) | [verify.sql](campus-3-test-1/verify.sql) | [validation-report.md](campus-3-test-1/validation-report.md) |

各目录还包含 paper.json、provenance.json、expected-rows.json、static-checks.json 和 sources/ 提取资料及页面图。标准卷 34 段（补入 3 条 PDF 副标题）、24 条证据；校园卷 38 段、73 条证据。标准卷网站没有段落译文，空译文保持为空；校园卷保留已有 38 段译文。

脚本使用默认 public schema，需先应用现有 001–010 迁移。每个 insert.sql 是独立整卷事务；十表同序锁会等待并发写事务。同数据重跑无更新，内容差异、缺行或多行报错回滚。已有 pending 占位卷保守拒绝。verify.sql 为只读完整行集比较，差异查询返回零行才表示一致。

本次仅生成和静态检查文件，没有连接 PostgreSQL，也没有实测导入、重跑或回滚。运行时使用 psql 的 ON_ERROR_STOP=1，并明确指定目标数据库。

项目 Flyway 迁移已提供 `src/main/resources/db/migration/011_import_standard_1_test_1.sql` 和 `012_import_campus_3_test_1.sql`。两份与独立插入脚本的数据及校验逻辑一致，移除了顶层 BEGIN/COMMIT，由 Flyway 管理事务与历史；应用下次启动会自动执行尚未应用的迁移。verify.sql 仍为手动执行的只读校验文件，不放入迁移目录。

build_imports.py 可重建这两套经本次视觉核对的数据；其中答案转录及修正只适用于指定的四份 PDF，不能直接用于其他卷。check_imports.py 检查中间数据、DDL 全业务列和 SQL 内嵌 JSON 转义往返。CheckSql.java 使用本机 Flyway PostgreSQLParser 检查分句与引号，不能替代服务器编译与执行。
