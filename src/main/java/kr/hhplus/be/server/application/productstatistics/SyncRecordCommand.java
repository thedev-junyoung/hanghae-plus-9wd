package kr.hhplus.be.server.application.productstatistics;

import kr.hhplus.be.server.common.vo.Money;

public record SyncRecordCommand(Long productId, int quantity, Money unitPrice) {
}