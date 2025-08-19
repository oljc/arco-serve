package io.github.oljc.arcoserve.shared.util;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.data.redis.core.script.RedisScript;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.*;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;

/**
 * 简化优化的 Redis 工具类
 */
@Component
@RequiredArgsConstructor
public final class RedisUtils {

    private final StringRedisTemplate redis;

    private static final RedisScript<List> SLIDING_WINDOW_SCRIPT = new DefaultRedisScript<>("""
        local key, window, limit, now = KEYS[1], tonumber(ARGV[1]), tonumber(ARGV[2]), tonumber(ARGV[3])
        local clearBefore = now - window * 1000

        redis.call('ZREMRANGEBYSCORE', key, 0, clearBefore)
        local current = redis.call('ZCARD', key)

        if current < limit then
            redis.call('ZADD', key, now, now)
            redis.call('EXPIRE', key, window + 1)
            return {1, limit - current - 1}
        end
        return {0, 0}
        """, List.class);

    private static final RedisScript<List> TOKEN_BUCKET_SCRIPT = new DefaultRedisScript<>("""
        local key, rate, capacity, now = KEYS[1], tonumber(ARGV[1]), tonumber(ARGV[2]), tonumber(ARGV[3])
        local bucket = redis.call('HMGET', key, 'tokens', 'last_refill')
        local tokens = tonumber(bucket[1]) or capacity
        local lastRefill = tonumber(bucket[2]) or now

        local elapsed = math.max(0, now - lastRefill)
        tokens = math.min(capacity, tokens + (elapsed * rate / 1000))

        if tokens >= 1 then
            tokens = tokens - 1
            redis.call('HMSET', key, 'tokens', tokens, 'last_refill', now)
            redis.call('EXPIRE', key, math.ceil(capacity / rate) + 10)
            return {1, math.floor(tokens)}
        else
            redis.call('HMSET', key, 'tokens', tokens, 'last_refill', now)
            redis.call('EXPIRE', key, math.ceil(capacity / rate) + 10)
            return {0, 0}
        end
        """, List.class);

    private static final RedisScript<Long> ACQUIRE_LOCK_SCRIPT = new DefaultRedisScript<>("""
        local key, identifier, ttl = KEYS[1], ARGV[1], tonumber(ARGV[2])
        local current = redis.call('HGET', key, 'owner')

        if current == false then
            redis.call('HMSET', key, 'owner', identifier, 'count', 1)
            redis.call('EXPIRE', key, ttl)
            return 1
        elseif current == identifier then
            local count = redis.call('HINCRBY', key, 'count', 1)
            redis.call('EXPIRE', key, ttl)
            return count
        else
            return 0
        end
        """, Long.class);

    private static final RedisScript<Long> RELEASE_LOCK_SCRIPT = new DefaultRedisScript<>("""
        local key, identifier = KEYS[1], ARGV[1]
        local current = redis.call('HGET', key, 'owner')

        if current == identifier then
            local count = redis.call('HINCRBY', key, 'count', -1)
            if count <= 0 then
                redis.call('DEL', key)
                return 1
            else
                return count
            end
        end
        return -1
        """, Long.class);

    private static final RedisScript<List> ATOMIC_COUNTER_SCRIPT = new DefaultRedisScript<>("""
        local key, delta, ttl, max_val = KEYS[1], tonumber(ARGV[1]), tonumber(ARGV[2]), tonumber(ARGV[3])
        local current = tonumber(redis.call('GET', key)) or 0
        local new_val = current + delta

        if max_val > 0 and new_val > max_val then
            return {0, current}
        end

        redis.call('SET', key, new_val, 'EX', ttl)
        return {1, new_val}
        """, List.class);

    private static final RedisScript<List> DELAY_QUEUE_BATCH_POP_SCRIPT = new DefaultRedisScript<>("""
        local queue_key, now, limit = KEYS[1], tonumber(ARGV[1]), tonumber(ARGV[2])
        local items = redis.call('ZRANGEBYSCORE', queue_key, 0, now, 'LIMIT', 0, limit)

        if #items > 0 then
            redis.call('ZREM', queue_key, unpack(items))
            return items
        end
        return {}
        """, List.class);

    private static final RedisScript<Long> BLOOM_FILTER_SCRIPT = new DefaultRedisScript<>("""
        local key, item, hash_count, bit_size = KEYS[1], ARGV[1], tonumber(ARGV[2]), tonumber(ARGV[3])
        local exists = true

        for i = 1, hash_count do
            local hash = redis.call('EVAL', 'return redis.sha1hex(ARGV[1] .. ARGV[2])', 0, item, i)
            local pos = tonumber(hash:sub(1, 8), 16) % bit_size
            if redis.call('GETBIT', key, pos) == 0 then
                exists = false
            end
            redis.call('SETBIT', key, pos, 1)
        end
        return exists and 1 or 0
        """, Long.class);

    private static final RedisScript<Long> CACHE_BREAKTHROUGH_GUARD_SCRIPT = new DefaultRedisScript<>("""
        local cache_key, lock_key, value, ttl, lock_ttl = KEYS[1], KEYS[2], ARGV[1], tonumber(ARGV[2]), tonumber(ARGV[3])

        if redis.call('SET', lock_key, '1', 'NX', 'EX', lock_ttl) then
            redis.call('SET', cache_key, value, 'EX', ttl)
            redis.call('DEL', lock_key)
            return 1
        else
            return redis.call('EXISTS', cache_key)
        end
        """, Long.class);


    public record RateLimitResult(boolean allowed, long remaining) {}
    public record CounterResult(boolean success, long value) {}

    // ===== 基础操作 =====

    public void set(String key, String value, long seconds) {
        redis.opsForValue().set(key, value, Duration.ofSeconds(seconds));
    }

    public void set(String key, String value) {
        redis.opsForValue().set(key, value);
    }

    public String get(String key) {
        return redis.opsForValue().get(key);
    }

    public boolean setNx(String key, String value, long seconds) {
        return Boolean.TRUE.equals(redis.opsForValue().setIfAbsent(key, value, Duration.ofSeconds(seconds)));
    }

    public void mset(Map<String, String> kvMap) {
        if (!kvMap.isEmpty()) {
            redis.opsForValue().multiSet(kvMap);
        }
    }

    public Map<String, String> mget(Collection<String> keys) {
        if (keys.isEmpty()) return Collections.emptyMap();

        List<String> keyList = new ArrayList<>(keys);
        List<String> values = redis.opsForValue().multiGet(keyList);

        Map<String, String> result = new HashMap<>(keys.size());
        for (int i = 0; i < keyList.size(); i++) {
            String value = values != null && i < values.size() ? values.get(i) : null;
            if (value != null) {
                result.put(keyList.get(i), value);
            }
        }
        return result;
    }

    public long incr(String key) {
        return redis.opsForValue().increment(key);
    }

    public long incrBy(String key, long delta) {
        return redis.opsForValue().increment(key, delta);
    }

    public long decr(String key) {
        return redis.opsForValue().decrement(key);
    }

    // ===== Hash 操作 =====

    public void hset(String key, String field, String value) {
        redis.opsForHash().put(key, field, value);
    }

    public void hmset(String key, Map<String, String> hash) {
        redis.opsForHash().putAll(key, hash);
    }

    public String hget(String key, String field) {
        Object value = redis.opsForHash().get(key, field);
        return value != null ? value.toString() : null;
    }

    public Map<Object, Object> hgetall(String key) {
        return redis.opsForHash().entries(key);
    }

    public long hincr(String key, String field, long delta) {
        return redis.opsForHash().increment(key, field, delta);
    }

    public long hdel(String key, String... fields) {
        return redis.opsForHash().delete(key, (Object[]) fields);
    }

    // ===== Set 操作 =====

    public long sadd(String key, String... members) {
        return redis.opsForSet().add(key, members);
    }

    public Set<String> smembers(String key) {
        return redis.opsForSet().members(key);
    }

    public boolean sismember(String key, String member) {
        return Boolean.TRUE.equals(redis.opsForSet().isMember(key, member));
    }

    public long srem(String key, String... members) {
        return redis.opsForSet().remove(key, (Object[]) members);
    }

    public long scard(String key) {
        return redis.opsForSet().size(key);
    }

    // ===== ZSet 操作 =====

    public boolean zadd(String key, String member, double score) {
        return Boolean.TRUE.equals(redis.opsForZSet().add(key, member, score));
    }

    public Set<String> zrange(String key, long start, long end) {
        return redis.opsForZSet().range(key, start, end);
    }

    public Set<String> zrangeByScore(String key, double min, double max, long offset, long count) {
        return redis.opsForZSet().rangeByScore(key, min, max, offset, count);
    }

    public long zrem(String key, String... members) {
        return redis.opsForZSet().remove(key, (Object[]) members);
    }

    public long zcard(String key) {
        return redis.opsForZSet().zCard(key);
    }

    // ===== List 操作 =====

    public long lpush(String key, String... values) {
        return redis.opsForList().leftPushAll(key, values);
    }

    public long rpush(String key, String... values) {
        return redis.opsForList().rightPushAll(key, values);
    }

    public String lpop(String key) {
        return redis.opsForList().leftPop(key);
    }

    public String rpop(String key) {
        return redis.opsForList().rightPop(key);
    }

    public List<String> lrange(String key, long start, long end) {
        return redis.opsForList().range(key, start, end);
    }

    // ===== 高级功能 =====

    /**
     * 滑动窗口限流
     * @param key 限流键
     * @param windowSeconds 窗口大小（秒）
     * @param limit 限制次数
     * @return 限流结果
     */
    public RateLimitResult slidingWindowLimit(String key, int windowSeconds, int limit) {
        List<Long> result = redis.execute(SLIDING_WINDOW_SCRIPT,
            List.of(key),
            String.valueOf(windowSeconds),
            String.valueOf(limit),
            String.valueOf(System.currentTimeMillis()));

        if (result != null && result.size() >= 2) {
            boolean allowed = result.get(0) == 1;
            long remaining = result.get(1);
            return new RateLimitResult(allowed, remaining);
        }
        return new RateLimitResult(false, 0);
    }

    /**
     * 令牌桶限流
     * @param key 限流键
     * @param rate 补充速率（tokens/second）
     * @param capacity 桶容量
     * @return 限流结果
     */
    public RateLimitResult tokenBucketLimit(String key, double rate, int capacity) {
        List<Long> result = redis.execute(TOKEN_BUCKET_SCRIPT,
            List.of(key),
            String.valueOf(rate),
            String.valueOf(capacity),
            String.valueOf(System.currentTimeMillis()));

        if (result != null && result.size() >= 2) {
            boolean allowed = result.get(0) == 1;
            long remaining = result.get(1);
            return new RateLimitResult(allowed, remaining);
        }
        return new RateLimitResult(false, 0);
    }

    /**
     * 获取可重入分布式锁
     * @param lockKey 锁键
     * @param identifier 锁标识
     * @param ttlSeconds 过期时间
     * @return 获取次数，0表示失败
     */
    public int acquireLock(String lockKey, String identifier, int ttlSeconds) {
        Long result = redis.execute(ACQUIRE_LOCK_SCRIPT,
            List.of(lockKey),
            identifier,
            String.valueOf(ttlSeconds));
        return result != null ? result.intValue() : 0;
    }

    /**
     * 释放分布式锁
     * @param lockKey 锁键
     * @param identifier 锁标识
     * @return 剩余计数，-1表示锁不属于当前持有者
     */
    public int releaseLock(String lockKey, String identifier) {
        Long result = redis.execute(RELEASE_LOCK_SCRIPT,
            List.of(lockKey),
            identifier);
        return result != null ? result.intValue() : -1;
    }

    /**
     * 原子计数器
     * @param key 计数器键
     * @param delta 增量
     * @param ttlSeconds 过期时间
     * @param maxValue 最大值限制，0表示无限制
     * @return 计数结果
     */
    public CounterResult atomicCounter(String key, long delta, int ttlSeconds, long maxValue) {
        List<Long> result = redis.execute(ATOMIC_COUNTER_SCRIPT,
            List.of(key),
            String.valueOf(delta),
            String.valueOf(ttlSeconds),
            String.valueOf(maxValue));

        if (result != null && result.size() >= 2) {
            return new CounterResult(
                result.get(0) == 1,
                result.get(1)
            );
        }
        return new CounterResult(false, 0);
    }

    /**
     * 添加延时任务
     */
    public void delayQueueAdd(String queueKey, String item, long delayMs) {
        long executeTime = System.currentTimeMillis() + delayMs;
        redis.opsForZSet().add(queueKey, item, executeTime);
    }

    /**
     * 批量获取到期任务
     */
    public List<String> delayQueueBatchPop(String queueKey, int limit) {
        List<String> result = redis.execute(DELAY_QUEUE_BATCH_POP_SCRIPT,
            List.of(queueKey),
            String.valueOf(System.currentTimeMillis()),
            String.valueOf(limit));

        return result != null ? result : Collections.emptyList();
    }

    /**
     * 布隆过滤器检查并添加
     * @param key 过滤器键
     * @param item 检查项
     * @param hashCount 哈希函数数量
     * @param bitSize 位图大小
     * @return true表示可能存在，false表示一定不存在
     */
    public boolean bloomFilterCheckAndAdd(String key, String item, int hashCount, int bitSize) {
        Long result = redis.execute(BLOOM_FILTER_SCRIPT,
            List.of(key),
            item,
            String.valueOf(hashCount),
            String.valueOf(bitSize));
        return result != null && result == 1L;
    }

    /**
     * 缓存击穿防护
     */
    public boolean cacheBreakthroughGuard(String cacheKey, String lockKey, String value, int cacheTtl, int lockTtl) {
        Long result = redis.execute(CACHE_BREAKTHROUGH_GUARD_SCRIPT,
            List.of(cacheKey, lockKey),
            value,
            String.valueOf(cacheTtl),
            String.valueOf(lockTtl));
        return result != null && result > 0;
    }

    /**
     * 带随机过期时间的缓存设置（防雪崩）
     */
    public void setWithRandomExpire(String key, String value, int baseTtl, int randomRange) {
        int randomTtl = baseTtl + ThreadLocalRandom.current().nextInt(randomRange);
        set(key, value, randomTtl);
    }

    /**
     * 是否存在
     */
    public boolean exists(String key) {
        return Boolean.TRUE.equals(redis.hasKey(key));
    }

    /**
     * 删除
     */
    public long delete(String... keys) {
        if (keys.length == 0) return 0;
        Long result = redis.delete(Arrays.asList(keys));
        return result != null ? result : 0;
    }

    /**
     * 设置过期时间
     */
    public boolean expire(String key, long seconds) {
        return Boolean.TRUE.equals(redis.expire(key, seconds, TimeUnit.SECONDS));
    }

    /**
     * 获取过期时间
     */
    public long ttl(String key) {
        Long ttl = redis.getExpire(key, TimeUnit.SECONDS);
        return ttl != null ? ttl : -1;
    }
}
