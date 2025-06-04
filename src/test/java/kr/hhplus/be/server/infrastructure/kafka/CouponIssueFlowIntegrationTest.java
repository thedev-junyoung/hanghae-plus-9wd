package kr.hhplus.be.server.infrastructure.kafka;

import com.fasterxml.jackson.databind.ObjectMapper;
import kr.hhplus.be.server.application.coupon.CouponUseCase;
import kr.hhplus.be.server.application.coupon.IssueLimitedCouponCommand;
import kr.hhplus.be.server.domain.coupon.Coupon;
import kr.hhplus.be.server.domain.coupon.CouponIssueRepository;
import kr.hhplus.be.server.domain.coupon.CouponRepository;
import kr.hhplus.be.server.domain.outbox.OutboxMessage;
import kr.hhplus.be.server.domain.outbox.OutboxRepository;
import kr.hhplus.be.server.infrastructure.outbox.OutboxRelayScheduler;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;


import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.kafka.annotation.EnableKafka;
import org.springframework.kafka.test.EmbeddedKafkaBroker;
import org.springframework.kafka.test.context.EmbeddedKafka;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@EmbeddedKafka(
        partitions = 1,
        topics = {"coupon.issue", "coupon.issue.DLT"},
        brokerProperties = {"listeners=PLAINTEXT://localhost:0"}
)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@ActiveProfiles("test")
@Slf4j
@EnableKafka
public class CouponIssueFlowIntegrationTest {


    private EmbeddedKafkaBroker embeddedKafka;

    @Autowired
    void setEmbeddedKafka(EmbeddedKafkaBroker broker) {
        this.embeddedKafka = broker;
    }

    @BeforeAll
    void init() {
        System.setProperty("spring.kafka.bootstrap-servers", embeddedKafka.getBrokersAsString());
    }


    @Autowired
    private CouponRepository couponRepository;

    @Autowired
    private CouponIssueRepository couponIssueRepository;

    @Autowired
    private OutboxRepository outboxRepository;

    @Autowired
    private CouponUseCase couponUseCase;

    @Autowired
    private OutboxRelayScheduler outboxRelayScheduler;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private CouponIssueKafkaConsumer kafkaConsumer;

    @Autowired
    private Clock clock;


    private final String couponCode = "SPRING-10";
    private final Long userId = 42L;



    @BeforeEach
    void setup() {

        outboxRepository.deleteAll();

        Coupon coupon = Coupon.createLimitedFixed(
                couponCode, 10, 10,
                LocalDateTime.now(clock).minusDays(1),
                LocalDateTime.now(clock).plusDays(1)
        );
        couponRepository.save(coupon);
    }

    @Test
    @DisplayName("쿠폰 발급 전체 플로우 통합 테스트")
    public void testFullCouponIssueFlow() throws Exception {
        // 1. 도메인 레벨 이벤트 발생
        couponUseCase.requestCoupon(new IssueLimitedCouponCommand(userId, couponCode, "requestId-123"));

        List<OutboxMessage> outboxMessagesBefore = outboxRepository.findAll();
        log.info("[Outbox 메시지 저장 전] outboxMessages={}", outboxMessagesBefore);

        // 2. Outbox 저장 확인
        List<OutboxMessage> outboxMessages = outboxRepository.findTop100ByIdGreaterThanOrderByIdAsc("1");
        assertThat(outboxMessages).hasSize(1);

        // 3. OutboxRelayScheduler를 수동 호출하여 Kafka로 발행
        outboxRelayScheduler.relay();

        // 4. KafkaConsumer가 메시지 처리
        OutboxMessage message = outboxMessages.get(0);
        kafkaConsumer.consume(new ConsumerRecord<>("coupon.issue", 0, 0L, message.getAggregateId(), objectMapper.readValue(message.getPayload(), kr.hhplus.be.server.infrastructure.kafka.CouponIssueKafkaMessage.class)), () -> {});

        // 5. 실제 쿠폰 발급 확인
        assertThat(couponIssueRepository.hasIssued(userId, couponRepository.findByCode(couponCode).get().getId())).isTrue();
    }

}
