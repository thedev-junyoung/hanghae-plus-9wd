package kr.hhplus.be.server.application.orderexport;

import kr.hhplus.be.server.domain.orderexport.OrderExportPayload;
import kr.hhplus.be.server.infrastructure.external.ExternalPlatformClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class OrderExportService implements OrderExportUseCase {

    private final ExternalPlatformClient platformClient;

    @Override
    public void export(OrderExportPayload payload) {
        log.info("[OrderExport] 외부 플랫폼으로 주문 전송 - orderId={}", payload.toString());
        platformClient.sendOrder(payload);
    }
}
