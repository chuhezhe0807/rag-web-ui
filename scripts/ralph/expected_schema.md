# expected_schema.md — US-008 跨端列类型对齐基线

本文件是 `scripts/ralph/check_schema.sql` 的期望输出 + 三方（Python ORM / Java
MyBatis @TableField / MySQL 实际列）对照表。每张业务表一个小节。

> 校验流程：
> 1. `docker exec -i rag-service-db-1 mysql -uroot -proot ragwebui < scripts/ralph/check_schema.sql`
> 2. 把 `MySQL (actual)` 一列跟下面各表对齐；字段 / 类型 / nullable 应当完全
>    一致。不一致时：要么调 ORM，要么加 alembic migration 并同步更新
>    `.dockervolumes/mysql/sql/01_schema.sql`，不要让三方继续漂移。
>
> 当前基线对齐了 US-008 明确要求的 `documents.file_size`（INT → BIGINT
> NOT NULL），其余"已知遗留 delta"集中记在本文件末尾，作为后续清理 backlog。

## 约定

- `Python ORM` 列来自 `rag-ai-service/app/models/*.py`
- `Java @TableField` 列来自各服务的 `entity/*.java`；没有对应 Java entity
  的表（api_keys）标注 `—`
- `MySQL (actual)` = `check_schema.sql` 打印出来的 `column_type / is_nullable`
  字段；类型用 `varchar(N)` 等完整形式，`NULL/NOT NULL` 对应 IS_NULLABLE
- 时间戳列：Python 侧通过 `TimestampMixin`（默认 DATETIME，Python 侧 default
  = `datetime.utcnow`）；Java 侧 `LocalDateTime` + `@TableField(fill = ...)`
  触发 MP 自动填充；MySQL 侧是 DATETIME 不带 server_default

## users

| column           | Python ORM                              | Java @TableField (rag-user-service User)           | MySQL (actual)           |
|------------------|-----------------------------------------|----------------------------------------------------|--------------------------|
| id               | Integer, PK, index                      | Integer, @TableId(AUTO)                            | int NOT NULL (PRI, auto) |
| email            | String(255), unique, index, NOT NULL    | String                                             | varchar(255) NOT NULL (UNI) |
| username         | String(255), unique, index, NOT NULL    | String                                             | varchar(255) NOT NULL (UNI) |
| hashed_password  | String(255), NOT NULL                   | String, @TableField("hashed_password")             | varchar(255) NOT NULL    |
| is_active        | Boolean, default=True                   | boolean (primitive), @TableField("is_active")      | tinyint(1) NULL DEFAULT 1 |
| is_superuser     | Boolean, default=False                  | boolean (primitive), @TableField("is_superuser")   | tinyint(1) NULL DEFAULT 0 |
| created_at       | DateTime (via TimestampMixin)           | LocalDateTime, fill=INSERT                         | datetime NOT NULL        |
| updated_at       | DateTime (via TimestampMixin)           | LocalDateTime, fill=INSERT_UPDATE                  | datetime NOT NULL        |

## knowledge_bases

| column           | Python ORM                              | Java @TableField (rag-knowledge-service KnowledgeBase) | MySQL (actual)         |
|------------------|-----------------------------------------|--------------------------------------------------------|------------------------|
| id               | Integer, PK, index                      | Integer, @TableField("`id`")                           | int NOT NULL (PRI, auto)|
| name             | String(255), NOT NULL                   | String, @TableField("`name`")                          | varchar(255) NOT NULL  |
| description      | LONGTEXT (mysql dialect)                | String, @TableField("`description`")                   | longtext NULL          |
| user_id          | Integer, FK users.id, NOT NULL          | Integer, @TableField("`user_id`")                      | int NOT NULL (MUL)     |
| created_at       | DateTime                                | LocalDateTime, fill=INSERT                             | datetime NOT NULL      |
| updated_at       | DateTime                                | LocalDateTime, fill=INSERT_UPDATE                      | datetime NOT NULL      |

## documents

| column              | Python ORM                                             | Java @TableField (rag-document-service Document)        | MySQL (actual)            |
|---------------------|--------------------------------------------------------|---------------------------------------------------------|---------------------------|
| id                  | Integer, PK, index                                     | Integer, @TableId(AUTO)                                 | int NOT NULL (PRI, auto)  |
| file_path           | String(255), NOT NULL                                  | String, @TableField("file_path")                        | varchar(255) NOT NULL     |
| file_name           | String(255), NOT NULL (uq with knowledge_base_id)      | String, @TableField("file_name")                        | varchar(255) NOT NULL (UNI part)|
| **file_size**       | **BigInteger, NOT NULL**                               | **BigInteger, @TableField("file_size")**                | **bigint NOT NULL** (US-008 对齐) |
| content_type        | String(100), NOT NULL *(see deltas)*                   | String, @TableField("content_type")                     | varchar(100) NULL         |
| file_hash           | String(64), index                                      | String, @TableField("file_hash")                        | varchar(64) NULL (MUL)    |
| knowledge_base_id   | Integer, FK knowledge_bases.id, NOT NULL               | Integer, @TableField("knowledge_base_id")               | int NOT NULL (MUL)        |
| created_at          | DateTime                                               | LocalDateTime, fill=INSERT                              | datetime NOT NULL         |
| updated_at          | DateTime                                               | LocalDateTime, fill=INSERT_UPDATE                       | datetime NOT NULL         |

## document_uploads

| column              | Python ORM                                       | Java @TableField (rag-document-service DocumentUpload) | MySQL (actual)                    |
|---------------------|--------------------------------------------------|--------------------------------------------------------|-----------------------------------|
| id                  | Integer, PK, index                               | Integer, @TableId(AUTO)                                | int NOT NULL (PRI, auto)          |
| knowledge_base_id   | Integer, FK knowledge_bases.id ON DELETE CASCADE | Integer, @TableField("knowledge_base_id")              | int NOT NULL (MUL)                |
| file_name           | String *(see deltas)*                            | String, @TableField("file_name")                       | varchar(255) NOT NULL             |
| file_hash           | String *(see deltas)*                            | String, @TableField("file_hash")                       | varchar(64) NOT NULL              |
| file_size           | BigInteger, NOT NULL                             | BigInteger, @TableField("file_size")                   | bigint NOT NULL                   |
| content_type        | String *(see deltas)*                            | String, @TableField("content_type")                    | varchar(100) NOT NULL             |
| temp_path           | String *(see deltas)*                            | String, @TableField("temp_path")                       | varchar(255) NOT NULL             |
| created_at          | TIMESTAMP server_default=now()                   | LocalDateTime, fill=INSERT                             | timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP |
| status              | String, server_default='pending'                 | String, @TableField("status")                          | varchar(50) NOT NULL DEFAULT 'pending' |
| error_message       | Text, NULL                                       | String, @TableField("error_message")                   | text NULL                         |

## processing_tasks

| column              | Python ORM                                   | Java @TableField (rag-document-service ProcessingTask) | MySQL (actual)            |
|---------------------|----------------------------------------------|--------------------------------------------------------|---------------------------|
| id                  | Integer, PK, index                           | Integer, @TableId(AUTO)                                | int NOT NULL (PRI, auto)  |
| knowledge_base_id   | Integer, FK knowledge_bases.id (nullable)    | Integer, @TableField("knowledge_base_id")              | int NULL (MUL)            |
| document_id         | Integer, FK documents.id (nullable)          | Integer, @TableField("document_id")                    | int NULL (MUL)            |
| document_upload_id  | Integer, FK document_uploads.id (nullable)   | Integer, @TableField("document_upload_id")             | int NULL (MUL)            |
| status              | String(50), default='pending' (nullable)     | String, @TableField("status")                          | varchar(50) NULL DEFAULT 'pending' |
| error_message       | Text, NULL                                   | String, @TableField("error_message")                   | text NULL                 |
| created_at          | DateTime (nullable)                          | LocalDateTime, fill=INSERT                             | datetime NULL             |
| updated_at          | DateTime (nullable)                          | LocalDateTime, fill=INSERT_UPDATE                      | datetime NULL             |

## document_chunks

| column           | Python ORM                                       | Java @TableField (rag-document-service DocumentChunk)  | MySQL (actual)           |
|------------------|--------------------------------------------------|--------------------------------------------------------|--------------------------|
| id               | String(64), PK (SHA-256)                         | String, @TableId("id")                                 | varchar(64) NOT NULL (PRI)|
| kb_id            | Integer, FK knowledge_bases.id, NOT NULL         | Integer, @TableField("kb_id")                          | int NOT NULL (MUL)       |
| document_id      | Integer, FK documents.id, NOT NULL               | Integer, @TableField("document_id")                    | int NOT NULL (MUL)       |
| file_name        | String(255), NOT NULL                            | String, @TableField("file_name")                       | varchar(255) NOT NULL    |
| chunk_metadata   | JSON (nullable)                                  | ChunkMetadata, typeHandler=JacksonTypeHandler          | json NULL                |
| hash             | String(64), NOT NULL, index                      | String, @TableField("hash")                            | varchar(64) NOT NULL (MUL)|
| created_at       | DateTime (via TimestampMixin)                    | *（无对应字段，见 deltas）*                             | datetime NOT NULL        |
| updated_at       | DateTime (via TimestampMixin)                    | *（无对应字段，见 deltas）*                             | datetime NOT NULL        |

## api_keys

| column           | Python ORM                                       | Java @TableField                                       | MySQL (actual)                       |
|------------------|--------------------------------------------------|--------------------------------------------------------|--------------------------------------|
| id               | Integer, PK, index                               | —                                                      | int NOT NULL (PRI, auto)             |
| key              | VARCHAR(128), unique, index, NOT NULL            | —                                                      | varchar(128) NOT NULL (UNI)          |
| name             | String(255), NOT NULL, index                     | —                                                      | varchar(255) NOT NULL (MUL)          |
| user_id          | Integer, FK users.id, NOT NULL                   | —                                                      | int NOT NULL (MUL)                   |
| is_active        | Boolean, NOT NULL, default=True                  | —                                                      | tinyint(1) NOT NULL DEFAULT 1        |
| last_used_at     | DateTime(timezone=True), NULL                    | —                                                      | datetime NULL                        |
| created_at       | DateTime (via TimestampMixin)                    | —                                                      | datetime NOT NULL DEFAULT CURRENT_TIMESTAMP |
| updated_at       | DateTime (via TimestampMixin)                    | —                                                      | datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP |

（api_keys 是纯 Python 资产，Java 侧暂无对应 entity / controller）

---

## 已知遗留 deltas（US-008 scope 外，后续拆新 story 处理）

这些是现在三方确实不一致、但不阻塞 US-008 的字段。记录在这里避免以后再审时
误当成 bug。需要清理时建议单独开一条 story：

1. **documents.content_type**：Python ORM 写 `String(100), nullable=False`，
   MySQL 实际 `varchar(100) NULL`。含义矛盾——Python 认为必填，但 DB 允许
   NULL。倾向是把 Python 改成 nullable=True（与 alembic initial_schema 的
   `sa.Column('content_type', sa.String(100), nullable=True)` 一致）。

2. **document_uploads.{file_name, file_hash, content_type, temp_path, status}**：
   Python ORM 用的是无长度的 `String`（SQLAlchemy 会退化成数据库默认 TEXT/
   VARCHAR 行为），Java 和 MySQL 都有明确长度（255/64/100/255/50）。当前 DDL
   由 01_schema.sql 定了明确长度，所以三方行为一致；但 Python ORM 源码形式
   上不准。建议把 Python 侧改成 `String(255)` 等显式长度。

3. **document_chunks 时间戳**：Python `DocumentChunk(Base, TimestampMixin)` 有
   created_at / updated_at 两列，MySQL 也建了这两列（由 alembic
   59cfa0f1361d 的 alter 带过来），但 Java `DocumentChunk` entity 里没这两个
   字段——MyBatis select * 会忽略它们，不会报错但 Java 侧拿不到时间信息。
   建议 Java 那边补上 `@TableField(fill=...)` 两个字段。

4. **User.is_active / is_superuser 的可空性**：Python `Boolean, default=True/
   False`（nullable），Java `boolean`（primitive, 不能装 NULL），MySQL
   `tinyint(1) NULL`。MySQL 建表默认 1/0，正常写入不会产生 NULL，但如果手工
   改表为 NULL 会让 Java 反序列化踩坑。要么统一改 MySQL 成 NOT NULL，要么
   Java 换成 Boolean。

5. **api_keys 缺 Java entity**：纯 Python 资产；如果未来 Java 服务也要读写
   api_keys 需要新增 entity + mapper。

6. **api_keys.created_at/updated_at vs 其他表的时间戳语义**：api_keys 的
   时间戳是 MySQL server_default，users/knowledge_bases/documents 的时间戳
   是 Python/Java 应用层填充；两套路径并存且目前都工作。
