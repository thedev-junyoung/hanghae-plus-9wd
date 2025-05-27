package kr.hhplus.be.server.application.order;

import kr.hhplus.be.server.application.orderexport.OrderExportUseCase;
import kr.hhplus.be.server.domain.orderexport.OrderExportPayload;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Slf4j
@Component
@RequiredArgsConstructor
public class OrderExportEventHandler {

    private final OrderExportUseCase orderExportService;


    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handle(OrderExportRequestedEvent event) {
        String orderId = event.getAggregateId();
        log.info("[OrderExport] 외부 플랫폼 전송 요청 - orderId={}", orderId);
        try {
            orderExportService.export(OrderExportPayload.from(event));  // 추후 export(OrderExportCommand)로 바꾸는 것도 추천
            log.info("[OrderExport] 외부 플랫폼 전송 완료 - orderId={}", orderId);
        } catch (Exception e) {
            log.error("[OrderExport] 전송 실패 - orderId={}", orderId, e);
            // TODO: DLQ or Slack 알림 등 장애 대응
        }
    }
}
