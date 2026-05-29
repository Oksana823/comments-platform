---@diagnostic disable: undefined-global

-- 1.参数列表
local voucherId = ARGV[1]
local userId = ARGV[2]
local orderId = ARGV[3]

-- 2.数据key
local stockKey = 'seckill:stock:' .. voucherId
local orderKey = 'seckill:order:' .. voucherId

-- 3.脚本业务
-- 3.1 判断库存是否充足
if tonumber(redis.call('GET', stockKey)) <= 0 then
    return 1
end

-- 3.2 判断用户是否已经下过单
if redis.call('SISMEMBER', orderKey, userId) == 1 then
    return 2
end

-- 3.3 扣减库存
redis.call('INCRBY', stockKey, -1)

-- 3.4 记录用户已下单
redis.call('SADD', orderKey, userId)

--redis stream 方法
--redis.call('XADD', 'stream.orders', '*',
--           'userId', userId,
--           'voucherId', voucherId,
--           'id', orderId)

return 0