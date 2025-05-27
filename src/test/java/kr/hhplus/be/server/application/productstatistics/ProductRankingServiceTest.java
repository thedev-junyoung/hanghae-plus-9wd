package kr.hhplus.be.server.application.productstatistics;

import kr.hhplus.be.server.application.productstatistics.strategy.ProductRankingStrategy;
import kr.hhplus.be.server.infrastructure.redis.ProductRankRedisRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.mockito.Mockito.*;

class ProductRankingServiceTest {

    ProductRankingStrategy daily;
    ProductRankingStrategy weekly;
    ProductRankingStrategy monthly;

    ProductRankingService service;

    ProductRankRedisRepository productRankRedisRepository;

    @BeforeEach
    void setUp() {
        daily = mock(ProductRankingStrategy.class);
        weekly = mock(ProductRankingStrategy.class);
        monthly = mock(ProductRankingStrategy.class);
        productRankRedisRepository = mock(ProductRankRedisRepository.class);
        service = new ProductRankingService(daily,weekly,monthly,productRankRedisRepository);
    }

    @Test
    @DisplayName("record 메서드가 호출되면 모든 전략에 대해 record 메서드가 호출된다.")
    void record_should_delegate_to_all_strategies() {
        // given
        Long productId = 1L;
        int quantity = 5;

        // when
        service.record(productId, quantity);

        // then
        verify(daily).record(productId, quantity);
        verify(weekly).record(productId, quantity);
        verify(monthly).record(productId, quantity);
    }

    @Test
    @DisplayName("record 메서드가 호출되면 전략이 없을 경우에도 예외가 발생하지 않는다.")
    void record_should_work_even_if_no_strategies() {
        // given
        ProductRankingService emptyService = new ProductRankingService(daily, weekly, monthly, productRankRedisRepository);

        // when
        emptyService.record(1L, 10);

        // then
        // 예외 발생하지 않으면 성공
    }
}
