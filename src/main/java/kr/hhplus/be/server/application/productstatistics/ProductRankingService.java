package kr.hhplus.be.server.application.productstatistics;

import kr.hhplus.be.server.application.productstatistics.strategy.ProductRankingStrategy;
import kr.hhplus.be.server.infrastructure.redis.ProductRankRedisRepository;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Service
public class ProductRankingService {

    private final ProductRankingStrategy daily;

    private final ProductRankingStrategy weekly;

    private final ProductRankingStrategy monthly;

    private final ProductRankRedisRepository redis;

    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyyMMdd");

    public ProductRankingService(
            @Qualifier("daily") ProductRankingStrategy daily,
            @Qualifier("weekly") ProductRankingStrategy weekly,
            @Qualifier("monthly") ProductRankingStrategy monthly,
            ProductRankRedisRepository redis) {

        this.daily = daily;
        this.weekly = weekly;
        this.monthly = monthly;
        this.redis = redis;
    }

    public void record(Long productId, int quantity) {
        // 전략을 찾고, 해당 전략이 지원하는지 확인
        // 만약 추가, 변경된 전략이 있다면, 이곳에서 추가하면 됨
        daily.record(productId, quantity);
        weekly.record(productId, quantity);
        monthly.record(productId, quantity);
    }

    public List<Long> getTopN(String periodPrefix, LocalDate date, int limit) {
        String key = buildKey(periodPrefix, date);
        return redis.getTopN(key, limit);
    }

    public Double getScore(String periodPrefix, LocalDate date, Long productId) {
        String key = buildKey(periodPrefix, date);
        return redis.getScore(key, productId);
    }


    public String buildKey(String periodPrefix, LocalDate date) {
        return periodPrefix + date.format(FORMATTER);  // ex) ranking:daily:20250515
    }

    public void rollback(Long productId, int quantity) {
        daily.undo(productId, quantity);
        weekly.undo(productId, quantity);
        monthly.undo(productId, quantity);
    }
}
