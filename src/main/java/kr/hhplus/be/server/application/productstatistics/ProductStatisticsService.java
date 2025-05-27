package kr.hhplus.be.server.application.productstatistics;

import kr.hhplus.be.server.application.product.PopularProductCriteria;
import kr.hhplus.be.server.common.vo.Money;
import kr.hhplus.be.server.domain.product.ProductRepository;
import kr.hhplus.be.server.domain.productstatistics.ProductStatistics;
import kr.hhplus.be.server.domain.productstatistics.ProductStatisticsRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class ProductStatisticsService implements ProductStatisticsUseCase {

    private final ProductStatisticsRepository repository;
    private final ProductRankingService productRankingService;
    private final ProductRepository productRepository;
    private final Clock clock; // 추가

    @Override
    public void record(RecordSalesCommand command) {
        // Redis만 기록
        productRankingService.record(command.productId(), command.quantity());
    }

    @Override
    public List<ProductSalesInfo> getTopSellingProducts(PopularProductCriteria criteria) {
        LocalDate today = LocalDate.now(clock); // 고정 가능한 now()
        LocalDate from = today.minusDays(criteria.days());
        int limit = criteria.limit();

        return repository.findTopSellingProducts(from, today, limit);
    }

    @Override
    public void syncDailyStatistics(LocalDate targetDate) {
        String prefix = "ranking:daily:";
        String key = productRankingService.buildKey(prefix, targetDate);
        List<Long> productIds = productRankingService.getTopN(prefix, targetDate, 1000);

        log.info("[Sync] 통계 동기화 시작 - key={}, 상품 수={}", key, productIds.size());

        productIds.stream()
                .map(productId -> toCommand(productId, prefix, targetDate))
                .filter(Optional::isPresent)
                .map(Optional::get)
                .forEach(command -> {
                    ProductStatistics stat = ProductStatistics.createOrUpdate(
                            repository.findByProductIdAndStatDate(command.productId(), targetDate).orElse(null),
                            command.productId(),
                            targetDate,
                            command.quantity(),
                            command.unitPrice()
                    );

                    repository.save(stat);
                    log.info("[Sync] 저장 - productId={}, 수량={}, 금액={}, 날짜={}",
                            command.productId(), stat.getSalesCount(), stat.getSalesAmount(), targetDate);
                });

        log.info("[Sync] 통계 동기화 완료 - date={}, total={}", targetDate, productIds.size());
    }

    private Optional<SyncRecordCommand> toCommand(Long productId, String prefix, LocalDate date) {
        double score = Optional.ofNullable(productRankingService.getScore(prefix, date, productId))
                .orElse(0.0);
        if (score <= 0.0) return Optional.empty();

        Money unitPrice = productRepository.findUnitPriceById(productId)
                .map(Money::from)
                .orElse(Money.ZERO);
        return Optional.of(new SyncRecordCommand(productId, (int) score, unitPrice));
    }


}
