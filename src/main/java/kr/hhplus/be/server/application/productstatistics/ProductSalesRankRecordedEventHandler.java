package kr.hhplus.be.server.application.productstatistics;

import kr.hhplus.be.server.domain.order.ProductSalesRankRecordedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
@RequiredArgsConstructor
@Slf4j
public class ProductSalesRankRecordedEventHandler {

    private final ProductRankingService rankingService;

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handle(ProductSalesRankRecordedEvent event) {
        log.info("[Product] 상품 판매 기록 이벤트 수신 - orderId={}", event.getAggregateId());

        try {
            for (ProductSalesRankRecordedEvent.ProductQuantity item : event.getItems()) {
                rankingService.record(item.productId(), item.quantity());
            }
            log.info("[Product] 상품 판매 기록 완료 - orderId={}", event.getAggregateId());
        } catch (Exception e) {
            log.error("[Product] 상품 판매 기록 실패 - orderId={}, error={}", event.getAggregateId(), e.getMessage());

            for (ProductSalesRankRecordedEvent.ProductQuantity item : event.getItems()) {
                rankingService.rollback(item.productId(), item.quantity());
            }
        }
    }


}
