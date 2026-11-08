-- 已按旧版 schema.sql 建库时执行；仅移除票档的单笔限制，保留每用户累计限购。
USE ticketflow;

ALTER TABLE ticket_tier DROP CHECK ck_tier_limits;
ALTER TABLE ticket_tier DROP COLUMN per_order_limit;
ALTER TABLE ticket_tier ADD CONSTRAINT ck_tier_limits CHECK (per_user_limit > 0);
