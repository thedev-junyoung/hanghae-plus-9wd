package kr.hhplus.be.server.application.outbox;

import com.fasterxml.jackson.databind.ObjectMapper;
import kr.hhplus.be.server.domain.common.DomainEvent;
import kr.hhplus.be.server.domain.outbox.OutboxMessage;
import kr.hhplus.be.server.domain.outbox.OutboxRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class OutboxService {

    private final OutboxRepository outboxRepository;
    private final ObjectMapper objectMapper;

    @Transactional
    public void saveEvent(DomainEvent event) {
        try {
            String payload = objectMapper.writeValueAsString(event);
            log.info("[Outbox Service] payload={}", payload);

            String eventId = event.getId(); // 또는 generateUniqueId(event) 같은 방식

            if (outboxRepository.existsById(eventId)) {
                log.warn("[Outbox 중복 차단] 이미 저장된 이벤트입니다. eventId={}", eventId);
                return;
            }

            log.info("[Outbox 직렬화 시작] event={}", event);
            OutboxMessage message = new OutboxMessage(
                    eventId,
                    event.getAggregateId(),
                    event.getEventType(),
                    payload,
                    event.getOccurredAt()
            );


            outboxRepository.save(message);
            
            log.info("[OutboxService] Save event: {}", message);
            log.info("[Outbox 저장 완료] aggregateId={}, eventType={}", event.getAggregateId(), event.getEventType());

        } catch (Exception e) {
            log.error("[Outbox 직렬화 실패] event={}", event, e);
            throw new RuntimeException("Outbox 직렬화 실패", e);
        }
    }
}
