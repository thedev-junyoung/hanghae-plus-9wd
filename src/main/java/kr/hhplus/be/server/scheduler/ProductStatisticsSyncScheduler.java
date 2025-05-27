package kr.hhplus.be.server.scheduler;

import kr.hhplus.be.server.application.productstatistics.ProductStatisticsService;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
@Component
@RequiredArgsConstructor
public class ProductStatisticsSyncScheduler {

    private final ProductStatisticsService service;

    @Scheduled(cron = "0 0 3 * * *")
    public void syncToDatabase() {
        service.syncDailyStatistics(LocalDate.now().minusDays(1));
    }
}
