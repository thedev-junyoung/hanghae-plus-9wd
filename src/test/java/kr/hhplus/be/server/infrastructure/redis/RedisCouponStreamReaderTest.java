package kr.hhplus.be.server.infrastructure.redis;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.connection.stream.*;
import org.springframework.data.redis.core.StreamOperations;
import org.springframework.data.redis.core.StringRedisTemplate;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

class RedisCouponStreamReaderTest {

    @Test
    @DisplayName("쿠폰 스트림에서 레코드를 읽어오는 테스트")
    void should_read_from_stream() {
        StringRedisTemplate redisTemplate = mock(StringRedisTemplate.class);
        var ops = mock(StreamOperations.class);
        when(redisTemplate.opsForStream()).thenReturn(ops);

        List<MapRecord<String, Object, Object>> expected = List.of(
                MapRecord.create("streamKey", Map.of("key", "value"))
        );

        when(ops.read(
                any(Consumer.class),
                any(StreamReadOptions.class),
                any(StreamOffset.class)
        )).thenReturn(expected);

        RedisCouponStreamReader reader = new RedisCouponStreamReader(redisTemplate);
        List<MapRecord<String, Object, Object>> actual = reader.readStream("WELCOME10");

        assertThat(actual).isEqualTo(expected);
    }
}
