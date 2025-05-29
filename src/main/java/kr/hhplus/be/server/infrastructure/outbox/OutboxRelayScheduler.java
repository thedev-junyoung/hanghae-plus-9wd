package kr.hhplus.be.server.infrastructure.outbox;

import kr.hhplus.be.server.domain.outbox.OutboxMessage;
import kr.hhplus.be.server.domain.outbox.OutboxRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class OutboxRelayScheduler {

    private final OutboxRepository outboxRepository;
    private final KafkaTemplate<String, String> kafkaTemplate;

    private long lastProcessedId = 0L;

    public void relay() {
        log.info("[Outbox Relay Scheduler 시작] lastProcessedId={}", lastProcessedId);
        List<OutboxMessage> messages = outboxRepository.findTop100ByIdGreaterThanOrderByIdAsc(lastProcessedId);

        for (OutboxMessage message : messages) {
            try {
                kafkaTemplate.send("coupon.issue.requested", message.getAggregateId(), message.getPayload());
                lastProcessedId = message.getId();
                log.info("[Kafka 전송 성공] id={}, topic=coupon.issue.requested", message.getId());
            } catch (Exception e) {
                log.error("[Kafka 전송 실패] id={}, topic=coupon.issue.requested", message.getId(), e);
                break; // 실패 시 이후 메시지 대기
            }
        }
    }
}
