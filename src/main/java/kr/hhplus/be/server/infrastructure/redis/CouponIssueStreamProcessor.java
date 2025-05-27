package kr.hhplus.be.server.infrastructure.redis;

import kr.hhplus.be.server.application.coupon.CouponAsyncIssueService;
import kr.hhplus.be.server.infrastructure.redis.util.CouponStreamKeyResolver;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.connection.stream.MapRecord;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class CouponIssueStreamProcessor {

    private final CouponAsyncIssueService issueService;
    private final StringRedisTemplate redisTemplate;

    public void process(String couponCode, List<MapRecord<String, Object, Object>> records) {
        for (MapRecord<String, Object, Object> record : records) {
            try {
                issueService.processAsync(record.getValue());
            } catch (Exception e) {
                log.info("쿠폰 발급 실패 → DLQ 이동 - code={}, error={}", couponCode, e.getMessage());
                redisTemplate.opsForStream()
                        .add(CouponStreamKeyResolver.dlq(couponCode), record.getValue());
                log.error("쿠폰 발급 실패 → DLQ 이동 - code={}, error={}", couponCode, e.getMessage(), e);
            }
        }
    }
}
