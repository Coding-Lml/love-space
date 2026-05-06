-- 生产环境迁移脚本：确保实时聊天消息表与未读查询索引存在。

CREATE TABLE IF NOT EXISTS `chat_message` (
    `id` BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '消息ID',
    `space_id` BIGINT NOT NULL COMMENT '空间ID',
    `from_user_id` BIGINT NOT NULL COMMENT '发送方用户ID',
    `to_user_id` BIGINT NOT NULL COMMENT '接收方用户ID',
    `type` VARCHAR(20) NOT NULL COMMENT '消息类型: text/image/audio/sticker',
    `content` TEXT DEFAULT NULL COMMENT '文本内容或补充说明',
    `media_url` VARCHAR(255) DEFAULT NULL COMMENT '媒体文件URL（图片/语音等）',
    `extra` JSON DEFAULT NULL COMMENT '扩展字段，如 {"duration":12}',
    `status` VARCHAR(20) NOT NULL DEFAULT 'sent' COMMENT '状态: sent/read',
    `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '发送时间',
    INDEX `idx_space_id` (`space_id`),
    INDEX `idx_user_pair_time` (`from_user_id`, `to_user_id`, `created_at`),
    INDEX `idx_to_user_time` (`to_user_id`, `created_at`),
    INDEX `idx_chat_unread` (`space_id`, `to_user_id`, `from_user_id`, `status`, `created_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='聊天消息表';

DELIMITER //
CREATE PROCEDURE add_chat_index_if_missing()
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM information_schema.statistics
        WHERE table_schema = DATABASE()
          AND table_name = 'chat_message'
          AND index_name = 'idx_chat_unread'
    ) THEN
        CREATE INDEX `idx_chat_unread`
            ON `chat_message` (`space_id`, `to_user_id`, `from_user_id`, `status`, `created_at`);
    END IF;
END//
DELIMITER ;

CALL add_chat_index_if_missing();
DROP PROCEDURE add_chat_index_if_missing;
