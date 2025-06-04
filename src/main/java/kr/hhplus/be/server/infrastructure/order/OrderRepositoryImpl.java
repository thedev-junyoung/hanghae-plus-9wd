package kr.hhplus.be.server.infrastructure.order;

import kr.hhplus.be.server.domain.order.Order;
import kr.hhplus.be.server.domain.order.OrderRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class OrderRepositoryImpl implements OrderRepository {

    private final OrderJpaRepository jpaRepository;

    @Override
    public Order save(Order order) {
        return jpaRepository.save(order);
    }

    @Override
    public Optional<Order> findById(String orderId) {
        return jpaRepository.findById(orderId);
    }

    @Override
    public long count() {
        return jpaRepository.count();
    }

    @Override
    public Collection<Order> findAll() {
        return jpaRepository.findAll();
    }

    @Override
    public Optional<Order> findByIdForUpdate(String orderId) {
        return jpaRepository.findByIdForUpdate(orderId);
    }

    @Override
    public Optional<Order> findByIdWithItems(String orderId) {
        return jpaRepository.findByIdWithItems(orderId);
    }

    @Override
    public void flush() {
        jpaRepository.flush();
    }

    @Override
    public List<Order> findAllWithItems() {
        return jpaRepository.findAllWithItems();
    }
}
