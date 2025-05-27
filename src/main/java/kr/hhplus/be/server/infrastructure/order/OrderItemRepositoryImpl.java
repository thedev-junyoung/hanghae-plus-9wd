package kr.hhplus.be.server.infrastructure.order;

import kr.hhplus.be.server.domain.order.OrderItemRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class OrderItemRepositoryImpl implements OrderItemRepository {

    private final OrderItemJpaRepository orderItemJpaRepository;

    @Override
    public Optional<Long> findLatestPriceByProductIdAndDate(Long productId, LocalDate orderDate) {
        return orderItemJpaRepository.findLatestPriceByProductIdAndDate(productId, orderDate);
    }
}
