package kr.hhplus.be.server.application.productstatistics;

import kr.hhplus.be.server.application.product.PopularProductCriteria;
import kr.hhplus.be.server.common.vo.Money;
import kr.hhplus.be.server.domain.balance.BalanceHistoryRepository;
import kr.hhplus.be.server.domain.product.Product;
import kr.hhplus.be.server.domain.product.ProductRepository;
import kr.hhplus.be.server.domain.productstatistics.ProductStatistics;
import kr.hhplus.be.server.domain.productstatistics.ProductStatisticsRepository;
import kr.hhplus.be.server.infrastructure.redis.ProductRankRedisRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
@SpringBootTest
class ProductStatisticsServiceIntegrationTest {

    @Autowired
    ProductStatisticsService service;

    @Autowired
    ProductStatisticsRepository repository;

    @Autowired
    ProductRankRedisRepository redisRepository;


    @Autowired
    ProductRepository productRepository;

    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyyMMdd");

    private Long productId1 = 901L;  // 테스트 전용 productId
    private Long productId2 = 902L;
    private final int delta1 = 3;
    private final int delta2 = 5;
    private LocalDate targetDate;
    private String redisKey;



    @BeforeEach
    void setUp() {

        targetDate = LocalDate.now().minusDays(1);
        redisKey = "ranking:daily:" + targetDate.format(FORMATTER);

        // 실제 존재하는 상품 ID로 대체하거나, 다음처럼 직접 추가
        Product product1 = Product.create("테스트1", "브랜드A", Money.wons(10000), targetDate.minusDays(10), null, null);
        Product product2 = Product.create("테스트2", "브랜드B", Money.wons(20000), targetDate.minusDays(10), null, null);
        product1 = productRepository.save(product1);
        product2 = productRepository.save(product2);

        productId1 = product1.getId();
        productId2 = product2.getId();

        repository.findByProductIdAndStatDate(productId1, targetDate).ifPresent(repository::delete);
        repository.findByProductIdAndStatDate(productId2, targetDate).ifPresent(repository::delete);

        redisRepository.incrementScore(redisKey, productId1, delta1);
        redisRepository.incrementScore(redisKey, productId2, delta2);
        redisRepository.incrementScore(redisKey, 9999L, 0); // 무효 점수
    }

    @Test
    @DisplayName("최근 3일간의 통계 기반으로 인기 상품 정렬 결과가 유효하다")
    void getTopSellingProducts_basedOnActualData() {
        // given
        PopularProductCriteria criteria = new PopularProductCriteria(3, 5);

        // when
        Collection<ProductSalesInfo> results = service.getTopSellingProducts(criteria);
        List<ProductSalesInfo> resultList = new ArrayList<>(results);

        // then
        assertThat(resultList).isNotEmpty();
        assertThat(resultList.size()).isLessThanOrEqualTo(5);

        // 정렬 검증
        for (int i = 1; i < resultList.size(); i++) {
            assertThat(resultList.get(i - 1).salesCount())
                    .isGreaterThanOrEqualTo(resultList.get(i).salesCount());
        }

        // 기본 출력 확인
        resultList.forEach(result -> {
            System.out.printf("상품 ID: %d, 판매량: %d\n", result.productId(), result.salesCount());
            assertThat(result.salesCount()).isGreaterThan(0);
        });
    }


    @Test
    @DisplayName("syncDailyStatistics - Redis → DB 통계 저장 검증")
    void syncDailyStatistics_should_write_to_database() {
        // when
        service.syncDailyStatistics(targetDate);

        // then
        Optional<ProductStatistics> stat1 = repository.findByProductIdAndStatDate(productId1, targetDate);
        Optional<ProductStatistics> stat2 = repository.findByProductIdAndStatDate(productId2, targetDate);
        Optional<ProductStatistics> none = repository.findByProductIdAndStatDate(9999L, targetDate);

        assertThat(stat1).isPresent();
        assertThat(stat1.get().getSalesCount()).isEqualTo(delta1);
        assertThat(stat1.get().getSalesAmount()).isGreaterThan(0);

        assertThat(stat2).isPresent();
        assertThat(stat2.get().getSalesCount()).isEqualTo(delta2);
        assertThat(stat2.get().getSalesAmount()).isGreaterThan(0);

        assertThat(none).isEmpty();
    }
}
