-- KEYS: stock, user quota, operation marker; ARGV: qty, restock flag.
if redis.call('EXISTS', KEYS[3]) == 1 then return 0 end
if ARGV[2] == '1' and redis.call('EXISTS', KEYS[1]) == 0 then return -3 end
local qty = tonumber(ARGV[1])
if tonumber(redis.call('GET', KEYS[2]) or '0') < qty then return -2 end
if ARGV[2] == '1' then redis.call('INCRBY', KEYS[1], qty) end
redis.call('DECRBY', KEYS[2], qty)
redis.call('SET', KEYS[3], '1')
return 1
