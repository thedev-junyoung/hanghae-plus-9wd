package kr.hhplus.be.server.application.order;

import kr.hhplus.be.server.domain.order.OrderItem;

import java.util.List;

public record ApplyDiscountCommand(
        Long userId,
        String couponCode,
        List<OrderItem> orderItems
) {
    public static ApplyDiscountCommand of(Long userId, String couponCode, List<OrderItem> orderItems) {
        return new ApplyDiscountCommand(userId, couponCode, orderItems);
    }

    public boolean hasCouponCode() {
        return couponCode != null && !couponCode.isBlank();
    }
}
