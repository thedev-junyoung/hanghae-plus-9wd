package kr.hhplus.be.server.application.order;

import kr.hhplus.be.server.common.vo.Money;
import kr.hhplus.be.server.domain.order.Order;
import kr.hhplus.be.server.domain.order.OrderException;
import kr.hhplus.be.server.domain.order.OrderItem;
import kr.hhplus.be.server.domain.order.OrderRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;


@Service
@RequiredArgsConstructor
@Slf4j
public class OrderService implements OrderUseCase {

    private final OrderRepository orderRepository;
    private final ApplicationEventPublisher eventPublisher;

    @Transactional
    public Order createOrder(Long userId, List<OrderItem> items, Money totalAmount) {
        Order order = Order.create(userId, items, totalAmount);
        orderRepository.save(order);
        order.getDomainEvents().forEach(eventPublisher::publishEvent);
        return order;
    }
    @Transactional(readOnly = true)
    public Order getOrderForPayment(String orderId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new OrderException.NotFoundException(orderId));
        order.validatePayable();
        return order;
    }

    @Transactional
    public void confirmOrder(String orderId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new OrderException.NotFoundException(orderId));

        order.markConfirmed();
        orderRepository.save(order);

        // 이벤트 발행 (도메인에서 등록된 이벤트를 꺼내어 발행)
        order.getDomainEvents().forEach(eventPublisher::publishEvent);
        order.clearEvents();

        log.info("[Order] 주문 확인 완료 - orderId={}", orderId);
    }

}

