-- 句子级错题表(完形填空 / 翻译练习)
CREATE TABLE IF NOT EXISTS sentence_error (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    sentence_id BIGINT,
    english TEXT COMMENT '英文句子',
    chinese TEXT COMMENT '中文句子',
    mode VARCHAR(20) COMMENT 'cloze / translation',
    slot_results TEXT COMMENT 'JSON: 每个slot的对错情况',
    total_slots INT DEFAULT 0,
    correct_slots INT DEFAULT 0,
    mastered TINYINT DEFAULT 0,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_user_mastered (user_id, mastered)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
