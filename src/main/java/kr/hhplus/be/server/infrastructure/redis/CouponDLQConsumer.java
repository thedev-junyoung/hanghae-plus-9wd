package kr.hhplus.be.server.infrastructure.redis;

import io.lettuce.core.RedisBusyException;
import kr.hhplus.be.server.application.coupon.CouponAsyncIssueService;
import kr.hhplus.be.server.application.coupon.CouponUseCase;
import kr.hhplus.be.server.infrastructure.redis.util.CouponStreamKeyResolver;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.RedisSystemException;
import org.springframework.data.redis.connection.stream.*;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Component
@RequiredArgsConstructor
@Slf4j
public class CouponDLQConsumer {

    private final StringRedisTemplate redisTemplate;
    private final CouponAsyncIssueService issueService;
    private final CouponUseCase couponService;

    private static final String GROUP_NAME = "dlq-group";
    private static final String CONSUMER_NAME = "dlq-consumer";
    private static final int MAX_RETRY = 3;

    @Scheduled(fixedDelay = 5000)
    public void consumeDLQ() {
        log.debug("[DLQ 소비 시작]");
        for (String code : couponService.findAllCouponCodes()) {
            String dlqKey = CouponStreamKeyResolver.dlq(code);

            try {
                ensureStreamAndGroup(dlqKey);

                List<MapRecord<String, Object, Object>> records = redisTemplate.opsForStream().read(
                        Consumer.from(GROUP_NAME, CONSUMER_NAME),
                        StreamReadOptions.empty().count(5).block(Duration.ofSeconds(2)),
                        StreamOffset.create(dlqKey, ReadOffset.lastConsumed())
                );

                for (MapRecord<String, Object, Object> record : records) {
                    String retryKey = "dlq:retry:" + record.getId();
                    int retryCount = Integer.parseInt(
                            Optional.ofNullable(redisTemplate.opsForValue().get(retryKey)).orElse("0")
                    );
                    redisTemplate.expire(retryKey, Duration.ofHours(1)); // optional


                    if (retryCount >= MAX_RETRY) {
                        log.warn("[DLQ 재시도 초과] {} - 건너뜀", record.getId());
                        redisTemplate.opsForStream().acknowledge(dlqKey, GROUP_NAME, record.getId());
                        continue;
                    }

                    try {
                        issueService.processAsync(record.getValue());
                        log.info("[DLQ 재처리 성공] {}", record.getId());
                        redisTemplate.opsForStream().acknowledge(dlqKey, GROUP_NAME, record.getId());
                        redisTemplate.delete(retryKey);
                    } catch (Exception e) {
                        log.error("[DLQ 재처리 실패] {} - {}", record.getId(), e.getMessage(), e);
                        redisTemplate.opsForValue().increment(retryKey);
                    }
                }

            } catch (Exception e) {
                log.debug("[DLQ 소비 실패] code={}, error={}", code, e.getMessage());
            }
            log.debug("[DLQ 소비 완료] code={}", code);
        }
    }

    private void ensureStreamAndGroup(String streamKey) {
        if (!Boolean.TRUE.equals(redisTemplate.hasKey(streamKey))) {
            redisTemplate.opsForStream().add(streamKey, Map.of("init", "init"));

        }
        try {
            redisTemplate.opsForStream().createGroup(streamKey, GROUP_NAME);
        } catch (RedisSystemException e) {
            if (e.getRootCause() instanceof RedisBusyException && e.getMessage().contains("BUSYGROUP")) {
                return;
            }
            throw e;
        }
    }
}
