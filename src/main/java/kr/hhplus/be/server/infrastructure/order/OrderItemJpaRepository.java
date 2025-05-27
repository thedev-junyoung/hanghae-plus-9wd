package kr.hhplus.be.server.infrastructure.order;

import kr.hhplus.be.server.domain.order.OrderItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.Optional;


public interface OrderItemJpaRepository extends JpaRepository<OrderItem, Long> {
    @Query("""
        SELECT oi.price
        FROM OrderItem oi
        JOIN oi.order o
        WHERE oi.productId = :productId
          AND DATE(o.createdAt) = :orderDate
          AND o.status = 'CONFIRMED'
        ORDER BY o.createdAt DESC
        LIMIT 1
    """)
    Optional<Long> findLatestPriceByProductIdAndDate(
            @Param("productId") Long productId,
            @Param("orderDate") LocalDate orderDate
    );
}
