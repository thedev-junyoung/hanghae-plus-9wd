package kr.hhplus.be.server.application.order;

import kr.hhplus.be.server.infrastructure.kafka.StockDecreaseRequestedKafkaMessage;
import kr.hhplus.be.server.infrastructure.kafka.KafkaStockDecreaseProducer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionalEventListener;

import static org.springframework.transaction.event.TransactionPhase.AFTER_COMMIT;

@Component
@RequiredArgsConstructor
@Slf4j
public class StockDecreaseRequestedEventHandler {

    private final KafkaStockDecreaseProducer kafkaProducer;

    @TransactionalEventListener(phase = AFTER_COMMIT)
    public void handle(StockDecreaseRequested event) {
        StockDecreaseRequestedKafkaMessage kafkaMessage =
                StockDecreaseRequestedKafkaMessage.from(event);  // 아래에서 정의

        kafkaProducer.send(kafkaMessage);
    }
}

