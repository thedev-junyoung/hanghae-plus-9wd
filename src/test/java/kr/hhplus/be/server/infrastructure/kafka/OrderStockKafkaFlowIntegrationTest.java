package kr.hhplus.be.server.infrastructure.kafka;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import kr.hhplus.be.server.application.order.CreateOrderCommand;
import kr.hhplus.be.server.application.order.OrderFacadeService;
import kr.hhplus.be.server.common.vo.Money;
import kr.hhplus.be.server.domain.order.*;
import kr.hhplus.be.server.domain.order.Order;
import kr.hhplus.be.server.domain.product.*;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.test.EmbeddedKafkaBroker;
import org.springframework.kafka.test.context.EmbeddedKafka;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDate;
import java.util.List;
import java.util.concurrent.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

@Tag("kafka")
@Slf4j
@SpringBootTest
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@ActiveProfiles("test")
@EmbeddedKafka(
        topics = {"order.stock.decrease.requested", "stock.decrease.failed"},
        partitions = 3,
        brokerProperties = {"listeners=PLAINTEXT://localhost:0"}
)
public class OrderStockKafkaFlowIntegrationTest {

    private EmbeddedKafkaBroker embeddedKafka;

    @Autowired
    void setEmbeddedKafka(EmbeddedKafkaBroker broker) {
        this.embeddedKafka = broker;
    }

    @BeforeAll
    void init() {
        System.setProperty("spring.kafka.bootstrap-servers", embeddedKafka.getBrokersAsString());
    }

    @Autowired private OrderFacadeService orderFacadeService;
    @Autowired private ProductRepository productRepository;
    @Autowired private ProductStockRepository stockRepository;
    @Autowired private OrderRepository orderRepository;
    @Autowired private KafkaTemplate<String, StockDecreaseFailedKafkaMessage> kafkaTemplate;

    @PersistenceContext
    private EntityManager em;

    private Long productId;
    private long testRunId;
    private static final int INIT_STOCK = 10;
    private static final int ORDER_QTY = 5;
    private static final int CONCURRENCY = 3;

    @BeforeEach
    void setUp() {
        this.testRunId = System.nanoTime(); // 유니크한 테스트 식별자

        String productName = "통합 테스트 상품 - " + testRunId;
        Product product = Product.create(productName, "TestBrand", Money.wons(10000), LocalDate.now(), null, null);
        product = productRepository.save(product);
        stockRepository.save(ProductStock.of(product.getId(), 270, INIT_STOCK));

        em.clear();
        this.productId = product.getId();
    }

    @Test
    @DisplayName("Kafka 기반 주문 재고 흐름 통합 테스트: 정상 차감 및 보상 흐름 검증")
    void should_test_order_stock_flow_via_kafka() throws Exception {
        var executor = Executors.newFixedThreadPool(CONCURRENCY);
        var latch = new CountDownLatch(CONCURRENCY);

        for (int i = 0; i < CONCURRENCY; i++) {
            final long userId = testRunId + i;
            executor.execute(() -> {
                try {
                    CreateOrderCommand command = new CreateOrderCommand(
                            userId,
                            List.of(new CreateOrderCommand.OrderItemCommand(productId, ORDER_QTY, 270)),
                            null
                    );
                    orderFacadeService.createOrder(command);
                } catch (Exception e) {
                    log.warn("[Test] 주문 실패 - {}", e.getMessage());
                } finally {
                    latch.countDown();
                }
            });
        }

        latch.await();
        em.clear();

        await().atMost(5, TimeUnit.SECONDS).untilAsserted(() -> {
            List<Order> testOrders = orderRepository.findAllWithItems().stream()
                    .filter(o -> o.getItems().stream().anyMatch(i -> i.getProductId().equals(productId)))
                    .toList();

            long confirmedCount = testOrders.stream()
                    .filter(order -> order.getStatus() == OrderStatus.CREATED)
                    .count();

            long failedCount = testOrders.stream()
                    .filter(order -> order.getStatus() == OrderStatus.CANCELLED)
                    .count();

            ProductStock stock = stockRepository.findByProductIdAndSize(productId, 270).orElseThrow();

            log.info("[Await] 재고={}, 성공 주문 수={}, 실패 주문 수={}",
                    stock.getStockQuantity(), confirmedCount, failedCount);

            assertThat(confirmedCount).isEqualTo(2);
            assertThat(failedCount).isEqualTo(1);
            assertThat(stock.getStockQuantity()).isEqualTo(0);
        });
    }

    @Test
    @DisplayName("중복 메시지 수신 시 보상 로직 멱등성 보장")
    void should_handle_duplicate_failure_messages_idempotently() {
        long userId = testRunId + 999;
        Order order = Order.create(
                userId,
                List.of(OrderItem.of(productId, 1, 270, Money.wons(10000))),
                Money.wons(10000)
        );
        String orderId = order.getId();
        orderRepository.save(order);
        em.clear();

        StockDecreaseFailedKafkaMessage message = new StockDecreaseFailedKafkaMessage(orderId, userId, "재고 부족");

        kafkaTemplate.send("stock.decrease.failed", orderId, message);
        kafkaTemplate.send("stock.decrease.failed", orderId, message);

        await().atMost(5, TimeUnit.SECONDS).untilAsserted(() -> {
            Order updated = orderRepository.findById(orderId).orElseThrow();
            assertThat(updated.getStatus()).isEqualTo(OrderStatus.CANCELLED);
        });
    }

    @Test
    @DisplayName("고부하 환경에서 Kafka 기반 주문 생성 및 재고 처리")
    void should_handle_high_concurrency_with_kafka_stock_decrease() throws InterruptedException {
        int concurrency = 50;
        CountDownLatch latch = new CountDownLatch(concurrency);

        for (int i = 0; i < concurrency; i++) {
            long userId = testRunId + 2000 + i;
            Executors.newSingleThreadExecutor().execute(() -> {
                try {
                    CreateOrderCommand cmd = new CreateOrderCommand(
                            userId,
                            List.of(new CreateOrderCommand.OrderItemCommand(productId, ORDER_QTY, 270)),
                            null
                    );
                    orderFacadeService.createOrder(cmd);
                } catch (Exception e) {
                    log.warn("주문 실패 - {}", e.getMessage());
                } finally {
                    latch.countDown();
                }
            });
        }

        latch.await();
        em.clear();

        await().atMost(10, TimeUnit.SECONDS).untilAsserted(() -> {
            List<Order> orders = orderRepository.findAllWithItems().stream()
                    .filter(o -> o.getUserId() >= testRunId + 2000)
                    .toList();

            long created = orders.stream().filter(o -> o.getStatus() == OrderStatus.CREATED).count();
            long cancelled = orders.stream().filter(o -> o.getStatus() == OrderStatus.CANCELLED).count();
            int remaining = stockRepository.findByProductIdAndSize(productId, 270).orElseThrow().getStockQuantity();

            log.info("총 주문: {}, 성공: {}, 실패: {}, 재고: {}", orders.size(), created, cancelled, remaining);

            assertThat(created + cancelled).isEqualTo(concurrency);
            assertThat(created).isLessThanOrEqualTo(INIT_STOCK);
            assertThat(cancelled).isGreaterThan(0);
        });
    }
}
