package kr.hhplus.be.server.application.payment;

import jakarta.persistence.EntityManager;
import kr.hhplus.be.server.common.vo.Money;
import kr.hhplus.be.server.domain.balance.Balance;
import kr.hhplus.be.server.domain.balance.BalanceRepository;
import kr.hhplus.be.server.domain.order.Order;
import kr.hhplus.be.server.domain.order.OrderItem;
import kr.hhplus.be.server.domain.order.OrderRepository;
import kr.hhplus.be.server.domain.order.OrderStatus;
import kr.hhplus.be.server.domain.payment.Payment;
import kr.hhplus.be.server.domain.payment.PaymentRepository;
import kr.hhplus.be.server.infrastructure.redis.ProductRankRedisRepository;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.scheduling.annotation.EnableAsync;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;
import static org.awaitility.Awaitility.await;

/**
 * PaymentFacadeServiceIntegrationTest
 *
 * <p>이 통합 테스트 클래스는 {@link kr.hhplus.be.server.application.payment.PaymentFacadeService}의
 * 결제 처리 흐름을 통합적으로 검증합니다.</p>
 *
 * <p>특히 다음 요소를 중점적으로 확인합니다:</p>
 * <ul>
 *     <li>잔액 차감과 결제 성공 처리</li>
 *     <li>도메인 이벤트 기반 주문 상태 변경 {@code CONFIRMED}</li>
 *     <li>이벤트 등록 → 발행 → 비동기 수신 처리 흐름의 완결성</li>
 *     <li>잔액 부족 등 예외 상황 처리</li>
 * </ul>
 *
 * <p>Spring의 {@code @Async}와 {@code @TransactionalEventListener} 기반의
 * 비동기 이벤트 흐름이 실제로 작동함을 Awaitility로 보장합니다.</p>
 *
 * @author 전준영
 */
@SpringBootTest
@EnableAsync
@Slf4j
class PaymentFacadeServiceIntegrationTest {

    @Autowired
    PaymentFacadeService paymentFacadeService;

    @Autowired
    PaymentRepository paymentRepository;

    @Autowired
    BalanceRepository balanceRepository;

    @Autowired
    OrderRepository orderRepository;

    @Autowired
    EntityManager entityManager;

    @Autowired
    ProductRankRedisRepository productRankRedisRepository;

    private final Long productId = 1L;
    private final int size = 270;

    @Test
    @DisplayName("성공: 잔액 차감 후 이벤트 기반으로 결제 정보와 주문 상태가 정확히 변경된다")
    void should_process_payment_successfully_and_confirm_order() {
        // given
        Long userId = 1000L;
        long price = 199000L;

        // 반드시 충분한 잔액으로 초기화
        balanceRepository.findByUserId(userId).ifPresentOrElse(
                balance -> {
                    balance.charge(Money.wons(1_000_000L), "유저 직접 충전 요청", "request-001"); // 충분히 충전
                    balanceRepository.save(balance);
                },
                () -> balanceRepository.save(Balance.createNew(userId, Money.wons(1_000_000L)))
        );

        long originalBalance = balanceRepository.findByUserId(userId)
                .orElseThrow().getAmount();

        // 주문 생성
        Order order = Order.create(userId,
                List.of(OrderItem.of(productId, 1, size, Money.wons(price))),
                Money.wons(price));
        orderRepository.save(order);

        RequestPaymentCommand command = new RequestPaymentCommand(order.getId(), userId, price, "BALANCE");

        // when
        PaymentResult result = paymentFacadeService.requestPayment(command);

        // then - 잔액은 즉시 검증 가능
        Balance updatedBalance = balanceRepository.findByUserId(userId).orElseThrow();
        assertThat(updatedBalance.getAmount()).isEqualTo(originalBalance - price);

        // 비동기 이벤트 결과 대기 → 결제 정보 & 주문 상태
        await().atMost(5, TimeUnit.SECONDS).untilAsserted(() -> {
            entityManager.clear();

            // 1. 주문 상태 확인
            OrderStatus status = entityManager
                    .createQuery("SELECT o.status FROM Order o WHERE o.id = :id", OrderStatus.class)
                    .setParameter("id", order.getId())
                    .getSingleResult();
            assertThat(status).isEqualTo(OrderStatus.CONFIRMED);

            // 2. 결제 정보 확인
            Payment payment = paymentRepository.findByOrderId(order.getId()).orElse(null);
            assertThat(payment).isNotNull();
            assertThat(payment.getOrderId()).isEqualTo(order.getId());

            // 3. Redis 랭킹 점수 확인
            String redisDailyKey = "ranking:daily:" + LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
            String redisWeeklyKey = "ranking:weekly:" + LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
            String redisMonthlyKey = "ranking:monthly:" + LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));

            Double dailyScore = productRankRedisRepository.getScore(redisDailyKey, productId);
            Double weeklyScore = productRankRedisRepository.getScore(redisWeeklyKey, productId);
            Double monthlyScore = productRankRedisRepository.getScore(redisMonthlyKey, productId);



            logRankingScore("DAILY", redisDailyKey, dailyScore);
            logRankingScore("WEEKLY", redisWeeklyKey, weeklyScore);
            logRankingScore("MONTHLY", redisMonthlyKey, monthlyScore);



            // 상품 1개 기준으로 1점 기록됐는지 확인
            assertThat(dailyScore).isNotNull();
            assertThat(dailyScore).isEqualTo(1.0);
        });




        assertThat(result.status()).isEqualTo("SUCCESS");
    }

    @Test
    @DisplayName("잔액 부족 시 예외 발생 및 상태 불변")
    void requestPayment_fail_ifInsufficientBalance() {
        Long userId = 101L;
        long tooMuch = 500_000L;

        balanceRepository.findByUserId(userId).ifPresentOrElse(
                balance -> {
                    balance.decrease(Money.wons(balance.getAmount()));
                    balanceRepository.save(balance);
                },
                () -> balanceRepository.save(Balance.createNew(userId, Money.wons(0L)))
        );

        Order order = Order.create(userId,
                List.of(OrderItem.of(productId, 1, size, Money.wons(tooMuch))),
                Money.wons(tooMuch));
        orderRepository.save(order);

        RequestPaymentCommand command = new RequestPaymentCommand(order.getId(), userId, tooMuch, "BALANCE");

        assertThatThrownBy(() -> paymentFacadeService.requestPayment(command))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("잔액이 부족");

        assertThat(paymentRepository.findByOrderId(order.getId())).isEmpty();
        assertThat(orderRepository.findById(order.getId()).orElseThrow().getStatus()).isEqualTo(OrderStatus.CREATED);
    }
    private void logRankingScore(String keyName, String key, Double score) {
        if (score == null) {
            log.info("[Redis][{}] 아직 점수 없음 - key={}", keyName, key);
        } else {
            log.info("[Redis][{}] 점수 확인 - key={}, score={}", keyName, key, score);
        }
    }
}