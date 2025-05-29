package kr.hhplus.be.server.application.order;

import kr.hhplus.be.server.application.product.*;
import kr.hhplus.be.server.common.lock.DistributedLock;
import kr.hhplus.be.server.domain.order.Order;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;


@Service
@RequiredArgsConstructor
@Slf4j
public class OrderFacadeService {

    private final StockService stockService;
    private final OrderProcessingService orderProcessingService;
    private final OrderCompensationService compensationService;

//    @DistributedLock(
//            prefix = "lock:order:create:",
//            key = "#command.userId",
//            waitTime = 5,
//            leaseTime = 3
//    )
    public OrderResult createOrder(CreateOrderCommand command) {
        Order order = null;

        try {
            // 1. 재고 차감 (side effect)
//            for (var item : command.items()) {
//                stockService.decrease(DecreaseStockCommand.of(item.productId(), item.size(), item.quantity()));
//            }

            // 2. 주문 처리 (순수 계산 + 저장)
            order = orderProcessingService.process(command);

            return OrderResult.from(order);

        // TODO : 보상 트랜잭션 처리를 CompensationService -> 이벤트 핸들러로 이동
        } catch (Exception e) {
            log.error("주문 실패 → 보상 트랜잭션 수행 시작", e);
            compensationService.compensateStock(command.items());

            if (order != null) {
                compensationService.markOrderAsFailed(order.getId());
            }

            throw e;
        }
    }
}
