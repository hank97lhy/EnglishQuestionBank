# PostgreSQL 插入脚本生成约定

## 输出模式与事务

默认独立 SQL，以试卷为事务单位（每卷一个 insert.sql）：`BEGIN`、设置明确 schema/search_path、获取锁、冲突检查/插入、整卷验证、`COMMIT`。不要猜测目标 schema，脚本明确注明默认 public；若用户配置目标不同则采用指定 schema。psql 运行时应使用 `-v ON_ERROR_STOP=1`，脚本本体不依赖 psql 特有命令。

用 PostgreSQL 文本字面量转义 `'` 为 `''`，明确设置 `standard_conforming_strings=on`；或选取与所有值不碰撞的 dollar-quote 标签。反斜杠、换行、中文、撇号和 `{{n}}` 必须往返无损。JSON 用真正的序列化后字符串加 `::jsonb`，不要手拼对象、把数组转逗号文本或把 SQL NULL 写成字符串 `'null'`。JSON 中的 `</script>` 不是 SQL 边界。

现有结构最低语法要求 PostgreSQL 12；Flyway 实际支持版本按项目当前依赖。生成 SQL 不改变序列、不分配固定 id、不修改 001–010。

在生成文件时说明事务目标和锁范围。为避免“比较后被别的写入覆盖/新增”的竞态，可按所有导入脚本相同顺序锁定十张表：

```sql
BEGIN;
SET LOCAL standard_conforming_strings = on;
SET LOCAL search_path = public, pg_catalog;
LOCK TABLE edition, book, exam_paper, exam_section, passage_paragraph,
           option_set, question_option, question, question_answer, question_evidence
  IN SHARE ROW EXCLUSIVE MODE;
-- 下面是按实际输入生成的插入与检查，不是全量建表。
```

这是简单、明确的导入串行化方式，会等待其他写事务；不能承诺在线零阻塞。若改为事务级 advisory lock，必须确认所有写入路径采用同一锁，单凭导入脚本锁不能防止其他业务写入。

## 自然键与冲突检测

按 edition → book → paper → section → paragraph → option_set → option → question → answer → evidence 顺序生成。依赖项用自然键查询已有/新插入 ID，查询必须恰好一行；缺失或多行用 RAISE EXCEPTION 失败。可以使用 DO 块局部变量或 CTE/临时映射表，不能假定空库或 id 从 1 开始。

每个自然键：不存在则插入并 RETURNING id；存在则比较全部业务列（忽略 identity、created_at、updated_at），一致返回已有 ID 且不 UPDATE。任意差异报错，指出 slug、Part、题号和字段。`ON CONFLICT DO NOTHING` 只可用在已包含完整比较的流程，不能单独用于隐藏冲突；禁止 `ON CONFLICT DO UPDATE` 覆盖人工编辑。

以下为完整单行逻辑示例（生成器将相同规则展开到每张表，而不是只检查版本）：

```sql
DO $import$
DECLARE v_id bigint;
BEGIN
  SELECT id INTO v_id FROM edition WHERE code = 'standard';
  IF v_id IS NULL THEN
    INSERT INTO edition (code, name, sort_order)
      VALUES ('standard', '标准版', 1) RETURNING id INTO v_id;
  ELSIF EXISTS (
    SELECT 1 FROM edition WHERE id = v_id
      AND (name, sort_order) IS DISTINCT FROM ('标准版'::text, 1)
  ) THEN
    RAISE EXCEPTION 'Import conflict: edition standard';
  END IF;
END;
$import$;
```

比较 JSONB 用结构等值（对象键顺序不重要，数组顺序重要），text[] 保留顺序，SQL NULL 用 IS DISTINCT FROM。所有正文、译文、题干、选项及解析字段都要覆盖。外键先映射数据库 ID 再比较，但 provenance/回读对比使用自然键。

仅检查输入逐行匹配不足以发现数据库里多余内容。导入开始即判断该卷是否已有任意内容：若有则整卷做双向集合比较，任何缺行、多行、排序变化或字段变化都终止，不尝试自动“补齐”半份旧卷。可把输入投影为临时期望表，现有内容也投影为父自然键 + 业务列，用双向 EXCEPT 或 FULL JOIN 做差异检查；NULL 和 JSONB 需正确比较。回读 SQL 使用相同完整投影。

## pending 占位卷与状态

目录初始化可能先创建 pending 占位卷，仅目录 title/subtitle 等为空或目录默认值。唯一可自动提升的情况：自然键和 slug 精确匹配，status=pending，完全没有 Part/题目等内容，元数据仍是用户认可的目录默认值（source_metadata={}、edition_note=''、duration_minutes IS NULL 等），并已识别其具体目录 title。否则冲突；不能把所有 pending 记录当作可覆盖占位符。

满足条件的占位卷可以在同一事务内更新为实际元数据和 draft，记录 updated_at；新卷插入 draft。父目录名称或排序不同仍应冲突，不通过占位提升顺便覆盖 edition/book。

内容写完后验证完整性。仅当原卷内容已核对、答案齐全、引用通过、发布阻塞事项解决时才转 published 并更新 updated_at。不以 INSERT 成功代替发布校验。保留源声明的选项数差异须明确确认后才能发布。

重跑时目标状态也要比较：已 published 的卷，不能因一次候选输入 draft 而降级；已有 draft 的元数据/内容全部一致且这次符合发布条件，明确允许 draft→published。其他状态变更按冲突处理。完全相同的状态和内容重跑不写时间戳。

如果无法核实某段/题但仍有结构完整的草稿，可交付 draft 脚本和报告；若自然键或引用结构本身无效，只交候选数据/错误报告，不输出能误执行的半成品 SQL。

## 整卷 SQL 验证与回读

在 COMMIT 前使用 DO/断言查询检查 Part/题号范围、答案（发布时）、选项共享、无选项题、来源分组/逻辑端点、证据、所有输入集合完整性。SQL 可以用源 JSON 临时期望数据辅助断言，但不要把整份网站 JSON 存入业务表。

数据库外键能约束证据和选项所属 Part，但不能检查 source_groups/logic_links 内的 source_key。解析 JSONB 的这些引用，按 section_id 连接 paragraph/option；生成前检查与 SQL 提交前检查相配合。

verify.sql 必须只读，不创建数据、不 UPDATE、不标发布。以 slug 选中试卷，再按自然键回读所有白名单字段，对照 paper.json 的投影检查双向差异；如果采用 JSONB 期望值 CTE，则保留正确 JSON 类型。不能仅靠“52 题”确认迁移正确。

数据库运行验证（仅在授权的测试环境）：首次导入；同数据重跑行数、已有 ID 和 updated_at 不变；改一道题干造成冲突且整卷回滚；重复题号、共享选项差异、跨 Part 引用、非法 JSON 类型、缺失/多余旧数据被拒绝；回读与期望完整相同。可准备验证用临时输入，不能改变原卷/网站或业务数据库来做负向测试。

## Flyway 模式

当用户要求新增数据迁移时，选择当前目录中下一个未用版本（当前 001–010 并不保证未来仍然如此），如 `011_import_standard_1_test_1.sql`。保持项目空 prefix、单下划线 separator；一个试卷一个文件，不加入 BEGIN/COMMIT，事务由 Flyway 管理。失败迁移不记成功；每卷完整导入和发布检查仍不可省略。不要开启 baseline-on-migrate 绕过已有结构，不把数据脚本生成请求变为应用启动请求。
