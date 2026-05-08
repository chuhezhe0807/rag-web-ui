-- ragwebui 应用表初始化脚本
-- 由 rag-ai-service/alembic head (3580c0dcd005) 的 upgrade 串生成：
--   initial_schema
--   -> 59cfa0f1361d (rename metadata->chunk_metadata, 加 document_chunks.document_id & FK)
--   -> fd73eebc87c1 (add document_uploads)
--   -> 5be054bd6587 (add processing_tasks.document_upload_id + FK)
--   -> e214adf7fb66 (add api_keys)
--   -> 3580c0dcd005 (api_keys.key 长度 64 -> 128)
--
-- 用法：本文件挂载到容器 /docker-entrypoint-initdb.d/01_schema.sql，MySQL 首次初始化时按字母序
--       先跑 01_schema.sql（ragwebui） 再跑 mysql-schema.sql（nacos_config）
-- 注意：ragwebui 数据库本身由 docker-compose 的 MYSQL_DATABASE 环境变量创建，这里只建表
-- 维护：后续若新增/修改 alembic migration，必须同步更新本文件，并把 alembic_version 末行的
--       版本号改成新的 head，否则容器首启后 `alembic upgrade head` 会把历史 migration 再跑一遍

USE `ragwebui`;

SET FOREIGN_KEY_CHECKS = 0;

-- ============================================================================
-- users
-- ============================================================================
CREATE TABLE IF NOT EXISTS `users` (
    `id` INT NOT NULL AUTO_INCREMENT,
    `email` VARCHAR(255) NOT NULL,
    `username` VARCHAR(255) NOT NULL,
    `hashed_password` VARCHAR(255) NOT NULL,
    `is_active` TINYINT(1) DEFAULT 1,
    `is_superuser` TINYINT(1) DEFAULT 0,
    `created_at` DATETIME NOT NULL,
    `updated_at` DATETIME NOT NULL,
    PRIMARY KEY (`id`),
    UNIQUE KEY `ix_users_email` (`email`),
    UNIQUE KEY `ix_users_username` (`username`),
    KEY `ix_users_id` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ============================================================================
-- knowledge_bases
-- ============================================================================
CREATE TABLE IF NOT EXISTS `knowledge_bases` (
    `id` INT NOT NULL AUTO_INCREMENT,
    `name` VARCHAR(255) NOT NULL,
    `description` LONGTEXT,
    `user_id` INT NOT NULL,
    `created_at` DATETIME NOT NULL,
    `updated_at` DATETIME NOT NULL,
    PRIMARY KEY (`id`),
    KEY `ix_knowledge_bases_id` (`id`),
    KEY `fk_knowledge_bases_user_id` (`user_id`),
    CONSTRAINT `fk_knowledge_bases_user_id`
        FOREIGN KEY (`user_id`) REFERENCES `users` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ============================================================================
-- documents
-- 备注：file_size 已按 US-008 对齐到 BIGINT NOT NULL（Python BigInteger /
-- Java java.math.BigInteger），跨 2GiB 文件才不会溢出
-- ============================================================================
CREATE TABLE IF NOT EXISTS `documents` (
    `id` INT NOT NULL AUTO_INCREMENT,
    `file_path` VARCHAR(255) NOT NULL,
    `file_name` VARCHAR(255) NOT NULL,
    `file_size` BIGINT NOT NULL,
    `content_type` VARCHAR(100) DEFAULT NULL,
    `file_hash` VARCHAR(64) DEFAULT NULL,
    `knowledge_base_id` INT NOT NULL,
    `created_at` DATETIME NOT NULL,
    `updated_at` DATETIME NOT NULL,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uq_kb_file_name` (`knowledge_base_id`, `file_name`),
    KEY `ix_documents_id` (`id`),
    KEY `ix_documents_file_hash` (`file_hash`),
    CONSTRAINT `fk_documents_knowledge_base_id`
        FOREIGN KEY (`knowledge_base_id`) REFERENCES `knowledge_bases` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ============================================================================
-- chats
-- ============================================================================
CREATE TABLE IF NOT EXISTS `chats` (
    `id` INT NOT NULL AUTO_INCREMENT,
    `title` VARCHAR(255) NOT NULL,
    `user_id` INT NOT NULL,
    `created_at` DATETIME NOT NULL,
    `updated_at` DATETIME NOT NULL,
    PRIMARY KEY (`id`),
    KEY `ix_chats_id` (`id`),
    KEY `fk_chats_user_id` (`user_id`),
    CONSTRAINT `fk_chats_user_id`
        FOREIGN KEY (`user_id`) REFERENCES `users` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ============================================================================
-- chat_knowledge_bases  (chat ↔ knowledge_base 多对多关联表)
-- ============================================================================
CREATE TABLE IF NOT EXISTS `chat_knowledge_bases` (
    `chat_id` INT NOT NULL,
    `knowledge_base_id` INT NOT NULL,
    PRIMARY KEY (`chat_id`, `knowledge_base_id`),
    KEY `fk_chat_kb_knowledge_base_id` (`knowledge_base_id`),
    CONSTRAINT `fk_chat_kb_chat_id`
        FOREIGN KEY (`chat_id`) REFERENCES `chats` (`id`),
    CONSTRAINT `fk_chat_kb_knowledge_base_id`
        FOREIGN KEY (`knowledge_base_id`) REFERENCES `knowledge_bases` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ============================================================================
-- messages  (PRD 9 张表清单没列出它，但 alembic head 里有，chat 流程需要它)
-- ============================================================================
CREATE TABLE IF NOT EXISTS `messages` (
    `id` INT NOT NULL AUTO_INCREMENT,
    `chat_id` INT NOT NULL,
    `role` VARCHAR(50) NOT NULL,
    `content` LONGTEXT NOT NULL,
    `created_at` DATETIME NOT NULL,
    `updated_at` DATETIME NOT NULL,
    PRIMARY KEY (`id`),
    KEY `ix_messages_id` (`id`),
    KEY `fk_messages_chat_id` (`chat_id`),
    CONSTRAINT `fk_messages_chat_id`
        FOREIGN KEY (`chat_id`) REFERENCES `chats` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ============================================================================
-- document_uploads
-- ============================================================================
CREATE TABLE IF NOT EXISTS `document_uploads` (
    `id` INT NOT NULL AUTO_INCREMENT,
    `knowledge_base_id` INT NOT NULL,
    `file_name` VARCHAR(255) NOT NULL,
    `file_hash` VARCHAR(64) NOT NULL,
    `file_size` BIGINT NOT NULL,
    `content_type` VARCHAR(100) NOT NULL,
    `temp_path` VARCHAR(255) NOT NULL,
    `created_at` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `status` VARCHAR(50) NOT NULL DEFAULT 'pending',
    `error_message` TEXT,
    PRIMARY KEY (`id`),
    KEY `ix_document_uploads_created_at` (`created_at`),
    KEY `ix_document_uploads_status` (`status`),
    KEY `fk_document_uploads_knowledge_base_id` (`knowledge_base_id`),
    CONSTRAINT `fk_document_uploads_knowledge_base_id`
        FOREIGN KEY (`knowledge_base_id`) REFERENCES `knowledge_bases` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ============================================================================
-- processing_tasks
-- ============================================================================
CREATE TABLE IF NOT EXISTS `processing_tasks` (
    `id` INT NOT NULL AUTO_INCREMENT,
    `knowledge_base_id` INT DEFAULT NULL,
    `document_id` INT DEFAULT NULL,
    `document_upload_id` INT DEFAULT NULL,
    `status` VARCHAR(50) DEFAULT 'pending',
    `error_message` TEXT,
    `created_at` DATETIME DEFAULT NULL,
    `updated_at` DATETIME DEFAULT NULL,
    PRIMARY KEY (`id`),
    KEY `ix_processing_tasks_id` (`id`),
    KEY `fk_processing_tasks_knowledge_base_id` (`knowledge_base_id`),
    KEY `fk_processing_tasks_document_id` (`document_id`),
    KEY `processing_tasks_document_upload_id_fkey` (`document_upload_id`),
    CONSTRAINT `fk_processing_tasks_knowledge_base_id`
        FOREIGN KEY (`knowledge_base_id`) REFERENCES `knowledge_bases` (`id`),
    CONSTRAINT `fk_processing_tasks_document_id`
        FOREIGN KEY (`document_id`) REFERENCES `documents` (`id`),
    CONSTRAINT `processing_tasks_document_upload_id_fkey`
        FOREIGN KEY (`document_upload_id`) REFERENCES `document_uploads` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ============================================================================
-- document_chunks
-- ============================================================================
CREATE TABLE IF NOT EXISTS `document_chunks` (
    `id` VARCHAR(64) NOT NULL,
    `kb_id` INT NOT NULL,
    `document_id` INT NOT NULL,
    `file_name` VARCHAR(255) NOT NULL,
    `chunk_metadata` JSON DEFAULT NULL,
    `hash` VARCHAR(64) NOT NULL,
    `created_at` DATETIME NOT NULL,
    `updated_at` DATETIME NOT NULL,
    PRIMARY KEY (`id`),
    KEY `idx_kb_file_name` (`kb_id`, `file_name`),
    KEY `ix_document_chunks_hash` (`hash`),
    KEY `fk_document_chunks_document_id` (`document_id`),
    CONSTRAINT `fk_document_chunks_kb_id`
        FOREIGN KEY (`kb_id`) REFERENCES `knowledge_bases` (`id`),
    CONSTRAINT `fk_document_chunks_document_id`
        FOREIGN KEY (`document_id`) REFERENCES `documents` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ============================================================================
-- api_keys
-- ============================================================================
CREATE TABLE IF NOT EXISTS `api_keys` (
    `id` INT NOT NULL AUTO_INCREMENT,
    `key` VARCHAR(128) NOT NULL,
    `name` VARCHAR(255) NOT NULL,
    `user_id` INT NOT NULL,
    `is_active` TINYINT(1) NOT NULL DEFAULT 1,
    `last_used_at` DATETIME DEFAULT NULL,
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    UNIQUE KEY `ix_api_keys_key` (`key`),
    KEY `ix_api_keys_id` (`id`),
    KEY `ix_api_keys_name` (`name`),
    KEY `fk_api_keys_user_id` (`user_id`),
    CONSTRAINT `fk_api_keys_user_id`
        FOREIGN KEY (`user_id`) REFERENCES `users` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ============================================================================
-- alembic_version：与 01_schema.sql 的 DDL 对应的 alembic head
-- 首启后再跑 `alembic upgrade head` 应为 no-op；新增 migration 时必须同步更新两处
-- ============================================================================
CREATE TABLE IF NOT EXISTS `alembic_version` (
    `version_num` VARCHAR(32) NOT NULL,
    PRIMARY KEY (`version_num`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

INSERT IGNORE INTO `alembic_version` (`version_num`) VALUES ('a1b2c3d4e5f6');

SET FOREIGN_KEY_CHECKS = 1;
