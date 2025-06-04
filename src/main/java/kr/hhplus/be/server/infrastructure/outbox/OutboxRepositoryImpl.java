package kr.hhplus.be.server.infrastructure.outbox;

import kr.hhplus.be.server.domain.outbox.OutboxMessage;
import kr.hhplus.be.server.domain.outbox.OutboxRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
@RequiredArgsConstructor
public class OutboxRepositoryImpl implements OutboxRepository {

    private final OutboxJpaRepository jpaRepository;

    @Override
    public List<OutboxMessage> findTop100ByIdGreaterThanOrderByIdAsc(String lastProcessedId) {
        return jpaRepository.findTop100ByIdGreaterThanOrderByIdAsc(lastProcessedId).stream()
                .toList();
    }

    @Override
    public void save(OutboxMessage outboxEvent) {
        jpaRepository.save(outboxEvent);
    }

    @Override
    public List<OutboxMessage> findAll() {
        return jpaRepository.findAll();
    }

    @Override
    public boolean existsById(String eventId) {
        return jpaRepository.existsById(eventId);
    }

    @Override
    public void deleteAll() {
        jpaRepository.deleteAll();
    }
}
