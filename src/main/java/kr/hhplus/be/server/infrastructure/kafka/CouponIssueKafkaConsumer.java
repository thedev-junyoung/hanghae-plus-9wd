package kr.hhplus.be.server.infrastructure.kafka;

import kr.hhplus.be.server.application.coupon.CouponUseCase;
import kr.hhplus.be.server.application.coupon.IssueLimitedCouponCommand;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

@Slf4j
@Component
@RequiredArgsConstructor
public class CouponIssueKafkaConsumer {

    private final CouponUseCase couponUseCase;
    // 테스트 검증을 위한 수신 메시지 저장소
    public static final List<ConsumerRecord<String, String>> receivedDltMessages = new CopyOnWriteArrayList<>();

    @KafkaListener(
            topics = "coupon.issue",
            groupId = "coupon-issue-group",
            containerFactory = "couponKafkaListenerFactory"
    )
    public void consume(ConsumerRecord<String, CouponIssueKafkaMessage> record, Acknowledgment ack) {
        CouponIssueKafkaMessage message = record.value();
        log.info("[Kafka] 쿠폰 발급 요청 수신 - userId={}, couponCode={}", message.getUserId(), message.getCouponCode());

        try {
            if ("FAILURE_TEST".equals(message.getCouponCode())) {
                log.info("[Kafka] 강제 실패 처리 - userId={}, couponCode={}", message.getUserId(), message.getCouponCode());
                throw new RuntimeException("강제 실패 처리 - DLQ 전송 대상");
            }

            // 실제 발급 로직은 여기에 있음 (트랜잭션 포함)
            couponUseCase.issueLimitedCoupon(
                    IssueLimitedCouponCommand.of(message.getUserId(), message.getCouponCode())
            );

            ack.acknowledge(); // 수동 커밋
        } catch (Exception e) {
            log.info("[Kafka] 쿠폰 발급 처리 실패 - userId={}, 이유={}", message.getUserId(), e.getMessage());
            throw e; // Kafka의 retry, DLQ로 흐르게
        }
    }

    @KafkaListener(topics = "coupon.issue.DLT", groupId = "coupon-issue-dlt-group")
    public void consumeFromDlt(ConsumerRecord<String, String> record) {
        log.warn("[DLT] 쿠폰 발급 실패 메시지 수신됨 - key={}, value={}", record.key(), record.value());
        byte[] decoded = Base64.getDecoder().decode(record.value().replaceAll("\"", ""));
        String json = new String(decoded, StandardCharsets.UTF_8);

        receivedDltMessages.add(record);

        log.info("[DLT] 디코딩된 메시지 = {}", json);
        // TODO: Slack 알림, 재처리 큐 적재 등 후속 조치
    }
}

