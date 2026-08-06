-- LinguaLearn 数据库初始化脚本
CREATE DATABASE IF NOT EXISTS lingua_learn DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE lingua_learn;

-- 用户表
CREATE TABLE IF NOT EXISTS `user` (
    `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
    `username` VARCHAR(32) NOT NULL UNIQUE,
    `email` VARCHAR(128) NOT NULL UNIQUE,
    `password` VARCHAR(256) NOT NULL,
    `avatar` VARCHAR(256) DEFAULT NULL,
    `level` INT NOT NULL DEFAULT 1 COMMENT '等级',
    `xp` INT NOT NULL DEFAULT 0 COMMENT '当前等级经验',
    `total_xp` INT NOT NULL DEFAULT 0 COMMENT '累计总经验',
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户表';

-- 词库表 (多语言支持)
CREATE TABLE IF NOT EXISTS `word_bank` (
    `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
    `language` VARCHAR(8) NOT NULL COMMENT '语言代码: en/ja/de/fr/ko',
    `word` VARCHAR(256) NOT NULL COMMENT '单词或短语',
    `phonetic` VARCHAR(256) DEFAULT NULL COMMENT '音标',
    `translation` VARCHAR(512) NOT NULL COMMENT '中文释义',
    `part_of_speech` VARCHAR(32) DEFAULT NULL COMMENT '词性',
    `example` VARCHAR(1024) DEFAULT NULL COMMENT '例句',
    `difficulty` TINYINT NOT NULL DEFAULT 1 COMMENT '难度 1-5',
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY `uk_lang_word` (`language`, `word`),
    INDEX `idx_language` (`language`),
    INDEX `idx_difficulty` (`language`, `difficulty`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='词库表';

-- 练习记录表
CREATE TABLE IF NOT EXISTS `practice_record` (
    `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
    `user_id` BIGINT NOT NULL,
    `word_id` BIGINT NOT NULL,
    `mode` VARCHAR(16) NOT NULL COMMENT '练习模式: typing/listening/translation',
    `correct` TINYINT(1) NOT NULL DEFAULT 0,
    `answer` VARCHAR(1024) DEFAULT NULL COMMENT '用户答案',
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    INDEX `idx_user_mode` (`user_id`, `mode`),
    INDEX `idx_user_word` (`user_id`, `word_id`),
    INDEX `idx_created` (`user_id`, `created_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='练习记录表';

-- 错题本
CREATE TABLE IF NOT EXISTS `error_book` (
    `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
    `user_id` BIGINT NOT NULL,
    `word_id` BIGINT NOT NULL,
    `error_count` INT NOT NULL DEFAULT 1 COMMENT '累计错误次数',
    `last_error_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `next_review_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '下次复习时间',
    `mastered` TINYINT(1) NOT NULL DEFAULT 0 COMMENT '是否已掌握',
    UNIQUE KEY `uk_user_word` (`user_id`, `word_id`),
    INDEX `idx_user_review` (`user_id`, `mastered`, `next_review_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='错题本';
