package kr.hhplus.be.server.infrastructure.kafka;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class KafkaStockDecreaseProducer {

    private static final String TOPIC = "order.stock.decrease.requested";

    private final KafkaTemplate<String, StockDecreaseRequestedKafkaMessage> kafkaTemplate;

    public void send(StockDecreaseRequestedKafkaMessage event) {
        kafkaTemplate.send(TOPIC, event.getOrderId(), event);
        log.info("[KafkaProducer] 재고 차감 요청 발행 - orderId={}", event.getOrderId());
    }
}
