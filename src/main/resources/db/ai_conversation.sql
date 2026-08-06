-- AI 对话记录表
CREATE TABLE IF NOT EXISTS ai_conversation (
    id          BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id     BIGINT       NOT NULL COMMENT '用户ID',
    title       VARCHAR(200) NOT NULL DEFAULT '新对话' COMMENT '对话标题',
    create_time DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_user (user_id),
    INDEX idx_user_time (user_id, update_time DESC)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='AI对话会话表';

-- AI 对话消息表
CREATE TABLE IF NOT EXISTS ai_conversation_message (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY,
    conversation_id BIGINT       NOT NULL COMMENT '对话ID',
    user_id         BIGINT       NOT NULL COMMENT '用户ID',
    role            VARCHAR(16)  NOT NULL COMMENT '角色: user/assistant',
    content         MEDIUMTEXT   NOT NULL COMMENT '消息内容',
    create_time     DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_conversation (conversation_id),
    INDEX idx_conversation_time (conversation_id, create_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='AI对话消息表';
