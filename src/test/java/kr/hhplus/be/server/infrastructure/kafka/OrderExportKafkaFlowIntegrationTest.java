package kr.hhplus.be.server.infrastructure.kafka;

import kr.hhplus.be.server.application.order.OrderService;
import kr.hhplus.be.server.common.vo.Money;
import kr.hhplus.be.server.domain.order.Order;
import kr.hhplus.be.server.domain.order.OrderItem;
import kr.hhplus.be.server.domain.order.OrderRepository;
import kr.hhplus.be.server.infrastructure.external.FakeExternalPlatformClient;
import lombok.extern.slf4j.Slf4j;
import org.awaitility.Awaitility;
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

import java.time.Duration;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@Tag("kafka")
@SpringBootTest
@ActiveProfiles("test")
@EmbeddedKafka(
        partitions = 1,
        topics = {"order-export"},
        brokerProperties = {"listeners=PLAINTEXT://localhost:0"}  // 0은 사용 가능한 포트를 자동 할당
)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@EnableKafka
@Slf4j
class OrderExportKafkaFlowIntegrationTest {

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
    OrderService orderService;

    @Autowired
    OrderRepository orderRepository;

    @Autowired
    FakeExternalPlatformClient fakeExternalPlatformClient;

    @BeforeEach
    void setUp() {
        fakeExternalPlatformClient.clear();
    }


    private Order order;
    @BeforeEach
    void setup() {
        // given
        Long userId = 100L;
        Long productId = 1L;
        int size = 270;
        long unitPrice = 199000L;

        OrderItem item = OrderItem.of(productId, 1, size, Money.wons(unitPrice));
        order = orderService.createOrder(userId, List.of(item), Money.wons(unitPrice));
        log.info("[OrderExportKafkaFlowIntegrationTest] 주문 생성 완료 - orderId={}", order.getId());
    }

    @Test
    @DisplayName("주문 생성 후 Kafka를 통해 외부 플랫폼으로 주문이 전송되는지 검증")
    void aend_to_end_kafka_flow_should_work() {

        // when
        orderService.confirmOrder(order.getId());

        // then: KafkaConsumer까지 도달하여 sendOrder가 실행되었는지 검증
        Awaitility.await().atMost(Duration.ofSeconds(5)).untilAsserted(() -> {
            assertThat(fakeExternalPlatformClient.getReceivedOrderIds()).contains(order.getId());
        });
    }
}
