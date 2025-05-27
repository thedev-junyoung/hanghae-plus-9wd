package kr.hhplus.be.server.infrastructure.redis;

import kr.hhplus.be.server.application.coupon.IssueLimitedCouponCommand;

import kr.hhplus.be.server.infrastructure.redis.util.CouponStreamKeyResolver;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.Range;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.test.context.ActiveProfiles;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
class CouponIssueStreamPublisherIntegrationTest {

    @Autowired
    private CouponIssueStreamPublisher publisher;

    @Autowired
    private StringRedisTemplate redisTemplate;

    private final String couponCode = "REDIS_TEST_COUPON";

    @Test
    @DisplayName("Redis Stream에 쿠폰 발급 메시지를 실제로 저장한다")
    void publishCommand_shouldWriteToRedisStream() {
        // given
        String streamKey = CouponStreamKeyResolver.resolve(couponCode);
        String userId = "100";
        String requestId = "req-redis-stream-test";

        // when
        publisher.publish(new IssueLimitedCouponCommand(Long.valueOf(userId), couponCode, requestId));

        // then
        var records = redisTemplate.opsForStream().range(streamKey, Range.unbounded());

        assertThat(records).isNotEmpty();

        Map<Object, Object> message = records.get(records.size() - 1).getValue();
        assertThat(message).containsEntry("userId", userId);
        assertThat(message).containsEntry("couponCode", couponCode);
        assertThat(message).containsEntry("requestId", requestId);
    }
}

