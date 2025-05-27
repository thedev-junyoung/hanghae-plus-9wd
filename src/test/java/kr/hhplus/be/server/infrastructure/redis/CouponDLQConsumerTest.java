package kr.hhplus.be.server.infrastructure.redis;

import kr.hhplus.be.server.application.coupon.CouponAsyncIssueService;
import kr.hhplus.be.server.application.coupon.CouponUseCase;
import kr.hhplus.be.server.infrastructure.redis.util.CouponStreamKeyResolver;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.connection.stream.*;
import org.springframework.data.redis.core.StreamOperations;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.util.List;
import java.util.Map;

import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CouponDLQConsumerTest {

    @Mock
    StringRedisTemplate redisTemplate;

    @Mock
    CouponAsyncIssueService issueService;

    @Mock
    CouponUseCase couponService;

    @Mock
    ValueOperations<String, String> valueOps;

    @InjectMocks
    CouponDLQConsumer consumer;

    StreamOperations<String, Object, Object> streamOps;

    @BeforeEach
    void setup() {
        streamOps = mock(StreamOperations.class);
        given(redisTemplate.opsForStream()).willReturn(streamOps);
        given(redisTemplate.opsForValue()).willReturn(valueOps);
    }

    @Test
    @DisplayName("DLQ에서 레코드를 읽고 처리 성공")
    void consumeDLQ_success() {
        // given
        String couponCode = "WELCOME10";
        String streamKey = CouponStreamKeyResolver.dlq(couponCode);
        String recordId = "1675716220000-0";
        String retryKey = "dlq:retry:" + recordId;

        Map<Object, Object> value = Map.of(
                "userId", "1",
                "couponCode", "WELCOME10",
                "requestId", "req1"
        );

        // recordId 지정이 필요한 경우 -> withId() 사용
        MapRecord<String, Object, Object> record = MapRecord.create(streamKey, value)
                .withId(RecordId.of(recordId));

        given(couponService.findAllCouponCodes()).willReturn(List.of(couponCode));
        given(streamOps.read(
                any(Consumer.class),
                any(StreamReadOptions.class),
                any(StreamOffset.class)
        )).willReturn(List.of(record));

        // when
        consumer.consumeDLQ();

        // then
        verify(issueService).processAsync(value);
        verify(streamOps).acknowledge(eq(streamKey), eq("dlq-group"), eq(RecordId.of(recordId)));
        verify(redisTemplate).delete(retryKey);
    }

    @Test
    @DisplayName("DLQ 처리 중 예외 발생 시 로그 출력 및 재시도 증가")
    void consumeDLQ_failure() {
        // given
        String couponCode = "WELCOME10";
        String streamKey = CouponStreamKeyResolver.dlq(couponCode);
        String recordId = "1675716220000-0";
        String retryKey = "dlq:retry:" + recordId;

        Map<Object, Object> value = Map.of(
                "userId", "1",
                "couponCode", couponCode,
                "requestId", "req2"
        );

        MapRecord<String, Object, Object> record = MapRecord.create(streamKey, value)
                .withId(RecordId.of(recordId));

        given(couponService.findAllCouponCodes()).willReturn(List.of(couponCode));
        given(streamOps.read(
                any(Consumer.class),
                any(StreamReadOptions.class),
                any(StreamOffset.class)
        )).willReturn(List.of(record));
        given(valueOps.get(retryKey)).willReturn("1");

        doThrow(new RuntimeException("fail")).when(issueService).processAsync(value);

        // when
        consumer.consumeDLQ();

        // then
        verify(issueService).processAsync(value);
        verify(valueOps).increment(retryKey);
    }

}
