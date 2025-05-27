package kr.hhplus.be.server.domain.order;


import java.time.LocalDate;
import java.util.Optional;

public interface OrderItemRepository {
    Optional<Long> findLatestPriceByProductIdAndDate(Long productId, LocalDate orderDate);
}
