package kr.hhplus.be.server.application.productstatistics.strategy;

import kr.hhplus.be.server.infrastructure.redis.ProductRankRedisRepository;
import kr.hhplus.be.server.infrastructure.redis.RedisScriptExecutor;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;

public abstract class AbstractRankingStrategy implements ProductRankingStrategy {

    protected final ProductRankRedisRepository redis;
    protected final RedisScriptExecutor scriptExecutor;

    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyyMMdd");

    private static final String LUA_INCREMENT_AND_EXPIRE = """
        local exists = redis.call('EXISTS', KEYS[1])
        redis.call('ZINCRBY', KEYS[1], ARGV[1], ARGV[2])
        if exists == 0 then
            redis.call('EXPIRE', KEYS[1], ARGV[3])
        end
        return 1
    """;

    private static final String LUA_DECREMENT = """
        redis.call('ZINCRBY', KEYS[1], -ARGV[1], ARGV[2])
        return 1
    """;

    protected AbstractRankingStrategy(ProductRankRedisRepository redis, RedisScriptExecutor scriptExecutor) {
        this.redis = redis;
        this.scriptExecutor = scriptExecutor;
    }

    @Override
    public void record(Long productId, int quantity) {
        String key = getRankingKey();
        scriptExecutor.execute(
                LUA_INCREMENT_AND_EXPIRE,
                List.of(key),
                List.of(
                        Double.toString((double) quantity),
                        productId.toString(),
                        String.valueOf(getExpireDuration().getSeconds())
                ),
                Long.class
        );
    }

    @Override
    public void undo(Long productId, int quantity) {
        String key = getRankingKey();
        scriptExecutor.execute(
                LUA_DECREMENT,
                List.of(key),
                List.of(
                        Double.toString((double) quantity),
                        productId.toString()
                ),
                Long.class
        );
    }

    private String getRankingKey() {
        return getPrefix() + LocalDate.now().format(FORMATTER);
    }
}
