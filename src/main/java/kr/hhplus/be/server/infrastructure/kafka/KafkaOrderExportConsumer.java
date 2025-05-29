package kr.hhplus.be.server.infrastructure.kafka;

import kr.hhplus.be.server.application.orderexport.OrderExportUseCase;
import kr.hhplus.be.server.domain.orderexport.OrderExportPayload;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class KafkaOrderExportConsumer {

    private final OrderExportUseCase orderExportUseCase;

    @KafkaListener(
            topics = "order-export",
            groupId = "order-export-consumer",
            containerFactory = "orderExportKafkaListenerFactory"
    )
    public void listen(OrderExportPayload payload) {
        log.info("[Kafka] 주문 외부 전송 시작 - orderId={}", payload.getOrderId());
        orderExportUseCase.export(payload);
    }
}
