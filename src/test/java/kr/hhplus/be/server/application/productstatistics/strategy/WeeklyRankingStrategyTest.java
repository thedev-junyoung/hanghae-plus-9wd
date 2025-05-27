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
class WeeklyRankingStrategyTest {

    @Mock
    RedisScriptExecutor scriptExecutor;

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
        // given
        WeeklyRankingStrategy strategy = new WeeklyRankingStrategy(redisRepository, scriptExecutor);
        Long productId = 100L;
        int quantity = 10;

        // when
        strategy.record(productId, quantity);

        // then
        verify(scriptExecutor).execute(
                scriptCaptor.capture(),
                keysCaptor.capture(),
                argsCaptor.capture(),
                resultTypeCaptor.capture()
        );

        assertThat(scriptCaptor.getValue()).contains("ZINCRBY"); // Lua 스크립트가 맞는지
        assertThat(keysCaptor.getValue().get(0)).startsWith("ranking:weekly:");
        assertThat(argsCaptor.getValue()).containsExactly(
                "10.0", // quantity
                "100",  // productId
                String.valueOf(Duration.ofDays(8).getSeconds()) // TTL
        );
        assertThat(resultTypeCaptor.getValue()).isEqualTo(Long.class);
    }

    @Test
    @DisplayName("getPrefix와 getExpireDuration 값이 정확해야 한다")
    void verify_prefix_and_expire_duration() {
        WeeklyRankingStrategy strategy = new WeeklyRankingStrategy(redisRepository, scriptExecutor);

        assertThat(strategy.getPrefix()).isEqualTo("ranking:weekly:");
        assertThat(strategy.getExpireDuration()).isEqualTo(Duration.ofDays(8));
    }
}
