package kr.hhplus.be.server.infrastructure.redis;

import kr.hhplus.be.server.application.coupon.CouponAsyncIssueService;
import kr.hhplus.be.server.infrastructure.redis.util.CouponStreamKeyResolver;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import org.springframework.data.redis.connection.stream.MapRecord;
import org.springframework.data.redis.core.StreamOperations;
import org.springframework.data.redis.core.StringRedisTemplate;

import java.util.List;
import java.util.Map;

import static org.mockito.Mockito.*;

class CouponIssueStreamProcessorTest {

    private CouponAsyncIssueService issueService;
    private StringRedisTemplate redisTemplate;
    private CouponIssueStreamProcessor processor;

    @BeforeEach
    void setUp() {
        issueService = mock(CouponAsyncIssueService.class);
        redisTemplate = mock(StringRedisTemplate.class);
        processor = new CouponIssueStreamProcessor(issueService, redisTemplate);
    }

    @Test
    @DisplayName("쿠폰 발급 성공 시 레코드 처리")
    void should_process_successful_record() {
        MapRecord<String, Object, Object> record = MapRecord.create("stream", Map.of("key", "value"));

        processor.process("WELCOME10", List.of(record));

        verify(issueService, times(1)).processAsync(record.getValue());
        verify(redisTemplate, never()).opsForStream();
    }

    @Test
    @DisplayName("쿠폰 발급 실패 시 DLQ로 이동")
    void should_move_to_dlq_when_processing_fails() {
        MapRecord<String, Object, Object> record = MapRecord.create("stream", Map.of("key", "value"));
        doThrow(new RuntimeException("fail")).when(issueService).processAsync(any());

        var ops = mock(StreamOperations.class);
        when(redisTemplate.opsForStream()).thenReturn(ops);

        processor.process("WELCOME10", List.of(record));

        verify(ops, times(1)).add(eq(CouponStreamKeyResolver.dlq("WELCOME10")), eq(record.getValue()));
    }
}
