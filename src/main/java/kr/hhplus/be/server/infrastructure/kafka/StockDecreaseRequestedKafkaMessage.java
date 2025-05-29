package kr.hhplus.be.server.infrastructure.kafka;

import kr.hhplus.be.server.application.order.StockDecreaseRequested;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class StockDecreaseRequestedKafkaMessage {
    private String orderId;
    private Long userId;
    private List<Item> items;

    public record Item(Long productId, int size, int quantity) {
        public static StockDecreaseRequestedKafkaMessage of(Long userId, List<Item> items) {
            return new StockDecreaseRequestedKafkaMessage(null, userId, items);
        }
    }

    public static StockDecreaseRequestedKafkaMessage from(StockDecreaseRequested event) {
        List<Item> itemDtos = event.getItems().stream()
                .map(i -> new Item(i.getProductId(), i.getSize(), i.getQuantity()))
                .toList();

        StockDecreaseRequestedKafkaMessage message = new StockDecreaseRequestedKafkaMessage();
        message.orderId = event.getOrderId();
        message.userId = event.getUserId();
        message.items = itemDtos;
        return message;
    }
}
