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

-- 动态表
CREATE TABLE IF NOT EXISTS `moment` (
    `id` BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '动态ID',
    `user_id` BIGINT NOT NULL COMMENT '用户ID',
    `content` TEXT COMMENT '文字内容',
    `location` VARCHAR(100) DEFAULT NULL COMMENT '位置',
    `likes` INT DEFAULT 0 COMMENT '点赞数',
    `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_at` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `deleted` TINYINT DEFAULT 0 COMMENT '是否删除',
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
    `content` VARCHAR(500) NOT NULL COMMENT '评论内容',
    `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `deleted` TINYINT DEFAULT 0 COMMENT '是否删除',
    INDEX `idx_moment_id` (`moment_id`)
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
    INDEX `idx_user_id` (`user_id`),
    INDEX `idx_diary_date` (`diary_date`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='日记表';

-- 纪念日表
CREATE TABLE IF NOT EXISTS `anniversary` (
    `id` BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '纪念日ID',
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
    INDEX `idx_date` (`date`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='纪念日表';

-- ==========================================
-- 初始化数据
-- ==========================================

-- 插入默认用户（密码都是 love520）
INSERT INTO `user` (`username`, `password`, `nickname`, `avatar`) VALUES
('limenglong', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iKTVKIUi', '李梦龙', '/uploads/images/default-avatar-boy.png'),
('zengfanrui', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iKTVKIUi', '曾凡芮', '/uploads/images/default-avatar-girl.png');

-- 插入恋爱纪念日
INSERT INTO `anniversary` (`title`, `description`, `date`, `type`, `repeat_yearly`, `icon`) VALUES
('在一起', '我们在一起的第一天 💕', '2026-01-21', 'past', 1, '💕');

-- 插入示例纪念日
INSERT INTO `anniversary` (`title`, `description`, `date`, `type`, `repeat_yearly`, `icon`) VALUES
('李梦龙生日', '李梦龙的生日', '2026-06-15', 'past', 1, '🎂'),
('曾凡芮生日', '曾凡芮的生日', '2026-08-20', 'past', 1, '🎂');
