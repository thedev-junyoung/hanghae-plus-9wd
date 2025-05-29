package kr.hhplus.be.server.infrastructure.kafka;

import kr.hhplus.be.server.application.product.StockService;
import kr.hhplus.be.server.application.product.DecreaseStockCommand;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class KafkaStockDecreaseConsumer {

    private final StockService stockService;
    private final KafkaStockDecreaseFailureProducer failureProducer;

    @KafkaListener(
            topics = "order.stock.decrease.requested",
            groupId = "stock-consumer-group",
            containerFactory = "stockDecreaseKafkaListenerFactory"
    )
    public void consume(StockDecreaseRequestedKafkaMessage event, Acknowledgment ack) {
        try {

            event.getItems().forEach(item -> stockService.decrease(
                    DecreaseStockCommand.of(item.productId(), item.size(), item.quantity())
            ));

            log.info("[KafkaConsumer] 재고 차감 성공 - orderId={}", event.getOrderId());
            ack.acknowledge();
        } catch (Exception e) {
            log.warn("[KafkaConsumer] 재고 차감 실패 - orderId={}, reason={}", event.getOrderId(), e.getMessage());

            failureProducer.send(StockDecreaseFailedKafkaMessage.from(event, e.getMessage()));
        }
    }
}
