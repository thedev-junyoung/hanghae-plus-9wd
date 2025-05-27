package kr.hhplus.be.server.infrastructure.redis;

import org.springframework.data.redis.connection.stream.MapRecord;

import java.util.List;

public interface CouponStreamReader {
    List<MapRecord<String, Object, Object>> readStream(String code);
}
