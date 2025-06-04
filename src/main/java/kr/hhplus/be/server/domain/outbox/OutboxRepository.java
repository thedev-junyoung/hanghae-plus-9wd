package kr.hhplus.be.server.domain.outbox;


import java.util.List;

public interface OutboxRepository {
    List<OutboxMessage> findTop100ByIdGreaterThanOrderByIdAsc(String lastProcessedId);

    void save(OutboxMessage outboxEvent);

    List<OutboxMessage> findAll();

    boolean existsById(String eventId);

    void deleteAll();

}
