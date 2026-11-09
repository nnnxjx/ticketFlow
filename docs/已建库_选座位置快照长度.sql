-- 旧库执行一次：容纳区域、排号、座号拼接后的完整选座位置快照。
USE ticketflow;
ALTER TABLE ticket_order_item MODIFY COLUMN item_name_snapshot VARCHAR(160) NOT NULL;
