package kr.hhplus.be.server.application.order;

import kr.hhplus.be.server.application.coupon.CouponUseCase;
import kr.hhplus.be.server.common.vo.Money;
import kr.hhplus.be.server.domain.order.Order;
import kr.hhplus.be.server.domain.order.OrderItem;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class OrderProcessingService {

    private final OrderItemCreator orderItemCreator;
    private final CouponUseCase couponUseCase;
    private final OrderUseCase orderService;

    public Order process(CreateOrderCommand command) {
        // 1. OrderItem 생성 (순수 기능)
        List<OrderItem> orderItems = orderItemCreator.create(command.items());

        // 2. 쿠폰 할인 적용
        Money discountedTotal = couponUseCase.calculateDiscountedTotal(
                ApplyDiscountCommand.of(
                    command.userId(),
                    command.couponCode(),
                    orderItems
        ));

        // 3. 주문 생성
        return orderService.createOrder(command.userId(), orderItems, discountedTotal);
    }
}
