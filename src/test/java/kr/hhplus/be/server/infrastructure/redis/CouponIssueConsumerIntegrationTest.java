package kr.hhplus.be.server.infrastructure.redis;

import kr.hhplus.be.server.application.coupon.CouponUseCase;
import kr.hhplus.be.server.application.coupon.IssueLimitedCouponCommand;
import kr.hhplus.be.server.domain.coupon.Coupon;
import kr.hhplus.be.server.domain.coupon.CouponIssueRepository;
import kr.hhplus.be.server.domain.coupon.CouponRepository;
import kr.hhplus.be.server.infrastructure.redis.util.CouponStreamKeyResolver;
import org.awaitility.Awaitility;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.RedisSystemException;
import org.springframework.data.redis.connection.stream.MapRecord;
import org.springframework.data.redis.connection.stream.StreamOffset;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.kafka.test.context.EmbeddedKafka;
import org.springframework.test.context.ActiveProfiles;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
class CouponIssueConsumerIntegrationTest {

    @Autowired
    private CouponRepository couponRepository;

    @Autowired
    private CouponIssueRepository couponIssueRepository;

    @Autowired
    private CouponUseCase couponUseCase;

    @Autowired
    private CouponIssueConsumer consumer;

    @Autowired
    private CouponIssueStreamPublisher streamPublisher;

    @Autowired
    private StringRedisTemplate redisTemplate;


    private final Long userId = 123L;
    private final String requestId = UUID.randomUUID().toString();
    private String couponCode;

    @BeforeEach
    void setUp() {
        couponCode = "TEST_" + UUID.randomUUID().toString().substring(0, 8); // 중복 방지

        Coupon coupon = couponRepository.findByCode(couponCode)
                .orElse(null);
        if (coupon == null) {
            coupon = Coupon.createLimitedFixed(
                    couponCode, 1000, 100,
                    LocalDateTime.now().minusDays(1),
                    LocalDateTime.now().plusDays(1)
            );
            couponRepository.save(coupon);
        }

    }

    @Test
    @DisplayName("정상적으로 쿠폰 발급됨")
    void coupon_should_be_issued_via_consumer() {
        String streamKey = CouponStreamKeyResolver.resolve(couponCode);

        // Stream + Group 보장
        if (!Boolean.TRUE.equals(redisTemplate.hasKey(streamKey))) {
            redisTemplate.opsForStream().add(streamKey, Map.of("init", "init"));
        }

        try {
            redisTemplate.opsForStream().createGroup(streamKey, "coupon-consumer-group");
        } catch (RedisSystemException e) {
            // BUSYGROUP 예외 무시
        }

        // 메시지 발행
        streamPublisher.publish(new IssueLimitedCouponCommand(userId, couponCode, requestId));

        // 레코드가 들어왔는지 확인
        Awaitility.await()
                .atMost(Duration.ofSeconds(2))
                .pollInterval(Duration.ofMillis(200))
                .untilAsserted(() -> {
                    List<MapRecord<String, Object, Object>> records =
                            redisTemplate.opsForStream().read(StreamOffset.fromStart(streamKey));
                    assertThat(records).isNotEmpty();
                });

        // consumer 수동 트리거
        consumer.consume();

        Long couponId = couponRepository.findByCode(couponCode).orElseThrow().getId();

        // 발급 여부 확인
        Awaitility.await()
                .atMost(Duration.ofSeconds(3))
                .pollInterval(Duration.ofMillis(200))
                .until(() -> couponIssueRepository.hasIssued(userId, couponId));
    }

}

