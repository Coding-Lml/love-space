-- 生产环境迁移脚本：为 chat_message 增加 space_id，并将历史数据回填到默认空间
-- 前置条件：已执行 migrate_space_visibility.sql（已创建 space/space_member 与默认空间）。

ALTER TABLE `chat_message` ADD COLUMN `space_id` BIGINT NULL COMMENT '空间ID' AFTER `id`;
CREATE INDEX `idx_chat_space_id` ON `chat_message` (`space_id`);

SET @default_space_id = (
  SELECT `id` FROM `space` WHERE `name` = '我们的空间' ORDER BY `id` ASC LIMIT 1
);

UPDATE `chat_message` SET `space_id` = @default_space_id WHERE `space_id` IS NULL;

ALTER TABLE `chat_message` MODIFY COLUMN `space_id` BIGINT NOT NULL COMMENT '空间ID';

