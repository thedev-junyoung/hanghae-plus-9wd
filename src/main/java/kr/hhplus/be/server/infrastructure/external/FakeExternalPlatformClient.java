package kr.hhplus.be.server.infrastructure.external;

import kr.hhplus.be.server.domain.orderexport.OrderExportPayload;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

@Getter
@Slf4j
@Component
public class FakeExternalPlatformClient implements ExternalPlatformClient {
    private final List<String> receivedOrderIds = new CopyOnWriteArrayList<>();

    @Override
    public void sendOrder(OrderExportPayload payload) {
        log.info("[FAKE] 주문 전송 요청: {}", payload);
        receivedOrderIds.add(payload.getOrderId());
        log.info("[FAKE] 외부 플랫폼으로 주문 전송됨: {}", payload);
    }

    public void clear() {
        receivedOrderIds.clear();
    }
}

