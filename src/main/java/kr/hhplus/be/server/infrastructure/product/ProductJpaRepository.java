package kr.hhplus.be.server.infrastructure.product;

import kr.hhplus.be.server.domain.product.Product;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface ProductJpaRepository extends JpaRepository<Product, Long> {
    @Query("SELECT p.price FROM Product p WHERE p.id = :productId")
    Optional<Long> findUnitPrice(@Param("productId") Long productId);
}
