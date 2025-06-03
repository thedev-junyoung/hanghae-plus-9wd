package kr.hhplus.be.server.infrastructure.redis;

import io.lettuce.core.RedisBusyException;
import kr.hhplus.be.server.application.coupon.CouponUseCase;
import kr.hhplus.be.server.infrastructure.redis.util.CouponStreamKeyResolver;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.connection.stream.*;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.List;
import java.util.Map;


@Component
@RequiredArgsConstructor
@Slf4j
public class CouponIssueConsumer {

    private final CouponUseCase couponService;
    private final CouponIssueStreamProcessor processor;
    private final StringRedisTemplate redisTemplate;

    private static final String GROUP = "coupon-consumer-group";
    private static final String CONSUMER = "consumer-1";

    @Scheduled(fixedDelay = 1000)
    public void consume() {
        for (String code : couponService.findAllCouponCodes()) {
            try {
                ensureStreamAndGroupExist(code);
                List<MapRecord<String, Object, Object>> records = redisTemplate.opsForStream().read(
                        Consumer.from(GROUP, CONSUMER),
                        StreamReadOptions.empty().count(10).block(Duration.ofSeconds(2)),
                        StreamOffset.create(CouponStreamKeyResolver.resolve(code), ReadOffset.lastConsumed())
                );

                log.debug("읽은 레코드 수 = {}", records.size());

                processor.process(code, records);

                for (MapRecord<String, Object, Object> record : records) {
                    redisTemplate.opsForStream().acknowledge(CouponStreamKeyResolver.resolve(code), GROUP, record.getId());
                }

            } catch (Exception e) {
                log.error("[CouponConsumer] 처리 실패 - couponCode: {}", code, e);
            }
        }
    }

    public void consumeSingle(String code) {
        ensureStreamAndGroupExist(code);

        List<MapRecord<String, Object, Object>> records = redisTemplate.opsForStream().read(
                Consumer.from(GROUP, CONSUMER),
                StreamReadOptions.empty().count(10).block(Duration.ofSeconds(2)),
                StreamOffset.create(CouponStreamKeyResolver.resolve(code), ReadOffset.lastConsumed())
        );

        log.debug("읽은 레코드 수 = {}", records.size());
        processor.process(code, records);

        for (MapRecord<String, Object, Object> record : records) {
            redisTemplate.opsForStream().acknowledge(CouponStreamKeyResolver.resolve(code), GROUP, record.getId());
        }
    }


    private void ensureStreamAndGroupExist(String code) {
        String streamKey = CouponStreamKeyResolver.resolve(code);

        if (!Boolean.TRUE.equals(redisTemplate.hasKey(streamKey))) {
            log.debug("[CouponConsumer] Stream이 존재하지 않아 더미 레코드를 추가합니다 - key={} ", streamKey);
            redisTemplate.opsForStream().add(streamKey, Map.of("init", "init"));
        }

        try {
            redisTemplate.opsForStream().createGroup(streamKey, GROUP);
        } catch (Exception e) {
            Throwable rootCause = e.getCause();
            if (rootCause instanceof RedisBusyException && rootCause.getMessage().contains("BUSYGROUP")) {
                log.debug("[CouponConsumer] 이미 생성된 Group입니다 - key={}", streamKey);
            } else {
                throw new IllegalStateException("Stream group 생성 실패: " + streamKey, e);
            }
        }
    }
}

