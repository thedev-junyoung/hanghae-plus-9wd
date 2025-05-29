package kr.hhplus.be.server.application.coupon;

import kr.hhplus.be.server.domain.common.DomainEvent;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.ToString;

import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@AllArgsConstructor
@ToString
public class CouponIssueRequestedEvent implements DomainEvent {
    private final Long userId;
    private final String couponCode;
    private final LocalDateTime occurredAt;
    private final String eventId;

    public CouponIssueRequestedEvent(Long userId, String couponCode, LocalDateTime occurredAt) {
        this.userId = userId;
        this.couponCode = couponCode;
        this.occurredAt = occurredAt;
        this.eventId = UUID.randomUUID().toString();
    }

    @Override
    public String getId() {
        return userId + ":" + couponCode;
    }

    @Override
    public String getAggregateId() {
        return couponCode;
    }

    @Override
    public LocalDateTime getOccurredAt() {
        return occurredAt;
    }

    @Override
    public String getEventType() {
        return "CouponIssueRequested";
    }

    @Override
    public String getEventDescription() {
        return String.format("쿠폰 발급 요청됨 - userId=%d, couponCode=%s", userId, couponCode);
    }

}
