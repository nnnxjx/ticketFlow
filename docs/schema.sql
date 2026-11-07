-- TicketFlow v1 / MySQL 8.0.16+ / UTC / utf8mb4
-- 首版整单取消、整单退款；订单明细一行一张票。
-- ID 由应用生成（雪花 ID 或等价方案）；所有写操作在服务层校验归属与状态转换。
CREATE DATABASE IF NOT EXISTS ticketflow CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci;
USE ticketflow;

CREATE TABLE users (
  id BIGINT NOT NULL PRIMARY KEY,
  phone VARCHAR(32) NOT NULL,
  password_hash VARCHAR(255) NOT NULL,
  status VARCHAR(16) NOT NULL DEFAULT 'ACTIVE',
  created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  updated_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
  UNIQUE KEY uk_users_phone (phone),
  CONSTRAINT ck_users_status CHECK (status IN ('ACTIVE','DISABLED'))
) ENGINE=InnoDB;

CREATE TABLE organizer (
  id BIGINT NOT NULL PRIMARY KEY,
  owner_user_id BIGINT NOT NULL,
  name VARCHAR(128) NOT NULL,
  status VARCHAR(16) NOT NULL DEFAULT 'ACTIVE',
  created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  updated_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
  KEY idx_organizer_owner (owner_user_id),
  CONSTRAINT ck_organizer_status CHECK (status IN ('ACTIVE','DISABLED'))
) ENGINE=InnoDB;

CREATE TABLE venue (
  id BIGINT NOT NULL PRIMARY KEY,
  organizer_id BIGINT NOT NULL,
  name VARCHAR(128) NOT NULL,
  address VARCHAR(255) NULL,
  created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  updated_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
  KEY idx_venue_org (organizer_id)
) ENGINE=InnoDB;

CREATE TABLE venue_seat (
  id BIGINT NOT NULL PRIMARY KEY,
  venue_id BIGINT NOT NULL,
  area VARCHAR(64) NOT NULL,
  row_no VARCHAR(32) NOT NULL,
  seat_no VARCHAR(32) NOT NULL,
  created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  UNIQUE KEY uk_venue_seat_position (venue_id,area,row_no,seat_no),
  KEY idx_venue_seat_venue (venue_id)
) ENGINE=InnoDB;

CREATE TABLE event (
  id BIGINT NOT NULL PRIMARY KEY,
  organizer_id BIGINT NOT NULL,
  title VARCHAR(200) NOT NULL,
  category VARCHAR(64) NULL,
  description TEXT NULL,
  status VARCHAR(16) NOT NULL DEFAULT 'DRAFT',
  created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  updated_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
  KEY idx_event_org_status (organizer_id,status),
  KEY idx_event_status_created (status,created_at),
  CONSTRAINT ck_event_status CHECK (status IN ('DRAFT','PUBLISHED','OFF_SHELF'))
) ENGINE=InnoDB;

CREATE TABLE event_session (
  id BIGINT NOT NULL PRIMARY KEY,
  event_id BIGINT NOT NULL,
  venue_id BIGINT NULL,
  sale_mode VARCHAR(16) NOT NULL,
  sale_start DATETIME(3) NOT NULL,
  sale_end DATETIME(3) NOT NULL,
  start_time DATETIME(3) NOT NULL,
  end_time DATETIME(3) NOT NULL,
  checkin_start DATETIME(3) NOT NULL,
  checkin_end DATETIME(3) NOT NULL,
  status VARCHAR(16) NOT NULL DEFAULT 'DRAFT',
  created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  updated_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
  KEY idx_session_event_start (event_id,start_time),
  KEY idx_session_venue_start (venue_id,start_time),
  CONSTRAINT ck_session_times CHECK (sale_start < sale_end AND start_time < end_time AND checkin_start < checkin_end),
  CONSTRAINT ck_session_mode CHECK (sale_mode IN ('GENERAL','ASSIGNED')),
  CONSTRAINT ck_session_status CHECK (status IN ('DRAFT','ON_SALE','STOPPED','FINISHED'))
) ENGINE=InnoDB;

CREATE TABLE ticket_tier (
  id BIGINT NOT NULL PRIMARY KEY,
  session_id BIGINT NOT NULL,
  name VARCHAR(80) NOT NULL,
  price DECIMAL(12,2) NOT NULL,
  total_stock INT NOT NULL,
  available_stock INT NULL,
  per_order_limit INT NOT NULL,
  per_user_limit INT NOT NULL,
  sale_start DATETIME(3) NOT NULL,
  sale_end DATETIME(3) NOT NULL,
  status VARCHAR(16) NOT NULL DEFAULT 'DRAFT',
  created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  updated_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
  UNIQUE KEY uk_tier_session_name (session_id,name),
  KEY idx_tier_session_sale (session_id,status,sale_start,sale_end),
  CONSTRAINT ck_tier_stock CHECK (total_stock >= 0 AND (available_stock IS NULL OR available_stock BETWEEN 0 AND total_stock)),
  CONSTRAINT ck_tier_price CHECK (price >= 0),
  CONSTRAINT ck_tier_limits CHECK (per_order_limit > 0 AND per_user_limit >= per_order_limit),
  CONSTRAINT ck_tier_window CHECK (sale_start < sale_end),
  CONSTRAINT ck_tier_status CHECK (status IN ('DRAFT','ON_SALE','STOPPED'))
) ENGINE=InnoDB;

CREATE TABLE session_seat (
  id BIGINT NOT NULL PRIMARY KEY,
  session_id BIGINT NOT NULL,
  venue_seat_id BIGINT NOT NULL,
  price DECIMAL(12,2) NOT NULL,
  status VARCHAR(16) NOT NULL DEFAULT 'FREE',
  lock_order_id BIGINT NULL,
  lock_expire_at DATETIME(3) NULL,
  version BIGINT NOT NULL DEFAULT 0,
  created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  updated_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
  UNIQUE KEY uk_session_physical_seat (session_id,venue_seat_id),
  KEY idx_seat_session_status (session_id,status),
  KEY idx_seat_lock_order (lock_order_id,status),
  CONSTRAINT ck_seat_status CHECK (status IN ('FREE','LOCKED','SOLD','UNAVAILABLE')),
  CONSTRAINT ck_seat_price CHECK (price >= 0),
  CONSTRAINT ck_seat_lock CHECK ((status = 'LOCKED' AND lock_order_id IS NOT NULL AND lock_expire_at IS NOT NULL) OR (status <> 'LOCKED' AND lock_expire_at IS NULL)),
  CONSTRAINT ck_seat_version CHECK (version >= 0)
) ENGINE=InnoDB;

CREATE TABLE user_tier_quota (
  user_id BIGINT NOT NULL,
  tier_id BIGINT NOT NULL,
  active_qty INT NOT NULL DEFAULT 0,
  updated_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
  PRIMARY KEY (user_id,tier_id),
  CONSTRAINT ck_quota_nonnegative CHECK (active_qty >= 0)
) ENGINE=InnoDB;

CREATE TABLE purchase_request (
  id BIGINT NOT NULL PRIMARY KEY,
  user_id BIGINT NOT NULL,
  request_id VARCHAR(64) NOT NULL,
  payload_hash BINARY(32) NOT NULL,
  tier_id BIGINT NOT NULL,
  quantity INT NOT NULL,
  status VARCHAR(16) NOT NULL DEFAULT 'INIT',
  order_id BIGINT NULL,
  reservation_expire_at DATETIME(3) NOT NULL,
  failure_code VARCHAR(64) NULL,
  created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  updated_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
  UNIQUE KEY uk_purchase_user_request (user_id,request_id),
  UNIQUE KEY uk_purchase_order (order_id),
  KEY idx_purchase_recover (status,updated_at),
  CONSTRAINT ck_purchase_qty CHECK (quantity > 0),
  CONSTRAINT ck_purchase_status CHECK (status IN ('INIT','RESERVED','ORDERED','REJECTED','RELEASED'))
) ENGINE=InnoDB;

CREATE TABLE ticket_order (
  id BIGINT NOT NULL PRIMARY KEY,
  user_id BIGINT NOT NULL,
  session_id BIGINT NOT NULL,
  tier_id BIGINT NULL,
  request_id VARCHAR(64) NOT NULL,
  sale_mode VARCHAR(16) NOT NULL,
  quantity INT NOT NULL,
  amount DECIMAL(12,2) NOT NULL,
  status VARCHAR(24) NOT NULL,
  expire_at DATETIME(3) NOT NULL,
  paid_at DATETIME(3) NULL,
  created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  updated_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
  UNIQUE KEY uk_order_user_request (user_id,request_id),
  KEY idx_order_user_status_created (user_id,status,created_at),
  KEY idx_order_session_created (session_id,created_at),
  KEY idx_order_expire (status,expire_at),
  CONSTRAINT ck_order_qty_amount CHECK (quantity > 0 AND amount >= 0),
  CONSTRAINT ck_order_mode CHECK (sale_mode IN ('GENERAL','ASSIGNED')),
  CONSTRAINT ck_order_tier CHECK ((sale_mode='GENERAL' AND tier_id IS NOT NULL) OR (sale_mode='ASSIGNED' AND tier_id IS NULL)),
  CONSTRAINT ck_order_status CHECK (status IN ('PENDING_PAYMENT','PAID','TICKETED','CANCELLED','REFUNDING','REFUNDED'))
) ENGINE=InnoDB;

CREATE TABLE ticket_order_item (
  id BIGINT NOT NULL PRIMARY KEY,
  order_id BIGINT NOT NULL,
  tier_id BIGINT NULL,
  session_seat_id BIGINT NULL,
  item_name_snapshot VARCHAR(80) NOT NULL,
  price_snapshot DECIMAL(12,2) NOT NULL,
  created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  KEY idx_item_seat (session_seat_id),
  KEY idx_item_order (order_id),
  CONSTRAINT ck_item_source CHECK ((tier_id IS NOT NULL AND session_seat_id IS NULL) OR (tier_id IS NULL AND session_seat_id IS NOT NULL)),
  CONSTRAINT ck_item_price CHECK (price_snapshot >= 0)
) ENGINE=InnoDB;

CREATE TABLE payment_record (
  id BIGINT NOT NULL PRIMARY KEY,
  order_id BIGINT NOT NULL,
  payment_no VARCHAR(64) NOT NULL,
  amount DECIMAL(12,2) NOT NULL,
  status VARCHAR(16) NOT NULL,
  paid_at DATETIME(3) NULL,
  created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  UNIQUE KEY uk_payment_order (order_id),
  UNIQUE KEY uk_payment_no (payment_no),
  CONSTRAINT ck_payment_amount CHECK (amount >= 0),
  CONSTRAINT ck_payment_status CHECK (status IN ('SUCCEEDED'))
) ENGINE=InnoDB;

CREATE TABLE ticket (
  id BIGINT NOT NULL PRIMARY KEY,
  order_item_id BIGINT NOT NULL,
  token_hash BINARY(32) NOT NULL,
  token_ciphertext VARBINARY(512) NOT NULL,
  key_version SMALLINT NOT NULL,
  status VARCHAR(16) NOT NULL DEFAULT 'UNUSED',
  issued_at DATETIME(3) NOT NULL,
  used_at DATETIME(3) NULL,
  updated_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
  UNIQUE KEY uk_ticket_item (order_item_id),
  UNIQUE KEY uk_ticket_token_hash (token_hash),
  CONSTRAINT ck_ticket_status CHECK (status IN ('UNUSED','USED','REFUNDING','INVALID'))
) ENGINE=InnoDB;

CREATE TABLE checkin_record (
  id BIGINT NOT NULL PRIMARY KEY,
  ticket_id BIGINT NOT NULL,
  operator_id BIGINT NOT NULL,
  checked_at DATETIME(3) NOT NULL,
  UNIQUE KEY uk_checkin_ticket (ticket_id),
  KEY idx_checkin_operator_time (operator_id,checked_at)
) ENGINE=InnoDB;

CREATE TABLE refund_record (
  id BIGINT NOT NULL PRIMARY KEY,
  order_id BIGINT NOT NULL,
  refund_no VARCHAR(64) NOT NULL,
  amount DECIMAL(12,2) NOT NULL,
  previous_order_status VARCHAR(24) NOT NULL,
  status VARCHAR(16) NOT NULL,
  failure_code VARCHAR(64) NULL,
  created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  updated_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
  UNIQUE KEY uk_refund_no (refund_no),
  KEY idx_refund_order_status (order_id,status),
  CONSTRAINT ck_refund_amount CHECK (amount >= 0),
  CONSTRAINT ck_refund_previous CHECK (previous_order_status IN ('PAID','TICKETED')),
  CONSTRAINT ck_refund_status CHECK (status IN ('PROCESSING','SUCCEEDED','FAILED'))
) ENGINE=InnoDB;

CREATE TABLE inventory_log (
  id BIGINT NOT NULL PRIMARY KEY,
  operation_id VARCHAR(96) NOT NULL,
  order_id BIGINT NOT NULL,
  tier_id BIGINT NULL,
  session_seat_id BIGINT NULL,
  operation VARCHAR(24) NOT NULL,
  quantity INT NOT NULL,
  created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  UNIQUE KEY uk_inventory_operation (operation_id),
  KEY idx_inventory_order (order_id),
  KEY idx_inventory_tier_time (tier_id,created_at),
  CONSTRAINT ck_inventory_qty CHECK (quantity > 0),
  CONSTRAINT ck_inventory_source CHECK ((tier_id IS NOT NULL AND session_seat_id IS NULL) OR (tier_id IS NULL AND session_seat_id IS NOT NULL)),
  CONSTRAINT ck_inventory_operation CHECK (operation IN ('ORDER_HOLD','ORDER_RELEASE','REFUND_RESALE','SEAT_LOCK','SEAT_RELEASE','SEAT_SOLD','SEAT_RESALE'))
) ENGINE=InnoDB;

CREATE TABLE outbox_event (
  id BIGINT NOT NULL PRIMARY KEY,
  event_type VARCHAR(64) NOT NULL,
  biz_id BIGINT NOT NULL,
  payload JSON NOT NULL,
  status VARCHAR(16) NOT NULL DEFAULT 'NEW',
  retry_count INT NOT NULL DEFAULT 0,
  next_retry_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  updated_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
  UNIQUE KEY uk_outbox_business (event_type,biz_id),
  KEY idx_outbox_dispatch (status,next_retry_at),
  CONSTRAINT ck_outbox_retry CHECK (retry_count >= 0),
  CONSTRAINT ck_outbox_status CHECK (status IN ('NEW','SENDING','SENT','FAILED'))
) ENGINE=InnoDB;

-- 关键事务 SQL 示例（参数由应用绑定）：
-- GENERAL 扣库存：先校验票档所属场次 sale_mode='GENERAL'，再执行
--   UPDATE ticket_tier SET available_stock=available_stock-:qty
--   WHERE id=:tierId AND available_stock>=:qty;
-- 限购：先 INSERT IGNORE user_tier_quota(user_id,tier_id,active_qty) VALUES (:userId,:tierId,0);
--   UPDATE user_tier_quota SET active_qty=active_qty+:qty
--   WHERE user_id=:userId AND tier_id=:tierId AND active_qty+:qty<=:limit;
-- 选座锁定：UPDATE session_seat SET status='LOCKED',lock_order_id=:orderId,
--   lock_expire_at=:expireAt,version=version+1
--   WHERE id=:seatId AND session_id=:sessionId AND status='FREE';
-- 所有条件更新都必须检查影响行数；失败则回滚整笔事务。
