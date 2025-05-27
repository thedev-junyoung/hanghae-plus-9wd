package kr.hhplus.be.server.application.productstatistics.strategy;

import kr.hhplus.be.server.infrastructure.redis.ProductRankRedisRepository;
import kr.hhplus.be.server.infrastructure.redis.RedisScriptExecutor;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;


@Component
@Qualifier("daily")
public class DailyRankingStrategy extends AbstractRankingStrategy {


    public DailyRankingStrategy(ProductRankRedisRepository redisRepository, RedisScriptExecutor redisScriptExecutor) {
        super(redisRepository, redisScriptExecutor);
    }

    @Override
    public String getPrefix() {
        return "ranking:daily:";
    }

    @Override
    public Duration getExpireDuration() {
        return Duration.ofDays(3);
    }

}
