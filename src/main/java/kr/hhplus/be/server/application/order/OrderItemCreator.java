package kr.hhplus.be.server.application.order;

import kr.hhplus.be.server.application.product.GetProductDetailCommand;
import kr.hhplus.be.server.application.product.ProductDetailResult;
import kr.hhplus.be.server.application.product.ProductUseCase;
import kr.hhplus.be.server.common.vo.Money;
import kr.hhplus.be.server.domain.order.OrderItem;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
public class OrderItemCreator {

    private final ProductUseCase productService;

    public List<OrderItem> create(List<CreateOrderCommand.OrderItemCommand> commands) {
        return commands.stream()
                .map(command -> {
                    Money unitPrice = productService.getPrice(command.productId());
                    return OrderItem.of(command.productId(), command.quantity(), command.size(), unitPrice);
                })
                .toList();
    }
}
