package kr.hhplus.be.server.application.order;

import kr.hhplus.be.server.domain.common.AbstractDomainEvent;
import lombok.Getter;

@Getter
public class OrderConfirmationFailedEvent extends AbstractDomainEvent {

    private final String reason;

    public OrderConfirmationFailedEvent(String orderId, String reason) {
        super(orderId, "주문 상태 CONFIRMED 변경 실패");
        this.reason = reason;
    }


}
