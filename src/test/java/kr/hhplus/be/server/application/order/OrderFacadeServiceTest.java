package kr.hhplus.be.server.application.order;

import kr.hhplus.be.server.application.product.DecreaseStockCommand;
import kr.hhplus.be.server.application.product.StockService;
import kr.hhplus.be.server.common.vo.Money;
import kr.hhplus.be.server.domain.order.Order;
import kr.hhplus.be.server.domain.order.OrderItem;
import kr.hhplus.be.server.domain.order.OrderStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.context.ApplicationEventPublisher;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

class OrderFacadeServiceTest {

    private StockService stockService;
    private OrderProcessingService orderProcessingService;
    private OrderCompensationService compensationService;

    private OrderFacadeService orderFacadeService;
    @BeforeEach
    void setUp() {
        stockService = mock(StockService.class);
        orderProcessingService = mock(OrderProcessingService.class);
        compensationService = mock(OrderCompensationService.class);

        orderFacadeService = new OrderFacadeService(
                stockService,
                orderProcessingService,
                compensationService
        );
    }

    @Test
    @DisplayName("정상적으로 주문을 생성하고 이벤트를 발행한다")
    void createOrder_success() {
        // given
        Long userId = 1L;
        Long productId = 1001L;
        int quantity = 2;
        int size = 270;
        long unitPrice = 5000L;
        String couponCode = "COUPON10";

        CreateOrderCommand.OrderItemCommand itemCommand = new CreateOrderCommand.OrderItemCommand(productId, quantity, size);
        CreateOrderCommand command = new CreateOrderCommand(userId, List.of(itemCommand), couponCode);

        List<OrderItem> orderItems = List.of(OrderItem.of(productId, quantity, size, Money.wons(unitPrice)));
        Money discountedTotal = Money.wons(unitPrice * quantity - 2000);
        Order order = Order.create(userId, orderItems, discountedTotal);

        when(orderProcessingService.process(command)).thenReturn(order);

        // when
        OrderResult result = orderFacadeService.createOrder(command);

        // then
        assertThat(result).isNotNull();
        assertThat(result.userId()).isEqualTo(userId);
        assertThat(result.totalAmount()).isEqualTo(discountedTotal.value());
        assertThat(result.items()).hasSize(1);
        assertThat(result.status()).isEqualTo(OrderStatus.CREATED);

        // verify: 재고 차감, 주문 생성, 이벤트 발행 호출 확인
//        verify(stockService).decrease(DecreaseStockCommand.of(productId, size, quantity));
        verify(orderProcessingService).process(command);
        verifyNoInteractions(compensationService);
    }

    @DisplayName("주문 생성 중 예외 발생 시 보상 트랜잭션을 수행한다")
    @Test
    void createOrder_whenException_thenTriggerCompensation() {
        // given
        Long userId = 1L;
        Long productId = 1001L;
        int quantity = 1;
        int size = 270;
        CreateOrderCommand.OrderItemCommand item = new CreateOrderCommand.OrderItemCommand(productId, quantity, size);
        CreateOrderCommand command = new CreateOrderCommand(userId, List.of(item), null);

        // 재고는 차감되었지만, 주문 생성 중 예외
        when(orderProcessingService.process(command)).thenThrow(new RuntimeException("예외 발생"));

        // when / then
        org.junit.jupiter.api.Assertions.assertThrows(RuntimeException.class, () ->
                orderFacadeService.createOrder(command)
        );

        // then
//        verify(stockService).decrease(any());
        verify(compensationService).compensateStock(command.items());
        verify(compensationService, never()).markOrderAsFailed(any()); // order == null
    }
}
