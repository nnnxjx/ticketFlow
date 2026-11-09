-- KEYS: stock, user quota, request token, paused, rejected result, completed marker.
-- ARGV: qty, limit, tier ID, sale start/end epoch milliseconds.
if redis.call('EXISTS', KEYS[5]) == 1 or redis.call('GET', KEYS[6]) == 'RELEASED' then return -7 end
local value = ARGV[3] .. ':' .. ARGV[1]
local previous = redis.call('GET', KEYS[3])
if previous then
  if previous == value then return 2 end
  return -6
end
if redis.call('EXISTS', KEYS[4]) == 1 then return -4 end
if redis.call('EXISTS', KEYS[1]) == 0 then return -3 end
local time = redis.call('TIME')
local now = tonumber(time[1]) * 1000 + math.floor(tonumber(time[2]) / 1000)
if now < tonumber(ARGV[4]) or now >= tonumber(ARGV[5]) then return -5 end
local qty = tonumber(ARGV[1])
if tonumber(redis.call('GET', KEYS[1])) < qty then return -1 end
if tonumber(redis.call('GET', KEYS[2]) or '0') + qty > tonumber(ARGV[2]) then return -2 end
redis.call('DECRBY', KEYS[1], qty)
redis.call('INCRBY', KEYS[2], qty)
-- Keep the token until the consumer confirms or releases it; a retry can re-send the MQ message.
redis.call('SET', KEYS[3], value)
return 1
