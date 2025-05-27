package kr.hhplus.be.server.application.coupon;

public record IssueLimitedCouponCommand(
        Long userId,
        String couponCode,
        String requestId
) {
    public static IssueLimitedCouponCommand of(Long userId, String couponCode) {
        return new IssueLimitedCouponCommand(userId, couponCode, generateRequestId(userId, couponCode));
    }

    private static String generateRequestId(Long userId, String couponCode) {
        return userId + ":" + couponCode + ":" + System.currentTimeMillis();
    }
}
