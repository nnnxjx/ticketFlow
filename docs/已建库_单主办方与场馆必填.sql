-- 执行前检查是否有一人多个主办方，以及未设置场馆的场次；先处理这些数据。
USE ticketflow;

SELECT owner_user_id, COUNT(*) AS organizer_count
FROM organizer GROUP BY owner_user_id HAVING COUNT(*) > 1;

SELECT id, event_id FROM event_session WHERE venue_id IS NULL;

-- 以上两项查询都无结果后，再执行以下结构调整。
ALTER TABLE organizer
  DROP INDEX idx_organizer_owner,
  ADD UNIQUE KEY uk_organizer_owner (owner_user_id);

ALTER TABLE event_session
  MODIFY COLUMN venue_id BIGINT NOT NULL;
