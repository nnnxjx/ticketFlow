-- KEYS: stock, user quota, request token, completed marker.
if redis.call('GET', KEYS[4]) == 'RELEASED' then return 2 end
if redis.call('GET', KEYS[4]) == 'CONFIRMED' then return 3 end
local value = redis.call('GET', KEYS[3])
if not value then return -1 end
local qty = tonumber(string.match(value, '^[^:]+:(%d+)$'))
if not qty or redis.call('EXISTS', KEYS[1]) == 0 then return -2 end
if tonumber(redis.call('GET', KEYS[2]) or '0') < qty then return -3 end
redis.call('DEL', KEYS[3])
redis.call('INCRBY', KEYS[1], qty)
redis.call('DECRBY', KEYS[2], qty)
redis.call('SET', KEYS[4], 'RELEASED')
return 1
