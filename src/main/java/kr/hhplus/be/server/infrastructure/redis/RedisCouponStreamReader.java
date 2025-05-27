package kr.hhplus.be.server.infrastructure.redis;

import kr.hhplus.be.server.infrastructure.redis.util.CouponStreamKeyResolver;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.connection.stream.*;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class RedisCouponStreamReader implements CouponStreamReader {

    private final StringRedisTemplate redisTemplate;

    @Override
    public List<MapRecord<String, Object, Object>> readStream(String code) {
        String streamKey = CouponStreamKeyResolver.resolve(code);
        List<MapRecord<String, Object, Object>> records = redisTemplate.opsForStream()
                .read(Consumer.from("coupon-consumer-group", "consumer-1"),
                        StreamReadOptions.empty().count(10),
                        StreamOffset.create(streamKey, ReadOffset.from("0"))
                );


        log.info("읽은 레코드 수 = {}", records.size());
        return records;
    }
}
