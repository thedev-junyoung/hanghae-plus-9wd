package kr.hhplus.be.server.scheduler;

import kr.hhplus.be.server.domain.productstatistics.ProductStatistics;
import kr.hhplus.be.server.domain.productstatistics.ProductStatisticsRepository;
import kr.hhplus.be.server.infrastructure.redis.ProductRankRedisRepository;
import kr.hhplus.be.server.common.vo.Money;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test") // Embedded Redis + Test DB 활성화
@Slf4j
class ProductStatisticsSyncSchedulerIntegrationTest {

    @Autowired
    ProductStatisticsSyncScheduler scheduler;

    @Autowired
    ProductRankRedisRepository redisRepository;

    @Autowired
    ProductStatisticsRepository statisticsRepository;

    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyyMMdd");

    private final Long productId1 = 1L;
    private final Long productId2 = 2L;
    private final int delta1 = 3;
    private final int delta2 = 5;

    private LocalDate targetDate;
    private String redisKey;

    @BeforeEach
    void setUp() {
        targetDate = LocalDate.now().minusDays(1);
        redisKey = "ranking:daily:" + targetDate.format(FORMATTER);

        // Redis 점수 초기화
        redisRepository.incrementScore(redisKey, productId1, delta1);
        redisRepository.incrementScore(redisKey, productId2, delta2);
        redisRepository.incrementScore(redisKey, 9999L, 0); // 무시될 데이터
    }

    @Test
    @DisplayName("통계 동기화 스케줄러가 Redis에서 통계를 가져와 DB에 저장해야 한다.")
    void syncToDatabase_should_create_or_update_statistics_safely() {
        // given
        int beforeCount1 = getSalesCount(productId1, targetDate);
        int beforeCount2 = getSalesCount(productId2, targetDate);
        log.info("[BEFORE] productId1: {}, count: {}", productId1, beforeCount1);
        log.info("[BEFORE] productId2: {}, count: {}", productId2, beforeCount2);

        // when
        scheduler.syncToDatabase();

        // then
        int afterCount1 = getSalesCount(productId1, targetDate);
        int afterCount2 = getSalesCount(productId2, targetDate);
        log.info("[AFTER]  productId1: {}, count: {}, expected: {}", productId1, afterCount1, beforeCount1 + delta1);
        log.info("[AFTER]  productId2: {}, count: {}, expected: {}", productId2, afterCount2, beforeCount2 + delta2);

        assertThat(afterCount1).isEqualTo(beforeCount1 + delta1);
        assertThat(afterCount2).isEqualTo(beforeCount2 + delta2);

        // 무효 점수 검증
        Optional<ProductStatistics> none = statisticsRepository.findByProductIdAndStatDate(9999L, targetDate);
        log.info("[ASSERT] productId=9999 저장 여부: {}", none.isPresent() ? "존재함 (에러)" : "없음 (정상)");

        assertThat(none).isEmpty();
    }


    private int getSalesCount(Long productId, LocalDate date) {
        return statisticsRepository.findByProductIdAndStatDate(productId, date)
                .map(ProductStatistics::getSalesCount)
                .orElse(0);
    }
}
