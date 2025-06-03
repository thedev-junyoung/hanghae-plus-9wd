package kr.hhplus.be.server.application.coupon;

import kr.hhplus.be.server.common.vo.Money;
import kr.hhplus.be.server.domain.coupon.*;
import kr.hhplus.be.server.infrastructure.redis.CouponIssueStreamPublisher;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class CouponServiceTest {

    @Mock
    private CouponRepository couponRepository;

    @Mock
    private CouponIssueRepository couponIssueRepository;

    private CouponService couponService;

    private Clock fixedClock;

    @Mock
    private ApplicationEventPublisher eventPublisher;

    @Mock
    CouponIssueStreamPublisher couponIssueStreamPublisher;

    private final String couponCode = "TEST10";
    private final long userId = 1L;

    @BeforeEach
    void setUp() {
        fixedClock = Clock.fixed(Instant.parse("2025-04-27T00:00:00Z"), ZoneId.of("UTC"));

        couponService = new CouponService(couponRepository, couponIssueRepository, fixedClock, couponIssueStreamPublisher, eventPublisher);

    }

    @Test
    @DisplayName("쿠폰 정상 발급 성공")
    void issueCoupon_success() {
        Coupon coupon = createValidCoupon();
        IssueLimitedCouponCommand command = new IssueLimitedCouponCommand(userId, couponCode, null);

        given(couponRepository.findByCode(couponCode)).willReturn(Optional.of(coupon));
        given(couponIssueRepository.hasIssued(userId, coupon.getId())).willReturn(false);

        CouponResult result = couponService.issueLimitedCoupon(command);

        assertThat(result).isNotNull();
        assertThat(result.userId()).isEqualTo(userId);

        ArgumentCaptor<CouponIssue> captor = ArgumentCaptor.forClass(CouponIssue.class);
        verify(couponIssueRepository).save(captor.capture());

        CouponIssue saved = captor.getValue();
        assertThat(saved.getUserId()).isEqualTo(userId);
        assertThat(saved.getCoupon()).isEqualTo(coupon);
    }

    @Test
    @DisplayName("이미 쿠폰을 발급받은 경우 예외 발생")
    void issueCoupon_fail_ifAlreadyIssued() {
        Coupon coupon = createValidCoupon();
        given(couponRepository.findByCode(couponCode)).willReturn(Optional.of(coupon));
        given(couponIssueRepository.hasIssued(userId, coupon.getId())).willReturn(true);

        assertThrows(CouponException.AlreadyIssuedException.class, () ->
                couponService.issueLimitedCoupon(new IssueLimitedCouponCommand(userId, couponCode, null)));
    }

    @Test
    @DisplayName("만료된 쿠폰일 경우 예외 발생")
    void issueCoupon_fail_ifExpired() {
        LocalDateTime now = LocalDateTime.now(fixedClock);
        Coupon expiredCoupon = Coupon.create(
                couponCode,
                CouponType.FIXED,
                1000,
                10,
                now.minusDays(10),
                now.minusDays(1)
        );

        given(couponRepository.findByCode(couponCode)).willReturn(Optional.of(expiredCoupon));


        assertThrows(CouponException.ExpiredException.class, () ->
                couponService.issueLimitedCoupon(new IssueLimitedCouponCommand(userId, couponCode, null))
        );
    }

    @Test
    @DisplayName("수량 소진된 쿠폰일 경우 예외 발생")
    void issueCoupon_fail_ifSoldOut() {
        LocalDateTime now = LocalDateTime.now(fixedClock);
        Coupon soldOutCoupon = new Coupon(
                couponCode,
                CouponType.PERCENTAGE,
                20,
                10,
                0, // 재고 0
                now.minusDays(1),
                now.plusDays(1)
        );

        given(couponRepository.findByCode(couponCode)).willReturn(Optional.of(soldOutCoupon));


        assertThrows(CouponException.AlreadyExhaustedException.class, () ->
                couponService.issueLimitedCoupon(new IssueLimitedCouponCommand(userId, couponCode, null))
        );
    }

    @Test
    @DisplayName("쿠폰 적용 성공")
    void applyCoupon_success() {
        Coupon coupon = createValidCoupon();
        Money orderAmount = Money.wons(10000);
        Money expectedDiscount = coupon.calculateDiscount(orderAmount);

        CouponIssue issue = CouponIssue.create(userId, coupon, fixedClock);

        given(couponRepository.findByCode(couponCode)).willReturn(Optional.of(coupon));
        given(couponIssueRepository.findByUserIdAndCouponId(userId, coupon.getId()))
                .willReturn(java.util.Optional.of(issue));

        ApplyCouponResult result = couponService.applyCoupon(
                new ApplyCouponCommand(userId, couponCode, orderAmount)
        );

        assertThat(result.couponCode()).isEqualTo(couponCode);
        assertThat(result.discountAmount()).isEqualTo(expectedDiscount);
    }

    @Test
    @DisplayName("쿠폰 미발급 상태에서 쿠폰 적용 시 예외 발생")
    void applyCoupon_fail_ifNotIssued() {
        Coupon coupon = createValidCoupon();

        given(couponRepository.findByCode(couponCode)).willReturn(Optional.of(coupon));
        given(couponIssueRepository.findByUserIdAndCouponId(userId, coupon.getId()))
                .willReturn(java.util.Optional.empty());

        assertThrows(CouponException.NotIssuedException.class, () ->
                couponService.applyCoupon(new ApplyCouponCommand(userId, couponCode, Money.wons(10000))));
    }

    @Test
    @DisplayName("등록된 모든 쿠폰 목록을 조회한다")
    void getAllCoupons_shouldReturnAllRegisteredCoupons() {
        // given
        Coupon coupon1 = createValidCoupon("WELCOME10");
        Coupon coupon2 = createValidCoupon("FLAT5000");

        given(couponRepository.findAll()).willReturn(List.of(coupon1, coupon2));

        // when
        List<String> couponList = couponService.findAllCouponCodes();

        // then
        verify(couponRepository).findAll();
        assertThat(couponList)
                .containsExactlyInAnyOrder("WELCOME10", "FLAT5000");
    }


    private Coupon createValidCoupon() {
        LocalDateTime now = LocalDateTime.now(fixedClock);
        return Coupon.create(
                couponCode,
                CouponType.PERCENTAGE,
                10,
                100,
                now.minusDays(1),
                now.plusDays(1)
        );
    }

    private Coupon createValidCoupon(String code) {
        return Coupon.createLimitedFixed(
                code,
                1000,
                100,
                LocalDateTime.now(),
                LocalDateTime.now().plusDays(30)
        );
    }
}

