package kr.hhplus.be.server.application.order;

import kr.hhplus.be.server.domain.common.AbstractDomainEvent;

/**
 * 결제 성공 이후, 주문을 CONFIRMED 상태로 변경하도록 요청하는 이벤트
 * 이 이벤트는 Payment 도메인에서 결제가 성공적으로 처리된 후 발행
 */
public class OrderConfirmedEvent extends AbstractDomainEvent {

    public OrderConfirmedEvent(String orderId) {
        super(orderId, "결제 성공으로 인한 주문 상태 변경 요청");
    }
}