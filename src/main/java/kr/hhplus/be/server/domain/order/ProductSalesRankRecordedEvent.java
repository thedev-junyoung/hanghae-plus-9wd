package kr.hhplus.be.server.domain.order;

import kr.hhplus.be.server.domain.common.AbstractDomainEvent;
import kr.hhplus.be.server.domain.common.DomainEvent;
import lombok.Getter;

import java.util.List;

/**
 * 주문 확정 시, 상품별 판매 수량을 기록하는 도메인 이벤트
 */
@Getter
public class ProductSalesRankRecordedEvent extends AbstractDomainEvent {

    private final List<ProductQuantity> items;

    public ProductSalesRankRecordedEvent(String orderId, List<ProductQuantity> items) {
        super(orderId, "상품 판매량 기록됨");
        this.items = items;
    }

    public static DomainEvent of(String id, List<ProductQuantity> products) {
        return new ProductSalesRankRecordedEvent(id, products);
    }

    public record ProductQuantity(long productId, int quantity) {
        public static ProductQuantity of(long productId, int quantity) {
            return new ProductQuantity(productId, quantity);
        }
    }
}
