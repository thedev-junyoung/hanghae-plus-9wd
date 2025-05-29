package kr.hhplus.be.server.application.order;

import kr.hhplus.be.server.domain.common.DomainEvent;
import kr.hhplus.be.server.domain.order.OrderItem;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.List;

@Getter
public class StockDecreaseRequested implements DomainEvent {
    private final String orderId;
    private final Long userId;
    private final List<OrderItem> items;

    public StockDecreaseRequested(String orderId, Long userId, List<OrderItem> items) {
        this.orderId = orderId;
        this.userId = userId;
        this.items = items;
    }


    @Override
    public String getId() {
        return orderId;
    }

    @Override
    public String getAggregateId() {
        return orderId;
    }

    @Override
    public LocalDateTime getOccurredAt() {
        return null;
    }

    @Override
    public String getEventType() {
        return "StockDecreaseRequested";
    }

    @Override
    public String getEventDescription() {
        return "재고 감소 요청 이벤트";
    }
}
