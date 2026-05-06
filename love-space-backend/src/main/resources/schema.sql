-- ==========================================
-- Love Space 情侣空间数据库初始化脚本
-- ==========================================

-- 用户表
CREATE TABLE IF NOT EXISTS `user` (
    `id` BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '用户ID',
    `username` VARCHAR(50) NOT NULL UNIQUE COMMENT '用户名',
    `password` VARCHAR(255) NOT NULL COMMENT '密码',
    `nickname` VARCHAR(50) NOT NULL COMMENT '昵称',
    `avatar` VARCHAR(255) DEFAULT NULL COMMENT '头像URL',
    `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_at` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用户表';

-- 空间表（用于隔离不同情侣/小圈子的数据）
CREATE TABLE IF NOT EXISTS `space` (
    `id` BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '空间ID',
    `name` VARCHAR(100) NOT NULL COMMENT '空间名称',
    `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='空间表';

-- 空间成员表
CREATE TABLE IF NOT EXISTS `space_member` (
    `id` BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '成员ID',
    `space_id` BIGINT NOT NULL COMMENT '空间ID',
    `user_id` BIGINT NOT NULL COMMENT '用户ID',
    `role` VARCHAR(20) NOT NULL COMMENT '角色: OWNER/MEMBER',
    `joined_at` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '加入时间',
    INDEX `idx_space_id` (`space_id`),
    INDEX `idx_user_id` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='空间成员表';

-- 动态表
CREATE TABLE IF NOT EXISTS `moment` (
    `id` BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '动态ID',
    `space_id` BIGINT NOT NULL COMMENT '空间ID',
    `user_id` BIGINT NOT NULL COMMENT '用户ID',
    `content` TEXT COMMENT '文字内容',
    `location` VARCHAR(100) DEFAULT NULL COMMENT '位置',
    `likes` INT DEFAULT 0 COMMENT '点赞数',
    `visibility` VARCHAR(20) NOT NULL DEFAULT 'SPACE' COMMENT '可见性: SPACE/PUBLIC',
    `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_at` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `deleted` TINYINT DEFAULT 0 COMMENT '是否删除',
    INDEX `idx_space_id` (`space_id`),
    INDEX `idx_visibility` (`visibility`),
    INDEX `idx_user_id` (`user_id`),
    INDEX `idx_created_at` (`created_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='动态表';

-- 动态媒体表（图片/视频）
CREATE TABLE IF NOT EXISTS `moment_media` (
    `id` BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '媒体ID',
    `moment_id` BIGINT NOT NULL COMMENT '动态ID',
    `type` VARCHAR(10) NOT NULL COMMENT '类型: image/video',
    `url` VARCHAR(255) NOT NULL COMMENT '文件URL',
    `thumbnail` VARCHAR(255) DEFAULT NULL COMMENT '缩略图URL',
    `sort` INT DEFAULT 0 COMMENT '排序',
    `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    INDEX `idx_moment_id` (`moment_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='动态媒体表';

-- 评论表
CREATE TABLE IF NOT EXISTS `comment` (
    `id` BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '评论ID',
    `moment_id` BIGINT NOT NULL COMMENT '动态ID',
    `user_id` BIGINT NOT NULL COMMENT '用户ID',
    `parent_id` BIGINT DEFAULT NULL COMMENT '父评论ID（回复）',
    `reply_to_user_id` BIGINT DEFAULT NULL COMMENT '被回复用户ID',
    `content` VARCHAR(500) NOT NULL COMMENT '评论内容',
    `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `deleted` TINYINT DEFAULT 0 COMMENT '是否删除',
    INDEX `idx_moment_id` (`moment_id`),
    INDEX `idx_parent_id` (`parent_id`),
    INDEX `idx_reply_to_user_id` (`reply_to_user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='评论表';

-- 点赞记录表
CREATE TABLE IF NOT EXISTS `moment_like` (
    `id` BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT 'ID',
    `moment_id` BIGINT NOT NULL COMMENT '动态ID',
    `user_id` BIGINT NOT NULL COMMENT '用户ID',
    `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    UNIQUE KEY `uk_moment_user` (`moment_id`, `user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='点赞记录表';

-- 日记表
CREATE TABLE IF NOT EXISTS `diary` (
    `id` BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '日记ID',
    `space_id` BIGINT NOT NULL COMMENT '空间ID',
    `user_id` BIGINT NOT NULL COMMENT '用户ID',
    `title` VARCHAR(100) DEFAULT NULL COMMENT '标题',
    `content` TEXT NOT NULL COMMENT '内容',
    `mood` VARCHAR(20) DEFAULT NULL COMMENT '心情: happy/sad/love/angry/normal',
    `weather` VARCHAR(20) DEFAULT NULL COMMENT '天气',
    `visibility` VARCHAR(10) DEFAULT 'both' COMMENT '可见性: self/both',
    `diary_date` DATE NOT NULL COMMENT '日记日期',
    `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_at` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `deleted` TINYINT DEFAULT 0 COMMENT '是否删除',
    INDEX `idx_space_id` (`space_id`),
    INDEX `idx_user_id` (`user_id`),
    INDEX `idx_diary_date` (`diary_date`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='日记表';

-- 纪念日表
CREATE TABLE IF NOT EXISTS `anniversary` (
    `id` BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '纪念日ID',
    `space_id` BIGINT NOT NULL COMMENT '空间ID',
    `title` VARCHAR(100) NOT NULL COMMENT '标题',
    `description` VARCHAR(255) DEFAULT NULL COMMENT '描述',
    `date` DATE NOT NULL COMMENT '日期',
    `type` VARCHAR(10) NOT NULL COMMENT '类型: past(纪念日)/future(倒计时)',
    `repeat_yearly` TINYINT DEFAULT 0 COMMENT '是否每年重复',
    `remind` TINYINT DEFAULT 1 COMMENT '是否提醒',
    `remind_days` INT DEFAULT 0 COMMENT '提前几天提醒',
    `icon` VARCHAR(50) DEFAULT '❤️' COMMENT '图标',
    `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_at` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `deleted` TINYINT DEFAULT 0 COMMENT '是否删除',
    INDEX `idx_space_id` (`space_id`),
    INDEX `idx_date` (`date`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='纪念日表';

-- 聊天消息表（双人私聊）
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

-- ==========================================
-- 初始化数据
-- ==========================================

-- 插入默认用户（密码都是 love520）
INSERT INTO `user` (`username`, `password`, `nickname`, `avatar`) VALUES
('limenglong', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iKTVKIUi', '李梦龙', '/uploads/images/default-avatar-boy.png'),
('zengfanrui', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iKTVKIUi', '曾凡芮', '/uploads/images/default-avatar-girl.png');

-- 初始化默认空间（情侣空间）
INSERT INTO `space` (`name`) VALUES ('我们的空间');
SET @default_space_id = LAST_INSERT_ID();
INSERT INTO `space_member` (`space_id`, `user_id`, `role`)
SELECT @default_space_id, `id`, 'OWNER' FROM `user` WHERE `username` = 'limenglong';
INSERT INTO `space_member` (`space_id`, `user_id`, `role`)
SELECT @default_space_id, `id`, 'MEMBER' FROM `user` WHERE `username` = 'zengfanrui';

-- 插入恋爱纪念日
INSERT INTO `anniversary` (`space_id`, `title`, `description`, `date`, `type`, `repeat_yearly`, `icon`) VALUES
(@default_space_id, '在一起', '我们在一起的第一天 💕', '2026-01-21', 'past', 1, '💕');

-- 插入示例纪念日
INSERT INTO `anniversary` (`space_id`, `title`, `description`, `date`, `type`, `repeat_yearly`, `icon`) VALUES
(@default_space_id, '李梦龙生日', '李梦龙的生日', '2026-06-15', 'past', 1, '🎂'),
(@default_space_id, '曾凡芮生日', '曾凡芮的生日', '2026-08-20', 'past', 1, '🎂');
