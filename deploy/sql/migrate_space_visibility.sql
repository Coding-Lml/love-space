-- 生产环境迁移脚本：引入空间（space/space_member）并为动态（moment）增加空间隔离与公开可见性
-- 说明：
-- 1) 该脚本用于“已有数据库”的升级，不用于新环境初始化（新环境请直接使用 schema.sql）。
-- 2) 该脚本默认你现有系统只有你和对象两位用户（历史版本行为），会创建一个默认空间并把两人加入其中。
-- 3) MySQL 不同版本对 ADD COLUMN IF NOT EXISTS 支持不同，如遇冲突请手动调整。

-- 1. 新增空间表
CREATE TABLE IF NOT EXISTS `space` (
  `id` BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '空间ID',
  `name` VARCHAR(100) NOT NULL COMMENT '空间名称',
  `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='空间表';

CREATE TABLE IF NOT EXISTS `space_member` (
  `id` BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '成员ID',
  `space_id` BIGINT NOT NULL COMMENT '空间ID',
  `user_id` BIGINT NOT NULL COMMENT '用户ID',
  `role` VARCHAR(20) NOT NULL COMMENT '角色: OWNER/MEMBER',
  `joined_at` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '加入时间',
  INDEX `idx_space_id` (`space_id`),
  INDEX `idx_user_id` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='空间成员表';

-- 2. 为 moment 表增加 space_id 与 visibility
ALTER TABLE `moment` ADD COLUMN `space_id` BIGINT NULL COMMENT '空间ID' AFTER `id`;
ALTER TABLE `moment` ADD COLUMN `visibility` VARCHAR(20) NOT NULL DEFAULT 'SPACE' COMMENT '可见性: SPACE/PUBLIC' AFTER `likes`;
CREATE INDEX `idx_moment_space_id` ON `moment` (`space_id`);
CREATE INDEX `idx_moment_visibility` ON `moment` (`visibility`);

-- 3. 创建默认空间并将你们两位用户加入（用户名请按需修改）
INSERT INTO `space` (`name`) VALUES ('我们的空间');
SET @default_space_id = LAST_INSERT_ID();
INSERT INTO `space_member` (`space_id`, `user_id`, `role`)
SELECT @default_space_id, `id`, 'OWNER' FROM `user` WHERE `username` = 'limenglong';
INSERT INTO `space_member` (`space_id`, `user_id`, `role`)
SELECT @default_space_id, `id`, 'MEMBER' FROM `user` WHERE `username` = 'zengfanrui';

-- 4. 回填历史动态到默认空间，并将历史动态默认设为仅空间内可见
UPDATE `moment` SET `space_id` = @default_space_id WHERE `space_id` IS NULL;
UPDATE `moment` SET `visibility` = 'SPACE' WHERE `visibility` IS NULL OR `visibility` = '';

-- 5. 将 space_id 设为 NOT NULL（确认回填完成后再执行）
ALTER TABLE `moment` MODIFY COLUMN `space_id` BIGINT NOT NULL COMMENT '空间ID';

