//package kr.hhplus.be.server.integration.coupon;
//
//import kr.hhplus.be.server.application.coupon.CouponAsyncIssueService;
//import kr.hhplus.be.server.application.coupon.CouponUseCase;
//import kr.hhplus.be.server.application.coupon.IssueLimitedCouponCommand;
//import kr.hhplus.be.server.domain.coupon.Coupon;
//import kr.hhplus.be.server.domain.coupon.CouponRepository;
//import kr.hhplus.be.server.domain.coupon.CouponIssueRepository;
//import kr.hhplus.be.server.infrastructure.redis.CouponDLQConsumer;
//import kr.hhplus.be.server.infrastructure.redis.CouponIssueConsumer;
//import kr.hhplus.be.server.infrastructure.redis.util.CouponStreamKeyResolver;
//import lombok.extern.slf4j.Slf4j;
//import org.awaitility.Awaitility;
//import org.junit.jupiter.api.BeforeEach;
//import org.junit.jupiter.api.DisplayName;
//import org.junit.jupiter.api.Test;
//import org.springframework.beans.factory.annotation.Autowired;
//import org.springframework.boot.test.context.SpringBootTest;
//import org.springframework.data.redis.connection.stream.MapRecord;
//import org.springframework.data.redis.connection.stream.StreamOffset;
//import org.springframework.data.redis.core.StringRedisTemplate;
//import org.springframework.kafka.test.context.EmbeddedKafka;
//import org.springframework.test.context.ActiveProfiles;
//
//import java.time.Clock;
//import java.time.Duration;
//import java.time.LocalDateTime;
//import java.util.List;
//import java.util.UUID;
//
//import static org.assertj.core.api.Assertions.assertThat;
//
//@Slf4j
//@SpringBootTest
////@EmbeddedKafka(
////        partitions = 1,
////        topics = {"coupon.issue", "coupon.issue.DLT"},
////        brokerProperties = {"listeners=PLAINTEXT://localhost:0"}
////)
//@ActiveProfiles("test")
//class CouponIntegrationTest {
//
//    private String testCouponCode;
//
//    @Autowired
//    private CouponUseCase couponService;
//
//    @Autowired
//    private CouponRepository couponRepository;
//
//    @Autowired
//    private CouponIssueRepository couponIssueRepository;
//
//    @Autowired
//    private CouponIssueConsumer consumer;
//
//    @Autowired
//    private Clock clock;
//
//    @Autowired
//    private StringRedisTemplate redisTemplate;
//
//    @Autowired
//    private CouponAsyncIssueService issueService;
//
//
//    @BeforeEach
//    void setup() {
//        String code = "DYNAMIC-" + UUID.randomUUID();
//        Coupon newCoupon = Coupon.createLimitedFixed(
//                code, 1000, 10,
//                LocalDateTime.now(clock).minusDays(1),
//                LocalDateTime.now(clock).plusDays(1)
//        );
//        couponRepository.save(newCoupon);
//        testCouponCode = code;
//    }
//
//    @Test
//    @DisplayName("쿠폰 발급 성공 시 남은 수량이 감소하고 발급 기록이 저장된다")
//    void issueCoupon_successFlow() {
//        // given
//        IssueLimitedCouponCommand command = new IssueLimitedCouponCommand(1L, testCouponCode, "req-1");
//
//        // when
//        consumer.consumeSingle(testCouponCode); // Stream/Group 선 생성
//        couponService.enqueueLimitedCoupon(command);
//
//        Awaitility.await().atMost(Duration.ofSeconds(3)).until(() ->
//                redisTemplate.hasKey(CouponStreamKeyResolver.resolve(testCouponCode)) == Boolean.TRUE
//        );
//
//        consumer.consumeSingle(testCouponCode);
//
//        // then
//        Awaitility.await()
//                .atMost(Duration.ofSeconds(5))
//                .pollInterval(Duration.ofMillis(200))
//                .untilAsserted(() -> {
//                    Coupon coupon = couponRepository.findByCode(testCouponCode).orElseThrow();
//                    log.info("남은 수량: {}", coupon.getRemainingQuantity());
//                    assertThat(couponIssueRepository.hasIssued(1L, coupon.getId())).isTrue();
//                });
//    }
//
//    @Test
//    @DisplayName("쿠폰 발급 실패 시 DLQ로 이동한다")
//    void failedCouponIssue_shouldMoveToDLQ() {
//        String code = "EXPIRED-" + UUID.randomUUID();
//        Coupon expired = Coupon.createLimitedFixed(
//                code, 1000, 10,
//                LocalDateTime.now(clock).minusDays(10),
//                LocalDateTime.now(clock).minusDays(1)
//        );
//        couponRepository.save(expired);
//
//        IssueLimitedCouponCommand command = new IssueLimitedCouponCommand(2L, code, "req-2");
//        couponService.enqueueLimitedCoupon(command);
//
//        // 정상 스트림 소비 시도 → DLQ로 이동
//        consumer.consumeSingle(code);
//
//        // DLQ 컨슈머 직접 실행 (보통 @Scheduled로 돌지만 테스트에선 직접 호출)
//        CouponDLQConsumer dlqConsumer = new CouponDLQConsumer(redisTemplate, issueService, couponService); // or @Autowired
//        dlqConsumer.consumeDLQ();
//
//        // Await DLQ Stream populated
//        Awaitility.await().atMost(Duration.ofSeconds(3)).untilAsserted(() -> {
//            List<MapRecord<String, Object, Object>> records =
//                    redisTemplate.opsForStream().read(StreamOffset.fromStart(CouponStreamKeyResolver.dlq(code)));
//            assertThat(records).isNotEmpty();
//        });
//    }
//
//
//}
