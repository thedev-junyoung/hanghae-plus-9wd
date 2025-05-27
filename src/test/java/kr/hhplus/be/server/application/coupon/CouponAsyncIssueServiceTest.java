package kr.hhplus.be.server.application.coupon;

import kr.hhplus.be.server.domain.coupon.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

class CouponAsyncIssueServiceTest {

    private CouponRepository couponRepository;
    private CouponIssueRepository couponIssueRepository;
    private Clock clock;

    private CouponAsyncIssueService service;

    @BeforeEach
    void setUp() {
        couponRepository = mock(CouponRepository.class);
        couponIssueRepository = mock(CouponIssueRepository.class);
        clock = Clock.fixed(Instant.parse("2025-05-16T00:00:00Z"), ZoneId.of("Asia/Seoul"));
        service = new CouponAsyncIssueService(couponRepository, couponIssueRepository, clock);
    }

    @Test
    @DisplayName("정상적으로 쿠폰이 발급되면 remainingQuantity가 감소하고 저장된다")
    void shouldIssueCouponSuccessfully() {
        // given
        Map<Object, Object> record = Map.of(
                "userId", "1",
                "couponCode", "WELCOME10",
                "requestId", "req-123"
        );

        Coupon coupon = Coupon.createLimitedFixed(
                "WELCOME10", 1000, 10,
                LocalDateTime.of(2025, 1, 1, 0, 0),
                LocalDateTime.of(2025, 12, 31, 23, 59)
        );
        when(couponRepository.findByCode("WELCOME10")).thenReturn(Optional.of(coupon));
        when(couponIssueRepository.hasIssued(1L, coupon.getId())).thenReturn(false);

        // when
        service.processAsync(record);

        // then
        verify(couponIssueRepository).save(any(CouponIssue.class));
        assertThat(coupon.getRemainingQuantity()).isEqualTo(9);
    }

    @Test
    @DisplayName("이미 해당 쿠폰을 발급받은 사용자는 예외가 발생하고 저장되지 않는다")
    void shouldThrowWhenAlreadyIssued() {
        // given
        Map<Object, Object> record = Map.of(
                "userId", "2",
                "couponCode", "WELCOME10",
                "requestId", "req-456"
        );

        Coupon coupon = Coupon.createLimitedFixed(
                "WELCOME10", 1000, 5,
                LocalDateTime.of(2025, 1, 1, 0, 0),
                LocalDateTime.of(2025, 12, 31, 23, 59)
        );
        when(couponRepository.findByCode("WELCOME10")).thenReturn(Optional.of(coupon));
        when(couponIssueRepository.hasIssued(2L, coupon.getId())).thenReturn(true);

        // when & then
        assertThatThrownBy(() -> service.processAsync(record))
                .isInstanceOf(CouponException.AlreadyIssuedException.class);
        verify(couponIssueRepository, never()).save(any());
    }

    @Test
    @DisplayName("존재하지 않는 쿠폰코드로 요청하면 예외가 발생한다")
    void shouldThrowWhenCouponNotFound() {
        // given
        Map<Object, Object> record = Map.of(
                "userId", "3",
                "couponCode", "INVALID",
                "requestId", "req-789"
        );

        when(couponRepository.findByCode("INVALID")).thenReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> service.processAsync(record))
                .isInstanceOf(CouponException.NotFoundException.class);
    }

    @Test
    @DisplayName("유효기간이 지난 쿠폰은 발급되지 않고 예외가 발생한다")
    void shouldThrowWhenCouponIsExpired() {
        // given
        Map<Object, Object> record = Map.of(
                "userId", "4",
                "couponCode", "EXPIRED",
                "requestId", "req-001"
        );

        Coupon expiredCoupon = Coupon.createLimitedFixed(
                "EXPIRED", 1000, 10,
                LocalDateTime.of(2024, 1, 1, 0, 0),
                LocalDateTime.of(2024, 12, 31, 23, 59)
        );
        when(couponRepository.findByCode("EXPIRED")).thenReturn(Optional.of(expiredCoupon));
        when(couponIssueRepository.hasIssued(4L, expiredCoupon.getId())).thenReturn(false);

        // when & then
        assertThatThrownBy(() -> service.processAsync(record))
                .isInstanceOf(CouponException.ExpiredException.class);
        verify(couponIssueRepository, never()).save(any());
    }

    @Test
    @DisplayName("쿠폰 수량이 소진된 경우 예외가 발생한다")
    void shouldThrowWhenCouponIsExhausted() {
        // given
        Map<Object, Object> record = Map.of(
                "userId", "5",
                "couponCode", "SOLDOUT",
                "requestId", "req-002"
        );

        Coupon exhaustedCoupon = Coupon.createLimitedFixed(
                "SOLDOUT", 1000, 0,
                LocalDateTime.of(2025, 1, 1, 0, 0),
                LocalDateTime.of(2025, 12, 31, 23, 59)
        );
        when(couponRepository.findByCode("SOLDOUT")).thenReturn(Optional.of(exhaustedCoupon));
        when(couponIssueRepository.hasIssued(5L, exhaustedCoupon.getId())).thenReturn(false);

        // when & then
        assertThatThrownBy(() -> service.processAsync(record))
                .isInstanceOf(CouponException.AlreadyExhaustedException.class);
        verify(couponIssueRepository, never()).save(any());
    }
}
