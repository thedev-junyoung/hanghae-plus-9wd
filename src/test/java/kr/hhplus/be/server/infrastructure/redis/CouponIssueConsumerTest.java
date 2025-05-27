package kr.hhplus.be.server.infrastructure.redis;

import kr.hhplus.be.server.application.coupon.CouponUseCase;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.connection.stream.Consumer;
import org.springframework.data.redis.connection.stream.MapRecord;
import org.springframework.data.redis.connection.stream.StreamOffset;
import org.springframework.data.redis.connection.stream.StreamReadOptions;
import org.springframework.data.redis.core.StreamOperations;
import org.springframework.data.redis.core.StringRedisTemplate;

import java.util.List;
import java.util.Map;

import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CouponIssueConsumerTest {

    @InjectMocks
    private CouponIssueConsumer consumer;

    @Mock
    private CouponUseCase couponService;

    @Mock
    private CouponStreamReader streamReader;

    @Mock
    private CouponIssueStreamProcessor processor;

    @Mock
    private StringRedisTemplate redisTemplate;

    @Mock
    private StreamOperations<String, Object, Object> streamOps;

    @BeforeEach
    void setup() {
        when(redisTemplate.opsForStream()).thenReturn(streamOps);
        reset(redisTemplate, streamOps, couponService, streamReader, processor);
        when(redisTemplate.opsForStream()).thenReturn(streamOps);

    }
    @Test
    @DisplayName("정상적인 쿠폰 코드에 대해 스트림 데이터를 읽고 처리한다")
    void consume_shouldProcessEachCouponCode() {
        // given
        String couponCode = "WELCOME10";
        String streamKey = "coupon:stream:WELCOME10";
        List<MapRecord<String, Object, Object>> mockRecords = List.of(
                MapRecord.create(streamKey, Map.of("userId", "1"))
        );

        when(couponService.findAllCouponCodes()).thenReturn(List.of(couponCode));
        when(redisTemplate.hasKey(streamKey)).thenReturn(true);
        when(streamOps.read(
                eq(Consumer.from("coupon-consumer-group", "consumer-1")),
                any(StreamReadOptions.class),
                any(StreamOffset.class)
        )).thenReturn(mockRecords);

        // when
        consumer.consume();

        // then
        verify(redisTemplate).hasKey(streamKey);
        verify(streamOps, never()).add(anyString(), anyMap());
        verify(streamOps).createGroup(streamKey, "coupon-consumer-group");
        // ✅ remove this:
        // verify(streamReader).readStream(couponCode);
        verify(processor).process(couponCode, mockRecords);
    }

    @Test
    @DisplayName("Stream이 없으면 더미 레코드를 추가하고 group을 생성한다")
    void ensureStreamAndGroupExist_shouldAddDummyIfStreamMissing() {
        // given
        String streamKey = "coupon:stream:TEST-COUPON";

        when(redisTemplate.hasKey(streamKey)).thenReturn(false);
        when(couponService.findAllCouponCodes()).thenReturn(List.of("TEST-COUPON"));

        // when
        consumer.consume();

        // then
        verify(streamOps).add(eq(streamKey), eq(Map.of("init", "init")));
    }

    @Test
    @DisplayName("이미 존재하는 group이면 BUSYGROUP 예외를 무시하고 진행한다")
    void ensureStreamAndGroupExist_shouldHandleBusyGroupException() {
        // given
        String streamKey = "coupon:stream:TEST-COUPON";

        when(redisTemplate.hasKey(streamKey)).thenReturn(true);
        when(couponService.findAllCouponCodes()).thenReturn(List.of("TEST-COUPON"));


        RuntimeException busyGroup = new RuntimeException(
                new io.lettuce.core.RedisBusyException("BUSYGROUP Consumer Group name already exists")
        );
        doThrow(busyGroup).when(streamOps).createGroup(streamKey, "coupon-consumer-group");

        // when
        consumer.consume();

        // then
        verify(streamOps).createGroup(streamKey, "coupon-consumer-group");
    }


    @Test
    @DisplayName("레코드가 존재하면 processor가 호출된다")
    void consume_shouldProcessRecordIfPresent() {
        // given
        String code = "TEST-COUPON";
        String streamKey = "coupon:stream:TEST-COUPON";
        MapRecord<String, Object, Object> record = MapRecord.create(streamKey, Map.of("userId", "1"));

        when(couponService.findAllCouponCodes()).thenReturn(List.of(code));
        when(redisTemplate.hasKey(streamKey)).thenReturn(true);
        when(streamOps.read(
                eq(Consumer.from("coupon-consumer-group", "consumer-1")),
                any(StreamReadOptions.class),
                any(StreamOffset.class)
        )).thenReturn(List.of(record));

        // when
        consumer.consume();

        // then
        verify(processor).process(code, List.of(record));
    }

}
