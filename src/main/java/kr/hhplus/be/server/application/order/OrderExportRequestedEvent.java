package kr.hhplus.be.server.application.order;

import kr.hhplus.be.server.domain.common.AbstractDomainEvent;
import kr.hhplus.be.server.domain.orderexport.OrderExportPayload;
import lombok.Getter;

@Getter
public class OrderExportRequestedEvent extends AbstractDomainEvent {

    private final OrderExportPayload payload;

    public OrderExportRequestedEvent(OrderExportPayload payload) {
        super(payload.getOrderId(),"주문이 CONFIRMED 상태가 되어 외부 데이터 플랫폼 전송이 요청됨");
        this.payload = payload;
    }

    @Override
    public String getEventType() {
        return "OrderExportRequestedEvent";
    }

    @Override
    public String getEventDescription() {
        return "주문이 CONFIRMED 상태가 되어 외부 데이터 플랫폼 전송이 요청됨";
    }
}
