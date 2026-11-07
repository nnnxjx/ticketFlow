-- 仅适用于已经建表、尚无业务数据的数据库。
-- 若 sale_mode 仍在 ticket_tier，先执行 已建库_购票模式调整.sql。
USE ticketflow;

ALTER TABLE event_session
  ADD COLUMN sale_start DATETIME(3) NOT NULL AFTER sale_mode,
  ADD COLUMN sale_end DATETIME(3) NOT NULL AFTER sale_start,
  DROP CHECK ck_session_times,
  ADD CONSTRAINT ck_session_times CHECK (sale_start < sale_end AND start_time < end_time AND checkin_start < checkin_end);

ALTER TABLE session_seat
  DROP INDEX idx_seat_tier_status,
  DROP COLUMN tier_id,
  ADD COLUMN price DECIMAL(12,2) NOT NULL AFTER venue_seat_id,
  ADD KEY idx_seat_session_status (session_id,status),
  ADD CONSTRAINT ck_seat_price CHECK (price >= 0);

ALTER TABLE ticket_order
  MODIFY COLUMN tier_id BIGINT NULL,
  ADD CONSTRAINT ck_order_tier CHECK ((sale_mode='GENERAL' AND tier_id IS NOT NULL) OR (sale_mode='ASSIGNED' AND tier_id IS NULL));

ALTER TABLE ticket_order_item
  MODIFY COLUMN tier_id BIGINT NULL,
  CHANGE COLUMN tier_name_snapshot item_name_snapshot VARCHAR(80) NOT NULL,
  ADD CONSTRAINT ck_item_source CHECK ((tier_id IS NOT NULL AND session_seat_id IS NULL) OR (tier_id IS NULL AND session_seat_id IS NOT NULL));

ALTER TABLE inventory_log
  MODIFY COLUMN tier_id BIGINT NULL,
  ADD COLUMN session_seat_id BIGINT NULL AFTER tier_id,
  ADD CONSTRAINT ck_inventory_source CHECK ((tier_id IS NOT NULL AND session_seat_id IS NULL) OR (tier_id IS NULL AND session_seat_id IS NOT NULL));
