-- 仅在此前执行过“已建库_异步下单通道.sql”时执行；未添加过字段则不要执行。
USE ticketflow;

ALTER TABLE ticket_tier DROP CHECK ck_tier_channel;
ALTER TABLE ticket_tier DROP COLUMN order_channel;
