package kr.hhplus.be.server.domain.coupon;


import kr.hhplus.be.server.common.vo.Money;

public enum CouponType {

    FIXED {
        @Override
        public Money calculateDiscount(Money orderAmount, int rate) {
            return Money.from(rate);
        }
    },

    PERCENTAGE {
        @Override
        public Money calculateDiscount(Money orderAmount, int rate) {
            return orderAmount.multiplyPercent(rate);
        }
    };

    public abstract Money calculateDiscount(Money orderAmount, int rate);
}
