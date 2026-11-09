-- An ordered request keeps its deductions. Marker distinguishes retry from token loss.
if redis.call('GET', KEYS[2]) == 'CONFIRMED' then return 2 end
if redis.call('GET', KEYS[2]) == 'RELEASED' then return 3 end
if redis.call('EXISTS', KEYS[1]) == 0 then return -1 end
redis.call('DEL', KEYS[1])
redis.call('SET', KEYS[2], 'CONFIRMED')
return 1
