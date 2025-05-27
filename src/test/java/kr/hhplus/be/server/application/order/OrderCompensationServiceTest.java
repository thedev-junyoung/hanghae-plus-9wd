package kr.hhplus.be.server.application.order;

import kr.hhplus.be.server.application.product.IncreaseStockCommand;
import kr.hhplus.be.server.application.product.StockService;
import kr.hhplus.be.server.domain.order.Order;
import kr.hhplus.be.server.domain.order.OrderRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static kr.hhplus.be.server.application.order.CreateOrderCommand.OrderItemCommand;
import static org.mockito.Mockito.*;

class OrderCompensationServiceTest {

    private StockService stockService;
    private OrderRepository orderRepository;
    private OrderCompensationService compensationService;

    @BeforeEach
    void setUp() {
        stockService = mock(StockService.class);
        orderRepository = mock(OrderRepository.class);
        compensationService = new OrderCompensationService(stockService, orderRepository);
    }

    @Test
    @DisplayName("compensateStock()은 각 아이템에 대해 stockService.increase()를 호출한다")
    void compensateStock_shouldCallIncreasePerItem() {
        // given
        OrderItemCommand item1 = new OrderItemCommand(1L, 2, 270);
        OrderItemCommand item2 = new OrderItemCommand(2L, 1, 280);
        List<OrderItemCommand> items = List.of(item1, item2);

        // when
        compensationService.compensateStock(items);

        // then
        verify(stockService).increase(IncreaseStockCommand.of(1L, 270, 2));
        verify(stockService).increase(IncreaseStockCommand.of(2L, 280, 1));
    }

    @Test
    @DisplayName("markOrderAsFailed()는 주문을 취소하고 저장한다")
    void markOrderAsFailed_shouldCancelAndSaveOrder() {
        // given
        String orderId = "ORDER123";
        Order order = mock(Order.class);

        when(orderRepository.findById(orderId)).thenReturn(Optional.of(order));

        // when
        compensationService.markOrderAsFailed(orderId);

        // then
        verify(order).cancel();
        verify(orderRepository).save(order);
    }

    @Test
    @DisplayName("markOrderAsFailed()는 주문이 없으면 예외를 던진다")
    void markOrderAsFailed_shouldThrowIfOrderNotFound() {
        // given
        String orderId = "NOT_FOUND";
        when(orderRepository.findById(orderId)).thenReturn(Optional.empty());

        // when / then
        org.junit.jupiter.api.Assertions.assertThrows(IllegalStateException.class, () ->
                compensationService.markOrderAsFailed(orderId)
        );
    }
}
