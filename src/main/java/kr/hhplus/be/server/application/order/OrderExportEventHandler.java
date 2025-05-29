package kr.hhplus.be.server.application.order;

import kr.hhplus.be.server.infrastructure.kafka.KafkaOrderExportProducer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Slf4j
@Component
@RequiredArgsConstructor
public class OrderExportEventHandler {

    private final KafkaOrderExportProducer kafkaProducer;

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handle(OrderExportRequestedEvent event) {
        log.info("[OrderExportEventHandler] Kafka 전송 시작 - orderId={}", event.getAggregateId());
        kafkaProducer.send(event.getPayload());
    }
}
