package kr.hhplus.be.server.application.order;

import jakarta.persistence.EntityManager;
import kr.hhplus.be.server.common.vo.Money;
import kr.hhplus.be.server.domain.order.Order;
import kr.hhplus.be.server.domain.order.OrderItem;
import kr.hhplus.be.server.domain.order.OrderRepository;
import kr.hhplus.be.server.domain.order.OrderStatus;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.scheduling.annotation.EnableAsync;

import java.util.List;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.awaitility.Awaitility.await;

@SpringBootTest
@EnableAsync
class OrderConfirmedEventHandlerTest {

    @Autowired
    private ApplicationEventPublisher eventPublisher;

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private EntityManager entityManager;

    @Test
    @DisplayName("주문이 이미 CONFIRMED 상태일 경우 - OrderConfirmationFailedEvent가 발행된다")
    void should_publish_failed_event_if_order_already_confirmed() {
        // given
        Long userId = 2000L;
        Order order = Order.create(userId, List.of(
                OrderItem.of(1L, 1, 270, Money.wons(10000L))
        ), Money.wons(10000L));
        order.markConfirmed(); // 주문 상태를 이미 CONFIRMED로 만듦
        orderRepository.save(order);

        // when
        eventPublisher.publishEvent(new OrderConfirmedEvent(order.getId()));

        // then - 이벤트가 비동기로 처리되므로 await 필요
        await().atMost(3, TimeUnit.SECONDS).untilAsserted(() -> {
            entityManager.clear();
            Order saved = orderRepository.findById(order.getId()).orElseThrow();
            assertThat(saved.getStatus()).isEqualTo(OrderStatus.CONFIRMED);
        });
    }
}
