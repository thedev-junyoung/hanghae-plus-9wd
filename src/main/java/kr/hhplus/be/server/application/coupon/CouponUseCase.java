package kr.hhplus.be.server.application.coupon;

import kr.hhplus.be.server.application.order.ApplyDiscountCommand;
import kr.hhplus.be.server.common.vo.Money;
import kr.hhplus.be.server.domain.coupon.Coupon;

import java.util.List;

public interface CouponUseCase {

    /**
     * 선착순 쿠폰을 발급합니다.
     */
    CouponResult issueLimitedCoupon(IssueLimitedCouponCommand command);

    /**
     * 주문 시 쿠폰의 유효성을 검증하고 할인 금액을 계산합니다.
     */
    ApplyCouponResult applyCoupon(ApplyCouponCommand command);

    Money calculateDiscountedTotal(ApplyDiscountCommand command);

    void enqueueLimitedCoupon(IssueLimitedCouponCommand command);

    List<String> findAllCouponCodes();

    void requestCoupon(IssueLimitedCouponCommand issueLimitedCouponCommand);
}

