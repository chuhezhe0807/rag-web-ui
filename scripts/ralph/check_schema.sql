-- scripts/ralph/check_schema.sql
-- US-008: 打印 7 张业务表的 column_name / data_type / is_nullable，
-- 结果应当与 scripts/ralph/expected_schema.md 所列完全一致。
--
-- 运行方式（主机上）：
--   docker exec -i rag-service-db-1 mysql -uroot -proot ragwebui \
--       < scripts/ralph/check_schema.sql
-- 或直接在容器内：
--   mysql -uroot -proot ragwebui < /path/to/check_schema.sql
--
-- 想要只看某一张表，改 IN (...) 白名单即可。

USE `ragwebui`;

SELECT
    table_name,
    column_name,
    column_type,
    data_type,
    is_nullable,
    column_key,
    column_default,
    extra
FROM information_schema.columns
WHERE table_schema = 'ragwebui'
  AND table_name IN (
      'users',
      'knowledge_bases',
      'documents',
      'document_uploads',
      'processing_tasks',
      'document_chunks',
      'api_keys'
  )
ORDER BY
    FIELD(table_name,
        'users',
        'knowledge_bases',
        'documents',
        'document_uploads',
        'processing_tasks',
        'document_chunks',
        'api_keys'),
    ordinal_position;
