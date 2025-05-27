package kr.hhplus.be.server.application.orderexport;

import kr.hhplus.be.server.domain.orderexport.OrderExportPayload;

public interface OrderExportUseCase {
    void export(OrderExportPayload payload);
}
