package kr.hhplus.be.server.infrastructure.outbox;

import kr.hhplus.be.server.domain.outbox.OutboxMessage;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface OutboxJpaRepository extends JpaRepository<OutboxMessage, String> {
    List<OutboxMessage> findTop100ByIdGreaterThanOrderByIdAsc(String lastProcessedId);
}