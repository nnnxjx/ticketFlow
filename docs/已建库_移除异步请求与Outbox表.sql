-- 仅在确认旧异步请求已处理完毕、无需保留历史数据后执行。
-- DROP TABLE 会永久删除两张旧表及其数据；执行前请备份。
USE ticketflow;
DROP TABLE IF EXISTS purchase_request;
DROP TABLE IF EXISTS outbox_event;
