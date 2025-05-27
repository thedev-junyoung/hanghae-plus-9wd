package kr.hhplus.be.server.application.order;

import kr.hhplus.be.server.domain.order.OrderException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.hibernate.LazyInitializationException;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
@RequiredArgsConstructor
@Slf4j
public class OrderConfirmedEventHandler {

    private final OrderUseCase orderUseCase;
    private final ApplicationEventPublisher eventPublisher;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Async
    public void handle(OrderConfirmedEvent event) {
        String orderId = event.getAggregateId();
        try {
            log.info("[Order] 이벤트 수신: {} - aggregateId={}", event.getEventDescription(), event.getAggregateId());
            orderUseCase.confirmOrder(orderId);
        } catch (OrderException.InvalidStateException e) {
            log.warn("[Order] 상태 전이 불가 - orderId={}, status={}, error={}",
                    orderId, e.getCurrentStatus(), e.getMessage());
            eventPublisher.publishEvent(new OrderConfirmationFailedEvent(orderId, "잘못된 상태 전이"));
        }
        catch (OrderException.NotFoundException e) {
            log.warn("[Order] 주문 존재하지 않음 - orderId={}, error={}", orderId, e.getMessage());
            eventPublisher.publishEvent(new OrderConfirmationFailedEvent(orderId, "주문 없음"));
        }
        catch (LazyInitializationException e) {
            log.error("[Order] LAZY 로딩 실패 - orderId={}, error={}", orderId, e.getMessage());
            eventPublisher.publishEvent(new OrderConfirmationFailedEvent(orderId, "영속성 컨텍스트 없음"));
        }
    }
}
