# Flyway 建表迁移说明

## 内容与执行

`src/main/resources/db/migration` 中的 001—010 文件按依赖顺序创建数据库设计文档中的 10 张业务表，包括约束、索引和中文注释，不包含业务数据。011—012 为两套试卷的数据迁移。

项目使用 Spring Boot Flyway starter 与 PostgreSQL 专用模块。启动应用时 Flyway 从 `classpath:db/migration` 读取脚本，通过应用配置中的 DataSource 连接数据库。数据库应事先存在；迁移账号需要目标 schema 的建表权限。SQL 使用 PostgreSQL identity、JSONB 和 JSONPath 功能（语法要求 PostgreSQL 12 或以上，实际服务器版本还须与项目 Flyway 版本兼容）。

采用自定义版本命名 `001_create_edition.sql`，配置为：

```yaml
spring:
  flyway:
    enabled: true
    locations: classpath:db/migration
    sql-migration-prefix: ""
    sql-migration-separator: "_"
    sql-migration-suffixes: ".sql"
    validate-migration-naming: true
    fail-on-missing-locations: true
```

使用独立 Flyway CLI 时也需要提供等价的 prefix、separator、suffixes 设置；Spring Boot 的 YAML 不会自动成为 CLI 配置。

每个文件由 Flyway 管理事务与历史。已执行版本不会因再次启动而重复执行。后续修改使用 011 及之后的新迁移，不修改已经应用的文件。Hibernate 继续使用 `ddl-auto: none`，不启用 `baseline-on-migrate`。

## 试卷数据迁移 011—012

- `011_import_standard_1_test_1.sql`：标准版 1 Test 1，7 Part、52 题及对应答案与解析。
- `012_import_campus_3_test_1.sql`：校园版 3 Test 1，7 Part、52 题及对应答案、已有译文与解析。

两份脚本遵循空前缀、单下划线命名规则，事务与版本历史由 Flyway 管理，脚本不含顶层 BEGIN/COMMIT。原卷核验记录、中间数据、独立插入脚本和只读回读脚本保留在 `data-import/<slug>/`。

应用下次启动时会自动执行尚未应用的数据迁移。脚本使用 public schema，按固定顺序锁定十张业务表，完整比较现有数据；相同内容无更新，内容冲突或不完整旧卷报错回滚。每卷核验完成后目标状态为 published。已有 pending 占位卷保守拒绝，不能自动覆盖。

本次仅生成迁移文件并检查命名和 SQL 分句，未启动应用或执行数据库迁移；此前 001—010 的运行验证不涵盖这两份数据迁移。标准卷网站段落译文为空，因此仍为空；校园卷保留已有 38 段译文。

## IF NOT EXISTS 的边界

表和独立索引均使用 `IF NOT EXISTS`。该语句只检查同名对象是否存在，不检查定义是否一致，也不补齐已有表中的字段或约束。已有结构不兼容时，后续外键、索引或注释语句仍可能失败。

已有业务表且没有 Flyway 历史的非空 schema 可能被 Flyway 拒绝初始化；不能以开启自动 baseline 绕过检查。本次脚本用于空 schema 或已正确管理的迁移链，不会自动接管未知旧结构。

## IDEA 启动与依赖同步

已定位过一次启动不迁移问题：运行中的 IDEA Java 进程只有 `flyway-core` 和 PostgreSQL 模块，没有 `spring-boot-flyway` 自动配置模块。POM 文件变更不会更新已经运行的 JVM，IDEA 的 Maven 项目模型也需要重新同步。

1. 在 IDEA Maven 工具窗口执行 Reload All Maven Projects，确认 `spring-boot-starter-flyway` 及其传递依赖 `spring-boot-flyway` 均已解析。
2. 停止旧的 FceQuestionBankApplication 进程，重新构建并启动。
3. 日志应先出现“开始执行 Flyway 数据库迁移”，首次迁移后出现 `Successfully applied 10 migrations`；再次启动应报告没有待执行迁移。

`FlywayConfig` 使用 `@ImportAutoConfiguration` 显式接入 Boot 的迁移模块，保留自动配置排序及数据库初始化依赖关系。策略只由 Boot 的 FlywayMigrationInitializer 调用一次，不在 ApplicationRunner 再次调用迁移。迁移失败原样抛出并终止启动。`spring.flyway.enabled=false` 仍可明确关闭迁移。

配置开启路径缺失检查与 Flyway INFO 日志，避免迁移目录缺失时静默跳过。命名规则仍保持空前缀和单下划线。

## 验证状态

- 已使用本机 Flyway 12.11.0 的文件名解析器验证 10 个文件的版本与名称。
- 已通过其 PostgreSQL 脚本解析器检查所有 114 条 SQL 语句，并静态核对外键创建顺序。
- 已在本机 PostgreSQL 18.0 上新建随机命名的独立临时数据库，以覆盖后的 datasource 参数启动实际应用：首次执行全部 10 个迁移，生成 10 张业务表及迁移历史；第二次启动没有待执行迁移，两次 Flyway validate 均通过。临时数据库已删除。
- 新增 FlywayConfigTests：验证真实 YAML 命名配置、初始化器仅调用一次、显式禁用时不迁移，以及迁移策略调用和异常传播；测试不连接业务数据库。
- Maven package（跳过测试）构建成功；随后单独运行 `-Dtest=FlywayConfigTests test`，3 项测试全部通过，无失败或跳过。
- 未连接或修改 questionBank 业务数据库。此次运行验证不代表所有数据库约束的负向用例均已执行。

## 隔离环境验收步骤

1. 在可用的隔离 PostgreSQL 实例中创建独立测试数据库，明确覆盖应用的 datasource URL、用户名、密码后启动应用，避免使用默认业务库。
2. 检查迁移历史包含成功的 001—010，核对 10 张业务表的字段类型、空值、默认值、主键、唯一键、外键、检查约束、索引及注释。
3. 再次启动确认没有新增迁移记录，执行 Flyway validate 确认校验和一致。
4. 在另一个空测试 schema 中按顺序直接执行 SQL 两次，检查第二次跳过已有表和索引。不要用删除 Flyway 历史的方式模拟重复运行。
5. 通过事务性测试数据检查正常关联成功，并检查重复题号、重复选项标签、跨 Part 选项组/证据引用、非正序号、非法 JSON 类型以及非字符串答案数组均被拒绝。
6. 草稿无答案、无选项题的 option_set_id 为空、知识点 knowledge 为 SQL NULL 应允许；JSON null 不等同于 SQL NULL。

本次不实现 JSONB 内部段落/选项引用、整卷题号完整性与发布完整性的业务校验，相关规则仍留给后续保存及导入层。
