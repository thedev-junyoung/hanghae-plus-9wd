package kr.hhplus.be.server.domain.outbox;


import java.util.List;

public interface OutboxRepository {
    List<OutboxMessage> findTop100ByIdGreaterThanOrderByIdAsc(Long lastProcessedId);

    void save(OutboxMessage outboxEvent);

    List<OutboxMessage> findAll();
}
