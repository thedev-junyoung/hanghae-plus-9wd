package kr.hhplus.be.server.infrastructure.kafka;

import com.fasterxml.jackson.databind.ObjectMapper;
import kr.hhplus.be.server.application.coupon.CouponUseCase;
import kr.hhplus.be.server.application.coupon.IssueLimitedCouponCommand;
import kr.hhplus.be.server.domain.coupon.CouponIssueRepository;
import kr.hhplus.be.server.domain.coupon.CouponRepository;
import kr.hhplus.be.server.domain.outbox.OutboxMessage;
import kr.hhplus.be.server.domain.outbox.OutboxOffset;
import kr.hhplus.be.server.domain.outbox.OutboxRepository;
import kr.hhplus.be.server.infrastructure.outbox.OutboxRelayScheduler;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.awaitility.Awaitility;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.kafka.annotation.EnableKafka;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.test.EmbeddedKafkaBroker;
import org.springframework.kafka.test.context.EmbeddedKafka;
import org.springframework.kafka.test.utils.KafkaTestUtils;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.TimeUnit;

import static java.util.concurrent.TimeUnit.SECONDS;
import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

@Tag("kafka")
@Slf4j
@SpringBootTest
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@ActiveProfiles("test")
@EmbeddedKafka(
        partitions = 1,
        topics = {"coupon.issue", "coupon.issue.DLT"},
        brokerProperties = {"listeners=PLAINTEXT://localhost:0"}
)
@EnableKafka
public class CouponIssueKafkaConsumerIntegrationTest {


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
    private KafkaTemplate<Object, Object> defaultKafkaTemplate;


    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private CouponUseCase couponUseCase;

    @Autowired
    private OutboxRelayScheduler outboxRelayScheduler;

    @Autowired
    private OutboxRepository outboxRepository;

    @Autowired
    private KafkaTemplate<String, CouponIssueKafkaMessage> kafkaTemplate;

    @Autowired
    private CouponRepository couponRepository;



    private final Long userId = 123L;
    private final String couponCode = "WELCOME10";

    private KafkaConsumer<String, String> dltConsumer;

    @Autowired
    private CouponIssueRepository couponIssueRepository;


    @BeforeEach
    void setUp() {

        outboxRepository.deleteAll();

        Map<String, Object> consumerProps = KafkaTestUtils.consumerProps("test-dlt-consumer", "false", embeddedKafka);
        Properties props = new Properties();
        props.putAll(consumerProps);
        props.put("key.deserializer", StringDeserializer.class.getName());
        props.put("value.deserializer", StringDeserializer.class.getName());
        props.put("auto.offset.reset", "earliest");
        dltConsumer = new KafkaConsumer<>(props);
        dltConsumer.subscribe(Collections.singletonList("coupon.issue.DLT"));
    }

    @Test
    @DisplayName("실패 쿠폰 발급 메시지는 DLT로 라우팅되어야 한다")
    void shouldRouteFailedCouponMessageToDlt() throws Exception {
        // given
        CouponIssueKafkaMessage failureMessage = new CouponIssueKafkaMessage(999L, "FAILURE_TEST");
        defaultKafkaTemplate.send("coupon.issue", "999", objectMapper.writeValueAsString(failureMessage));

        // then: Awaitility 로 DLT 수신까지 기다림
        Awaitility.await().atMost(10, TimeUnit.SECONDS)
                .untilAsserted(() -> {
                    assertThat(CouponIssueKafkaConsumer.receivedDltMessages).isNotEmpty();

                    ConsumerRecord<String, String> record = CouponIssueKafkaConsumer.receivedDltMessages.get(0);
                    byte[] decoded = Base64.getDecoder().decode(record.value().replaceAll("\"", ""));
                    String json = new String(decoded, StandardCharsets.UTF_8);

                    log.info("[TEST] 디코딩된 DLT 메시지 = {}", json);
                    assertThat(json).contains("FAILURE_TEST");
                });
    }


    @Test
    @DisplayName("성공적인 쿠폰 발급 메시지는 DLT로 가지 않아야 하고, 정상 처리되어야 한다")
    void shouldNotGoToDltOnSuccess() throws Exception {
        CouponIssueKafkaMessage successMessage = new CouponIssueKafkaMessage(999L, "SUCCESS_CASE");
        kafkaTemplate.send("coupon.issue", "999", successMessage); // 문자열 말고 객체 그대로 전송

        await().atMost(5, SECONDS).untilAsserted(() -> {
            assertThat(couponIssueRepository.existsByUserIdAndCouponId(999L, 4L)).isTrue();
        });


        ConsumerRecords<String, String> records = dltConsumer.poll(Duration.ofMillis(100));
        assertThat(records.count()).isEqualTo(0);
    }

    @Test
    @DisplayName("쿠폰 발급 요청 중복 방지 테스트")
    void shouldPreventDuplicateCouponIssue() {
        // given
        IssueLimitedCouponCommand command = new IssueLimitedCouponCommand(123L, "WELCOME10", "duplicate-req-1");

        // when
        couponUseCase.requestCoupon(command);
        couponUseCase.requestCoupon(command); // 중복 요청

        outboxRelayScheduler.relay(); // 한 번만 발행되어야 함

        // then
        List<OutboxMessage> outboxMessages = outboxRepository.findAll();
        assertThat(outboxMessages).hasSize(1); // 중복 제거 검증
    }


    @Test
    @DisplayName("이미 발행된 메시지는 재발행되지 않아야 한다")
    void relayShouldNotRePublishAlreadySentMessage() {
        IssueLimitedCouponCommand command = new IssueLimitedCouponCommand(userId, couponCode, "request-456");
        couponUseCase.requestCoupon(command);
        outboxRelayScheduler.relay(); // 1차 발행
        outboxRelayScheduler.relay(); // 2차 발행 → 아무것도 없어야 함

        List<OutboxMessage> messages = outboxRepository.findAll();
        assertThat(messages).hasSize(1); // 여전히 1개
    }


}
