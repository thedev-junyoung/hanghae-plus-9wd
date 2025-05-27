package kr.hhplus.be.server.infrastructure.redis;

import kr.hhplus.be.server.application.coupon.IssueLimitedCouponCommand;
import kr.hhplus.be.server.infrastructure.redis.util.CouponStreamKeyResolver;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class CouponIssueStreamPublisher {

    private final StringRedisTemplate redisTemplate;

    public void publish(IssueLimitedCouponCommand command) {
        log.info("쿠폰 발급 요청 → Stream 전송 - couponCode={}, userId={}", command.couponCode(), command.userId());
        String streamKey = CouponStreamKeyResolver.resolve(command.couponCode());

        Map<String, String> payload = Map.of(
                "userId", String.valueOf(command.userId()),
                "couponCode", command.couponCode(),
                "requestId", command.requestId()
        );

        redisTemplate.opsForStream().add(streamKey, payload);
        log.info("쿠폰 발급 요청 → Stream 전송 완료 - couponCode={}, userId={}", command.couponCode(), command.userId());
    }
}
