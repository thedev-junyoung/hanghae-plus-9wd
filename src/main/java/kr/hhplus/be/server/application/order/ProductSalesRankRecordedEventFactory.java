package kr.hhplus.be.server.application.order;

import kr.hhplus.be.server.domain.order.Order;
import kr.hhplus.be.server.domain.order.ProductSalesRankRecordedEvent;

public class ProductSalesRankRecordedEventFactory {
    public static ProductSalesRankRecordedEvent from(Order order) {
        return new ProductSalesRankRecordedEvent(
                order.getId(),
                order.getItems().stream()
                        .map(item -> new ProductSalesRankRecordedEvent.ProductQuantity(item.getProductId(), item.getQuantity()))
                        .toList()
        );
    }
}
