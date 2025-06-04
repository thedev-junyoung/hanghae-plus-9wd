package kr.hhplus.be.server.infrastructure.outbox;

import kr.hhplus.be.server.domain.outbox.OutboxOffset;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OutboxOffsetJpaRepository extends JpaRepository<OutboxOffset, String> {
}
