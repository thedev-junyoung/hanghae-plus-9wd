package kr.hhplus.be.server.application.balance;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;


@Slf4j
@Component
@RequiredArgsConstructor
public class BalanceHistoryEventHandler {

    private final BalanceHistoryUseCase balanceHistoryUseCase;


    /**
     * 잔액 충전 이벤트 처리.
     * 현재는 AFTER_COMMIT + @Async 방식으로 비동기 처리 중.
     * TODO: 실패(저장 실패, 중복 등) 시 재시도 또는 보상 트랜잭션 필요
     * - 메시지 큐(Kafka 등) 도입 필요
     * - 일시적 장애 발생 시 재시도 로직 추가 필요
     */

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Async
    public void handle(RecordBalanceChargeEvent event) {
        try{
            log.info("[BalanceHistoryEventHandler] AFTER_COMMIT: 잔액 충전 이력 저장 요청 - {}", event);
            balanceHistoryUseCase.recordHistory(RecordBalanceHistoryCommand.of(event));
            log.info("[BalanceHistoryEventHandler] AFTER_COMMIT: 잔액 충전 이력 저장 완료 - {}", event);
        } catch (Exception e){
            // TODO: 예외 처리 필요
            log.error("[BalanceHistoryEventHandler] AFTER_COMMIT: 잔액 충전 이력 저장 실패 - {}", event, e);
        }

    }
}

