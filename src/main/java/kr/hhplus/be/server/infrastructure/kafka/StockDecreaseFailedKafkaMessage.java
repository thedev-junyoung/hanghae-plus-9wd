package kr.hhplus.be.server.infrastructure.kafka;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;


@Builder
@NoArgsConstructor
@AllArgsConstructor
@Getter
public class StockDecreaseFailedKafkaMessage {

    private String orderId;
    private Long userId;
    private String reason;

    public static StockDecreaseFailedKafkaMessage from(StockDecreaseRequestedKafkaMessage msg, String reason) {
        return new StockDecreaseFailedKafkaMessage(msg.getOrderId(), msg.getUserId(), reason);
    }
    public static StockDecreaseFailedKafkaMessage of(String orderId, Long userId, String reason) {
        return StockDecreaseFailedKafkaMessage.builder()
                .orderId(orderId)
                .userId(userId)
                .reason(reason)
                .build();
    }
}
