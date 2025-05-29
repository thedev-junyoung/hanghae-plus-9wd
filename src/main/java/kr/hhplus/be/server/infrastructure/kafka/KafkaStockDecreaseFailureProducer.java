package kr.hhplus.be.server.infrastructure.kafka;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class KafkaStockDecreaseFailureProducer {

    private static final String FAILURE_TOPIC = "stock.decrease.failed";

    private final KafkaTemplate<String, StockDecreaseFailedKafkaMessage> kafkaTemplate;

    public void send(StockDecreaseFailedKafkaMessage event) {
        kafkaTemplate.send(FAILURE_TOPIC, event.getOrderId(), event);
        log.warn("[KafkaProducer] 재고 차감 실패 발행 - orderId={}", event.getOrderId());
    }
}
