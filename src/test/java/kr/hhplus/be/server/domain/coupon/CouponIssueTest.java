package kr.hhplus.be.server.domain.coupon;

import kr.hhplus.be.server.common.vo.Money;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;

import static org.assertj.core.api.Assertions.*;

class CouponIssueTest {

    private final String code = "TEST10";
    private final long userId = 1L;
    private final Clock fixedClock = Clock.fixed(
            Instant.parse("2025-04-27T00:00:00Z"), ZoneId.of("UTC")
    );

    @Test
    @DisplayName("쿠폰 발급 시 수량이 차감되고 발급 시간이 고정된 시간으로 저장된다")
    void create_shouldDecreaseQuantityAndSetIssuedAt() {
        Coupon coupon = createValidCoupon();

        CouponIssue issue = CouponIssue.createAndValidateDecreaseQuantity(userId, coupon, fixedClock);

        assertThat(issue.getUserId()).isEqualTo(userId);
        assertThat(issue.getCoupon()).isEqualTo(coupon);
        assertThat(issue.isUsed()).isFalse();
        assertThat(issue.getIssuedAt()).isEqualTo(LocalDateTime.of(2025, 4, 27, 0, 0));
        assertThat(coupon.getRemainingQuantity()).isEqualTo(9);
    }

    @Test
    @DisplayName("markAsUsed 호출 시 isUsed가 true가 된다")
    void markAsUsed_shouldSetIsUsedTrue() {
        Coupon coupon = createValidCoupon();
        CouponIssue issue = CouponIssue.create(userId, coupon, fixedClock);

        issue.markAsUsed();

        assertThat(issue.isUsed()).isTrue();
    }

    @Test
    @DisplayName("이미 사용한 쿠폰에 markAsUsed 호출 시 예외 발생")
    void markAsUsed_shouldThrowIfAlreadyUsed() {
        Coupon coupon = createValidCoupon();
        CouponIssue issue = CouponIssue.create(userId, coupon, fixedClock);
        issue.markAsUsed();

        assertThatThrownBy(issue::markAsUsed)
                .isInstanceOf(CouponException.AlreadyIssuedException.class);
    }

    @Test
    @DisplayName("만료된 쿠폰 발급 시 CouponException 발생")
    void create_shouldThrowIfCouponExpired() {
        Coupon expiredCoupon = Coupon.create(code, CouponType.FIXED, 1000, 10,
                LocalDateTime.now(fixedClock).minusDays(10),
                LocalDateTime.now(fixedClock).minusDays(1));

        assertThatThrownBy(() -> CouponIssue.createAndValidateDecreaseQuantity(userId, expiredCoupon, fixedClock))
                .isInstanceOf(CouponException.ExpiredException.class);
    }

    @Test
    @DisplayName("정상 발급된 쿠폰은 할인 금액을 계산할 수 있다")
    void calculateDiscount_should_return_correct_discount() {
        Coupon coupon = Coupon.create(code, CouponType.FIXED, 2000, 10,
                LocalDateTime.now(fixedClock).minusDays(1),
                LocalDateTime.now(fixedClock).plusDays(1));
        CouponIssue issue = CouponIssue.create(userId, coupon, fixedClock);

        Money orderAmount = Money.wons(10000);
        Money discount = issue.calculateDiscount(orderAmount);

        assertThat(discount).isEqualTo(Money.wons(2000));
    }
    @Test
    @DisplayName("유효한 쿠폰은 validateUsable() 호출 시 예외가 발생하지 않는다")
    void validateUsable_should_not_throw_for_valid_coupon() {
        Coupon coupon = createValidCoupon();
        CouponIssue issue = CouponIssue.create(userId, coupon, fixedClock);

        // when + then (예외 발생 없음)
        assertThatCode(() -> issue.validateUsable(fixedClock)).doesNotThrowAnyException();
    }


    private Coupon createValidCoupon() {
        return Coupon.create(code, CouponType.FIXED, 1000, 10,
                LocalDateTime.now(fixedClock).minusDays(1),
                LocalDateTime.now(fixedClock).plusDays(1));
    }
}
