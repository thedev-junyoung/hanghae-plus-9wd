package kr.hhplus.be.server.application.order;

import kr.hhplus.be.server.application.product.ProductUseCase;
import kr.hhplus.be.server.common.vo.Money;
import kr.hhplus.be.server.domain.order.OrderItem;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static kr.hhplus.be.server.application.order.CreateOrderCommand.OrderItemCommand;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

class OrderItemCreatorTest {

    private ProductUseCase productService;
    private OrderItemCreator orderItemCreator;

    @BeforeEach
    void setUp() {
        productService = mock(ProductUseCase.class);
        orderItemCreator = new OrderItemCreator(productService);
    }

    @Test
    @DisplayName("상품 ID로 가격을 조회하여 OrderItem을 생성한다")
    void createOrderItems_success() {
        // given
        Long productId = 1001L;
        int quantity = 2;
        int size = 270;
        long price = 5000L;

        OrderItemCommand command = new OrderItemCommand(productId, quantity, size);
        when(productService.getPrice(productId)).thenReturn(Money.wons(price));

        // when
        List<OrderItem> result = orderItemCreator.create(List.of(command));

        // then
        assertThat(result).hasSize(1);
        OrderItem item = result.get(0);
        assertThat(item.getProductId()).isEqualTo(productId);
        assertThat(item.getQuantity()).isEqualTo(quantity);
        assertThat(item.getSize()).isEqualTo(size);
        assertThat(item.getPrice()).isEqualTo(price);

        // verify
        verify(productService).getPrice(productId);
    }
}
