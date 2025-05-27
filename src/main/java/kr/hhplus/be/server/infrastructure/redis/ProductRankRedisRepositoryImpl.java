package kr.hhplus.be.server.infrastructure.redis;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ZSetOperations;
import org.springframework.stereotype.Repository;

import java.time.Duration;
import java.util.List;
import java.util.stream.Collectors;

@Repository
@RequiredArgsConstructor
public class ProductRankRedisRepositoryImpl implements ProductRankRedisRepository {

    private final StringRedisTemplate redisTemplate;

    @Override
    public void incrementScore(String key, Long productId, double score) {
        redisTemplate.opsForZSet().incrementScore(key, productId.toString(), score);
    }

    @Override
    public List<Long> getTopN(String key, int limit) {
        ZSetOperations<String, String> ops = redisTemplate.opsForZSet();
        return ops.reverseRange(key, 0, limit - 1)
                .stream()
                .map(Long::valueOf)
                .collect(Collectors.toList());
    }

    @Override
    public void remove(String key, Long productId) {
        redisTemplate.opsForZSet().remove(key, productId.toString());
    }

    @Override
    public void clear(String key) {
        redisTemplate.delete(key);
    }

    @Override
    public Double getScore(String key, Long productId) {
        return redisTemplate.opsForZSet().score(key, productId.toString());
    }

    // Redis 키 존재 여부와 TTL 설정
    @Override
    public void expireIfAbsent(String key, Duration ttl) {
        Boolean exists = redisTemplate.hasKey(key);
        if (Boolean.TRUE.equals(exists)) return;
        redisTemplate.expire(key, ttl);
    }

}
