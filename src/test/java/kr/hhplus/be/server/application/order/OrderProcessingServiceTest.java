package kr.hhplus.be.server.application.order;

import kr.hhplus.be.server.application.coupon.CouponUseCase;
import kr.hhplus.be.server.common.vo.Money;
import kr.hhplus.be.server.domain.order.Order;
import kr.hhplus.be.server.domain.order.OrderItem;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static kr.hhplus.be.server.application.order.CreateOrderCommand.OrderItemCommand;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

class OrderProcessingServiceTest {

    private OrderItemCreator orderItemCreator;
    private CouponUseCase couponUseCase;
    private OrderUseCase orderService;

    private OrderProcessingService orderProcessingService;

    @BeforeEach
    void setUp() {
        orderItemCreator = mock(OrderItemCreator.class);
        couponUseCase = mock(CouponUseCase.class);
        orderService = mock(OrderUseCase.class);

        orderProcessingService = new OrderProcessingService(
                orderItemCreator,
                couponUseCase,
                orderService
        );
    }

    @Test
    @DisplayName("OrderProcessingService는 OrderItem 생성, 할인 적용, 주문 생성을 처리한다")
    void process_shouldCreateOrderWithDiscountedTotal() {
        // given
        Long userId = 1L;
        Long productId = 1001L;
        int quantity = 2;
        int size = 270;
        String couponCode = "COUPON10";

        OrderItemCommand itemCommand = new OrderItemCommand(productId, quantity, size);
        CreateOrderCommand command = new CreateOrderCommand(userId, List.of(itemCommand), couponCode);

        List<OrderItem> orderItems = List.of(OrderItem.of(productId, quantity, size, Money.wons(5000)));
        Money discountedTotal = Money.wons(8000);
        Order order = Order.create(userId, orderItems, discountedTotal);

        when(orderItemCreator.create(command.items())).thenReturn(orderItems);
        when(couponUseCase.calculateDiscountedTotal(ApplyDiscountCommand.of(command.userId(),command.couponCode(),orderItems))).thenReturn(discountedTotal);
        when(orderService.createOrder(userId, orderItems, discountedTotal)).thenReturn(order);

        // when
        Order result = orderProcessingService.process(command);

        // then
        assertThat(result).isEqualTo(order);

        // verify flow
        verify(orderItemCreator).create(command.items());
        verify(couponUseCase).calculateDiscountedTotal(ApplyDiscountCommand.of(command.userId(),command.couponCode(),orderItems));
        verify(orderService).createOrder(userId, orderItems, discountedTotal);
    }
}
