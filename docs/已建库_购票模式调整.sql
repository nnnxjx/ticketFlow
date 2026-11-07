-- 仅适用于已经执行旧版 schema.sql、且尚未写入业务数据的数据库。
-- 旧版 sale_mode 在 ticket_tier；新版移到 event_session。
USE ticketflow;

ALTER TABLE event_session
  ADD COLUMN sale_mode VARCHAR(16) NOT NULL DEFAULT 'GENERAL' AFTER venue_id,
  ADD CONSTRAINT ck_session_mode CHECK (sale_mode IN ('GENERAL','ASSIGNED'));

ALTER TABLE ticket_tier
  DROP CHECK ck_tier_stock;

ALTER TABLE ticket_tier
  DROP COLUMN sale_mode,
  ADD CONSTRAINT ck_tier_stock CHECK (
    total_stock >= 0 AND
    (available_stock IS NULL OR available_stock BETWEEN 0 AND total_stock)
  );

-- 建好后，创建场次时须明确填写 sale_mode；待所有调用方适配后可移除上面的 DEFAULT。
