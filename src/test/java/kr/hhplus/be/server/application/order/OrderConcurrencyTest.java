//package kr.hhplus.be.server.application.order;
//
//import jakarta.persistence.EntityManager;
//import jakarta.persistence.PersistenceContext;
//import kr.hhplus.be.server.domain.order.Order;
//import kr.hhplus.be.server.domain.order.OrderRepository;
//import kr.hhplus.be.server.domain.order.OrderStatus;
//import kr.hhplus.be.server.domain.product.Product;
//import kr.hhplus.be.server.domain.product.ProductRepository;
//import kr.hhplus.be.server.domain.product.ProductStock;
//import kr.hhplus.be.server.domain.product.ProductStockRepository;
//import kr.hhplus.be.server.common.vo.Money;
//import lombok.extern.slf4j.Slf4j;
//import org.junit.jupiter.api.BeforeEach;
//import org.junit.jupiter.api.DisplayName;
//import org.junit.jupiter.api.Test;
//import org.springframework.beans.factory.annotation.Autowired;
//import org.springframework.boot.test.context.SpringBootTest;
//import org.springframework.kafka.test.context.EmbeddedKafka;
//import org.springframework.test.context.ActiveProfiles;
//
//import java.time.LocalDate;
//import java.util.List;
//import java.util.concurrent.CountDownLatch;
//import java.util.concurrent.ExecutorService;
//import java.util.concurrent.Executors;
//import java.util.concurrent.TimeUnit;
//
//import static org.assertj.core.api.Assertions.assertThat;
//import static org.awaitility.Awaitility.await;
//
//@Slf4j
//@SpringBootTest
//@ActiveProfiles("test")
//@EmbeddedKafka(
//        topics = {"order.stock.decrease.requested", "stock.decrease.failed"},
//        partitions = 3,
//        brokerProperties = {"listeners=PLAINTEXT://localhost:0"}
//)
//class OrderKafkaConcurrencyTest {
//
//    @Autowired
//    private OrderFacadeService orderFacadeService;
//
//    @Autowired
//    private ProductRepository productRepository;
//
//    @Autowired
//    private ProductStockRepository stockRepository;
//
//    @Autowired
//    private OrderRepository orderRepository;
//
//    @PersistenceContext
//    private EntityManager em;
//
//    private Long productId;
//
//    private static final int INIT_STOCK = 10;
//    private static final int ORDER_QTY = 5;
//    private static final int CONCURRENCY = 3;
//
//    @BeforeEach
//    void setUp() {
//        // 트랜잭션 없이 바로 DB 반영됨
//        Product product = Product.create(
//                "Test Product", "TestBrand", Money.wons(10000),
//                LocalDate.now().minusDays(1), null, null
//        );
//        product = productRepository.save(product);
//
//        stockRepository.save(ProductStock.of(product.getId(), 270, INIT_STOCK));
//
//        em.clear(); // 1차 캐시 제거 (다른 쓰레드에서 DB만 조회하게 됨)
//
//        this.productId = product.getId();
//    }
//
//    @Test
//    @DisplayName("Kafka + 보상처리: 동시 주문 시 재고 차감 및 실패 보상이 정확히 동작해야 한다")
//    void should_handle_concurrent_orders_with_kafka_and_compensation() throws Exception {
//        ExecutorService executor = Executors.newFixedThreadPool(CONCURRENCY);
//        CountDownLatch latch = new CountDownLatch(CONCURRENCY);
//
//        for (int i = 0; i < CONCURRENCY; i++) {
//            long userId = 100L + i;
//            executor.execute(() -> {
//                try {
//                    CreateOrderCommand command = new CreateOrderCommand(
//                            userId,
//                            List.of(new CreateOrderCommand.OrderItemCommand(productId, ORDER_QTY, 270)),
//                            null
//                    );
//                    orderFacadeService.createOrder(command);
//                } catch (Exception e) {
//                    log.warn("주문 실패: {}", e.getMessage());
//                } finally {
//                    latch.countDown();
//                }
//            });
//        }
//
//        latch.await();
//        em.clear();
//
//        await().atMost(5, TimeUnit.SECONDS).untilAsserted(() -> {
//            // 1. 모든 주문 조회
//            List<Order> all = orderRepository.findAllWithItems();
//
//            // 2. 테스트 상품의 주문만 필터링
//            List<Order> testOrders = all.stream()
//                    .filter(o -> o.getItems().stream()
//                            .anyMatch(i -> i.getProductId().equals(productId))
//                    )
//                    .toList();
//
//            log.info("[Await] 테스트 주문 수={}", testOrders.size());
//            for (Order order : testOrders) {
//                log.info("[Await] 주문 ID={}, 상태={}", order.getId(), order.getStatus());
//            }
//
//            long confirmedCount = testOrders.stream()
//                    .filter(order -> order.getStatus() == OrderStatus.CREATED) // 비즈니스 상 SUCCESS 상태
//                    .count();
//            long failedCount = testOrders.stream()
//                    .filter(order -> order.getStatus() == OrderStatus.CANCELLED)
//                    .count();
//            ProductStock stock = stockRepository.findByProductIdAndSize(productId, 270)
//                    .orElseThrow();
//
//            log.info("[Await] 재고={}, 성공 주문 수={}, 실패 주문 수={}",
//                    stock.getStockQuantity(), confirmedCount, failedCount);
//
//            assertThat(confirmedCount).isEqualTo(2);
//            assertThat(failedCount).isEqualTo(1);
//            assertThat(stock.getStockQuantity()).isEqualTo(0);
//        });
//    }
//
//
//
//}
