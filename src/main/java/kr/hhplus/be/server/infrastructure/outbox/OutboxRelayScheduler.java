package kr.hhplus.be.server.infrastructure.outbox;

import kr.hhplus.be.server.domain.order.OrderRepository;
import kr.hhplus.be.server.domain.outbox.OutBoxOffsetRepository;
import kr.hhplus.be.server.domain.outbox.OutboxMessage;
import kr.hhplus.be.server.domain.outbox.OutboxOffset;
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

    private final OutBoxOffsetRepository offsetRepository;

    public void relay() {
        final String topic = "coupon.issue.requested";
        OutboxOffset offset = offsetRepository.findById(topic)
                .orElse(OutboxOffset.create(topic, ""));

        String lastId = offset.getLastProcessedId();

        while (true) {
            List<OutboxMessage> messages = outboxRepository.findTop100ByIdGreaterThanOrderByIdAsc(lastId);
            if (messages.isEmpty()) break;

            for (OutboxMessage message : messages) {
                try {
                    kafkaTemplate.send(topic, message.getAggregateId(), message.getPayload());
                    lastId = message.getId(); // 성공한 메시지의 ID 갱신
                    log.info("[Kafka 전송 성공] id={}, topic={}", message.getId(), topic);
                } catch (Exception e) {
                    log.error("[Kafka 전송 실패] id={}, topic={}", message.getId(), topic, e);
                    break;
                }
            }

            offset.updateLastProcessedId(lastId); // 마지막 처리 ID 갱신
            offsetRepository.save(offset); // 마지막 처리 ID 저장
        }
    }

}
