package kr.hhplus.be.server.application.order;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

/**
 * 주문 확정 실패 이벤트를 비동기로 수신하는 핸들러
 * <p> - 현재는 로깅만 처리하며, 차후 복구 로직 필요
 */
@Component
@Slf4j
public class OrderConfirmationFailedEventHandler {

    @Async
    @EventListener
    public void handle(OrderConfirmationFailedEvent event) {
        log.warn("[Order] 주문 확정 실패 이벤트 수신 - orderId={}, reason={}",
                event.getAggregateId(), event.getReason());

        // TODO: 추후 Kafka DLQ 또는 테이블에 실패 인벤트를 저장해서 스케쥴러로 실패 이벤트 처리하는 로직 추가 가능
    }
}
