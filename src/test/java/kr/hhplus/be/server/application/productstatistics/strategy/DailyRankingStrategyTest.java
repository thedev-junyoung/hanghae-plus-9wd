package kr.hhplus.be.server.application.productstatistics.strategy;

import kr.hhplus.be.server.infrastructure.redis.ProductRankRedisRepository;
import kr.hhplus.be.server.infrastructure.redis.RedisScriptExecutor;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Duration;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class DailyRankingStrategyTest {

    @Mock
    RedisScriptExecutor redisScriptExecutor;

    @Mock
    ProductRankRedisRepository redisRepository;

    @Captor
    ArgumentCaptor<String> scriptCaptor;

    @Captor
    ArgumentCaptor<List<String>> keysCaptor;

    @Captor
    ArgumentCaptor<List<String>> argsCaptor;

    @Captor
    ArgumentCaptor<Class<Long>> resultTypeCaptor;

    @Test
    @DisplayName("record 호출 시 RedisScriptExecutor.execute가 정확한 인자로 호출된다")
    void record_should_call_scriptExecutor_execute() {
        DailyRankingStrategy strategy = new DailyRankingStrategy(redisRepository, redisScriptExecutor);
        Long productId = 1L;
        int quantity = 3;

        strategy.record(productId, quantity);

        verify(redisScriptExecutor).execute(
                scriptCaptor.capture(),
                keysCaptor.capture(),
                argsCaptor.capture(),
                resultTypeCaptor.capture()
        );

        assertThat(scriptCaptor.getValue()).contains("ZINCRBY");
        assertThat(keysCaptor.getValue().get(0)).startsWith("ranking:daily:");
        assertThat(argsCaptor.getValue()).containsExactly(
                "3.0", "1", String.valueOf(Duration.ofDays(3).getSeconds())
        );
        assertThat(resultTypeCaptor.getValue()).isEqualTo(Long.class);
    }

    @Test
    @DisplayName("getPrefix와 getExpireDuration 값이 정확해야 한다")
    void verify_prefix_and_expire_duration() {
        DailyRankingStrategy strategy = new DailyRankingStrategy(redisRepository, redisScriptExecutor);
        assertThat(strategy.getPrefix()).isEqualTo("ranking:daily:");
        assertThat(strategy.getExpireDuration()).isEqualTo(Duration.ofDays(3));
    }
}
