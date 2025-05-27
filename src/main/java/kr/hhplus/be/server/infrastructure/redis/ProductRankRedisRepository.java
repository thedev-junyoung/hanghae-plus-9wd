package kr.hhplus.be.server.infrastructure.redis;

import java.time.Duration;
import java.util.List;

public interface ProductRankRedisRepository {
    void incrementScore(String key, Long productId, double score);
    List<Long> getTopN(String key, int limit);
    void remove(String key, Long productId);
    void clear(String key);

    Double getScore(String key, Long productId);
    void expireIfAbsent(String key, Duration ttl);
}
