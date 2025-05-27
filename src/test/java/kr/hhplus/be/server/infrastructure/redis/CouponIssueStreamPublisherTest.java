package kr.hhplus.be.server.infrastructure.redis;

import kr.hhplus.be.server.application.coupon.IssueLimitedCouponCommand;
import kr.hhplus.be.server.infrastructure.redis.util.CouponStreamKeyResolver;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.StreamOperations;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

class CouponIssueStreamPublisherTest {

    private final StringRedisTemplate redisTemplate = mock(StringRedisTemplate.class);
    private final StreamOperations<String, Object, Object> streamOps = mock(StreamOperations.class);

    private final CouponIssueStreamPublisher publisher = new CouponIssueStreamPublisher(redisTemplate);

    @Test
    @DisplayName("쿠폰 발급 명령을 Redis Stream에 발행한다")
    void publishToRedisStream() {
        // given
        IssueLimitedCouponCommand command = new IssueLimitedCouponCommand(1L, "WELCOME10", "req-123");

        when(redisTemplate.opsForStream()).thenReturn(streamOps);

        // when
        publisher.publish(command);

        // then
        ArgumentCaptor<Map<String, String>> captor = ArgumentCaptor.forClass(Map.class);
        verify(streamOps).add(eq(CouponStreamKeyResolver.resolve("WELCOME10")), captor.capture());

        Map<String, String> payload = captor.getValue();
        assertThat(payload)
                .containsEntry("userId", "1")
                .containsEntry("couponCode", "WELCOME10")
                .containsEntry("requestId", "req-123");
    }
}
