-- 生产环境迁移脚本：为 diary/anniversary 增加 space_id，并将历史数据回填到默认空间
-- 前置条件：已执行 migrate_space_visibility.sql（已创建 space/space_member 与默认空间）。

-- 1) diary 增加 space_id
ALTER TABLE `diary` ADD COLUMN `space_id` BIGINT NULL COMMENT '空间ID' AFTER `id`;
CREATE INDEX `idx_diary_space_id` ON `diary` (`space_id`);

-- 2) anniversary 增加 space_id
ALTER TABLE `anniversary` ADD COLUMN `space_id` BIGINT NULL COMMENT '空间ID' AFTER `id`;
CREATE INDEX `idx_anniversary_space_id` ON `anniversary` (`space_id`);

-- 3) 找到默认空间（按名称）
SET @default_space_id = (
  SELECT `id` FROM `space` WHERE `name` = '我们的空间' ORDER BY `id` ASC LIMIT 1
);

-- 4) 回填历史数据
UPDATE `diary` SET `space_id` = @default_space_id WHERE `space_id` IS NULL;
UPDATE `anniversary` SET `space_id` = @default_space_id WHERE `space_id` IS NULL;

-- 5) 设为 NOT NULL（确认回填完成后再执行）
ALTER TABLE `diary` MODIFY COLUMN `space_id` BIGINT NOT NULL COMMENT '空间ID';
ALTER TABLE `anniversary` MODIFY COLUMN `space_id` BIGINT NOT NULL COMMENT '空间ID';

