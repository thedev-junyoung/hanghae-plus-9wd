package kr.hhplus.be.server.infrastructure.kafka;

import kr.hhplus.be.server.application.coupon.CouponIssueRequestedEvent;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class CouponIssueKafkaMessage {
    private Long userId;
    private String couponCode;

    public static CouponIssueKafkaMessage from(CouponIssueRequestedEvent event) {
        return new CouponIssueKafkaMessage(event.getUserId(), event.getCouponCode());
    }
}
