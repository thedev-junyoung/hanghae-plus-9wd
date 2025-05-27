package kr.hhplus.be.server.infrastructure.redis.util;

public class CouponStreamKeyResolver {

    private static final String PREFIX = "coupon:stream:";

    public static String resolve(String couponCode) {
        return PREFIX + couponCode;
    }

    public static String dlq(String couponCode) {
        return PREFIX + couponCode + ":dlq";
    }
}
