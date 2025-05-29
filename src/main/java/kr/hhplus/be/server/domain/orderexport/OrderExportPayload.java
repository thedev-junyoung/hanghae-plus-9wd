package kr.hhplus.be.server.domain.orderexport;

import kr.hhplus.be.server.application.order.OrderExportRequestedEvent;
import kr.hhplus.be.server.domain.order.Order;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.ToString;

import java.util.List;

@Getter
@AllArgsConstructor(staticName = "of")
@NoArgsConstructor
@ToString
public class OrderExportPayload {
    private String orderId;
    private Long userId;
    private List<OrderItemPayload> items;
    private long totalAmount;

    @Getter
    @AllArgsConstructor(staticName = "of")
    @NoArgsConstructor
    @ToString
    public static class OrderItemPayload {
        private Long productId;
        private int quantity;
        private int size;
        private long price;
    }

    public static OrderExportPayload from(Order order) {
        return OrderExportPayload.of(
                order.getId(),
                order.getUserId(),
                order.getItems().stream()
                        .map(item -> OrderItemPayload.of(
                                item.getProductId(),
                                item.getQuantity(),
                                item.getSize(),
                                item.getPrice()
                        ))
                        .toList(),
                order.getTotalAmount()
        );
    }

    public static OrderExportPayload from(OrderExportRequestedEvent event) {
        return event.getPayload();
    }
}
