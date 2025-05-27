package kr.hhplus.be.server.application.productstatistics.strategy;

import kr.hhplus.be.server.infrastructure.redis.ProductRankRedisRepository;
import kr.hhplus.be.server.infrastructure.redis.RedisScriptExecutor;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import java.time.Duration;


@Component
@Qualifier("weekly")
public class WeeklyRankingStrategy extends AbstractRankingStrategy {
    public WeeklyRankingStrategy(ProductRankRedisRepository redisRepository, RedisScriptExecutor redisScriptExecutor) {
        super(redisRepository, redisScriptExecutor);
    }
    @Override
    public String getPrefix() {
        return "ranking:weekly:";
    }

    @Override
    public Duration getExpireDuration() {
        return Duration.ofDays(8);
    }
}

