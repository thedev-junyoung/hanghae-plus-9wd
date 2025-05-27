package kr.hhplus.be.server.application.balance;

import kr.hhplus.be.server.domain.common.AbstractDomainEvent;
import lombok.Getter;

@Getter
public class RecordBalanceChargeEvent extends AbstractDomainEvent {

    private final Long userId;
    private final long amount;
    private final String reason;
    private final String requestId;

    private RecordBalanceChargeEvent(Long userId, long amount, String reason, String requestId) {
        super(String.valueOf(userId), "충전 내역 기록 요청");
        this.userId = userId;
        this.amount = amount;
        this.reason = reason;
        this.requestId = requestId;
    }

    public static RecordBalanceChargeEvent of(Long userId, long amount, String reason, String requestId) {
        return new RecordBalanceChargeEvent(userId, amount, reason, requestId);
    }

}
