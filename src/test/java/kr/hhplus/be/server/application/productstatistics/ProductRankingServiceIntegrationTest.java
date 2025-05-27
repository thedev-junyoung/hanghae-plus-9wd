package kr.hhplus.be.server.application.productstatistics;

import kr.hhplus.be.server.application.productstatistics.strategy.DailyRankingStrategy;
import kr.hhplus.be.server.application.productstatistics.strategy.MonthlyRankingStrategy;
import kr.hhplus.be.server.application.productstatistics.strategy.WeeklyRankingStrategy;
import kr.hhplus.be.server.infrastructure.redis.ProductRankRedisRepository;
import kr.hhplus.be.server.infrastructure.redis.RedisScriptExecutor;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.data.redis.core.RedisTemplate;

import static org.mockito.Mockito.*;

@SpringBootTest
class ProductRankingServiceIntegrationTest {


    @TestConfiguration
    static class TestConfig {

        @Bean
        @Primary
        public ProductRankRedisRepository productRankRedisRepository() {
            return mock(ProductRankRedisRepository.class);
        }

        @Bean
        public DailyRankingStrategy daily(ProductRankRedisRepository redis, RedisScriptExecutor redisScriptExecutor) {
            return spy(new DailyRankingStrategy(redis, redisScriptExecutor));
        }

        @Bean
        public WeeklyRankingStrategy weekly(ProductRankRedisRepository redis, RedisScriptExecutor redisScriptExecutor) {
            return spy(new WeeklyRankingStrategy(redis, redisScriptExecutor));
        }

        @Bean
        public MonthlyRankingStrategy monthly(ProductRankRedisRepository redis, RedisScriptExecutor redisScriptExecutor) {
            return spy(new MonthlyRankingStrategy(redis, redisScriptExecutor));
        }
    }


    @Autowired
    ProductRankingService rankingService;

    @Autowired
    DailyRankingStrategy daily;

    @Autowired
    WeeklyRankingStrategy weekly;

    @Autowired
    MonthlyRankingStrategy monthly;

    @Test
    @DisplayName("모든 전략에 대해 record 메소드가 호출된다.")
    void record_should_delegate_to_all_strategies() {
        // given
        Long productId = 1L;
        int quantity = 10;

        // when
        rankingService.record(productId, quantity);

        // then
        verify(daily).record(productId, quantity);
        verify(weekly).record(productId, quantity);
        verify(monthly).record(productId, quantity);
    }
}