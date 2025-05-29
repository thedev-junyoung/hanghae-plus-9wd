package kr.hhplus.be.server.infrastructure.kafka;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import kr.hhplus.be.server.application.order.CreateOrderCommand;
import kr.hhplus.be.server.application.order.OrderFacadeService;
import kr.hhplus.be.server.application.order.OrderResult;
import kr.hhplus.be.server.domain.order.Order;
import kr.hhplus.be.server.domain.order.OrderItem;
import kr.hhplus.be.server.domain.order.OrderRepository;
import kr.hhplus.be.server.domain.order.OrderStatus;
import kr.hhplus.be.server.domain.product.Product;
import kr.hhplus.be.server.domain.product.ProductRepository;
import kr.hhplus.be.server.domain.product.ProductStock;
import kr.hhplus.be.server.domain.product.ProductStockRepository;
import kr.hhplus.be.server.common.vo.Money;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.test.context.EmbeddedKafka;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import java.time.LocalDate;

import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;


@Tag("kafka")
@Slf4j
@SpringBootTest
@ActiveProfiles("test")
@EmbeddedKafka(
        topics = {"order.stock.decrease.requested", "stock.decrease.failed"},
        partitions = 3,
        brokerProperties = {"listeners=PLAINTEXT://localhost:0"}
)
public class OrderStockKafkaFlowIntegrationTest {

    @Autowired
    private OrderFacadeService orderFacadeService;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private ProductStockRepository stockRepository;

    @Autowired
    private OrderRepository orderRepository;

    @PersistenceContext
    private EntityManager em;

    @Autowired
    private KafkaTemplate<String, StockDecreaseFailedKafkaMessage> kafkaTemplate;


    private Long productId;

    private static final int INIT_STOCK = 10;
    private static final int ORDER_QTY = 5;
    private static final int CONCURRENCY = 3;


    @DynamicPropertySource
    static void kafkaProperties(DynamicPropertyRegistry registry) {
        // EmbeddedKafkaBroker.getBrokersAsString()을 정적으로 접근
        // 테스트 클래스 로딩 시 자동으로 설정됨
        registry.add("spring.kafka.bootstrap-servers", () ->
                System.getProperty("spring.embedded.kafka.brokers")
        );
    }
    @BeforeEach
    void setUp() {
        Product product = Product.create("통합 테스트 상품", "TestBrand", Money.wons(10000), LocalDate.now(), null, null);
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
            final long userId = 100L + i;
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
            // 1. 모든 주문 조회
            List<Order> all = orderRepository.findAllWithItems();

            // 2. 테스트 상품의 주문만 필터링
            List<Order> testOrders = all.stream()
                    .filter(o -> o.getItems().stream()
                            .anyMatch(i -> i.getProductId().equals(productId))
                    )
                    .toList();

            log.info("[Await] 테스트 주문 수={}", testOrders.size());
            for (Order order : testOrders) {
                log.info("[Await] 주문 ID={}, 상태={}", order.getId(), order.getStatus());
            }

            long confirmedCount = testOrders.stream()
                    .filter(order -> order.getStatus() == OrderStatus.CREATED) // 비즈니스 상 SUCCESS 상태
                    .count();
            long failedCount = testOrders.stream()
                    .filter(order -> order.getStatus() == OrderStatus.CANCELLED)
                    .count();
            ProductStock stock = stockRepository.findByProductIdAndSize(productId, 270)
                    .orElseThrow();

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
        Order order = Order.create(
                100L,
                List.of(OrderItem.of(productId, 1, 270, Money.wons(10000))),
                Money.wons(10000)
        );
        String orderId = order.getId();
        orderRepository.save(order);
        em.clear();

        StockDecreaseFailedKafkaMessage message = new StockDecreaseFailedKafkaMessage(orderId , 100L, "재고 부족");
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
            long userId = 2000 + i;
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
                    .filter(o -> o.getUserId() >= 2000)
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
