package kr.hhplus.be.server.infrastructure.kafka;

import jakarta.annotation.PostConstruct;
import kr.hhplus.be.server.application.order.OrderCompensationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class KafkaStockDecreaseFailureConsumer {

    private final OrderCompensationService compensationService;

    @KafkaListener(
            topics = "stock.decrease.failed",
            groupId = "stock-failure-consumer-group",
            containerFactory = "stockDecreaseFailedKafkaListenerFactory"
    )
    public void consume(StockDecreaseFailedKafkaMessage event, Acknowledgment ack) {
        try {
            log.warn("[KafkaConsumer] 재고 차감 실패 수신 - orderId={}, reason={}", event.getOrderId(), event.getReason());

            // 주문 보상 처리: 상태를 FAILED로 변경
            compensationService.markOrderAsFailed(event.getOrderId());
            ack.acknowledge();
        } catch (Exception e) {
            log.error("[KafkaConsumer] 재고 차감 실패 보상 처리 오류 - orderId={}, error={}", event.getOrderId(), e.getMessage(), e);
        }
    }
}
