package kr.hhplus.be.server.infrastructure.kafka;

import kr.hhplus.be.server.domain.orderexport.OrderExportPayload;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class KafkaOrderExportProducer {

    private static final String TOPIC = "order-export";

    private final KafkaTemplate<String, OrderExportPayload> kafkaTemplate;

    public void send(OrderExportPayload payload) {
        kafkaTemplate.send(TOPIC, payload.getOrderId(), payload);
        log.info("[KafkaProducer] 주문 전송 이벤트 발행 - orderId={}", payload.getOrderId());
    }
}
