package kr.hhplus.be.server.application.productstatistics;

import kr.hhplus.be.server.application.product.PopularProductCriteria;
import kr.hhplus.be.server.domain.product.ProductRepository;
import kr.hhplus.be.server.domain.productstatistics.ProductStatistics;
import kr.hhplus.be.server.domain.productstatistics.ProductStatisticsRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Clock;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ProductStatisticsServiceTest {


    @Mock
    ProductStatisticsRepository repository;

    @Mock
    ProductRankingService productRankingService;

    @InjectMocks
    ProductStatisticsService service;

    @Mock
    ProductRepository productRepository;


    @Test
    @DisplayName("상품 판매 기록 시 Redis에 저장된다")
    void record_callsRedisRepository() {
        // given
        Long productId = 1L;
        int quantity = 3;
        long unitAmount = 10000;

        // when
        service.record(new RecordSalesCommand(productId, quantity, unitAmount));

        // then
        verify(productRankingService).record(productId, quantity);
    }

    @Test
    @DisplayName("인기 상품 조회 시 성공")
    void getTopSellingProducts_success() {
        // given
        int days = 3;
        int limit = 5;
        LocalDate fixedToday = LocalDate.of(2020, 1, 4);
        Clock fixedClock = Clock.fixed(fixedToday.atStartOfDay(ZoneId.systemDefault()).toInstant(), ZoneId.systemDefault());

        service = new ProductStatisticsService(repository, productRankingService, productRepository, fixedClock);

        PopularProductCriteria criteria = new PopularProductCriteria(days, limit);

        // when
        service.getTopSellingProducts(criteria);

        // then
        LocalDate expectedFrom = fixedToday.minusDays(days);
        verify(repository).findTopSellingProducts(expectedFrom, fixedToday, limit);
    }


    @Test
    @DisplayName("syncDailyStatistics - Redis 점수 기반으로 통계 저장")
    void syncDailyStatistics_should_store_statistics_from_redis() {
        // given
        LocalDate targetDate = LocalDate.of(2025, 5, 14);
        String prefix = "ranking:daily:";
        String key = prefix + "20250514";

        Long productId1 = 1L;
        Long productId2 = 2L;

        when(productRankingService.buildKey(prefix, targetDate)).thenReturn(key);
        when(productRankingService.getTopN(prefix, targetDate, 1000)).thenReturn(List.of(productId1, productId2));
        when(productRankingService.getScore(prefix, targetDate, productId1)).thenReturn(5.0);
        when(productRankingService.getScore(prefix, targetDate, productId2)).thenReturn(3.0);
        when(productRepository.findUnitPriceById(productId1)).thenReturn(Optional.of(1000L));
        when(productRepository.findUnitPriceById(productId2)).thenReturn(Optional.of(2000L));
        when(repository.findByProductIdAndStatDate(productId1, targetDate)).thenReturn(Optional.empty());
        when(repository.findByProductIdAndStatDate(productId2, targetDate)).thenReturn(Optional.empty());

        // when
        service.syncDailyStatistics(targetDate);

        // then
        verify(repository).save(argThat(stat ->
                stat.getProductId().equals(productId1)
                        && stat.getSalesCount() == 5
                        && stat.getSalesAmount() == 5000
                        && stat.getStatDate().equals(targetDate)
        ));

        verify(repository).save(argThat(stat ->
                stat.getProductId().equals(productId2)
                        && stat.getSalesCount() == 3
                        && stat.getSalesAmount() == 6000
                        && stat.getStatDate().equals(targetDate)
        ));
    }
}
