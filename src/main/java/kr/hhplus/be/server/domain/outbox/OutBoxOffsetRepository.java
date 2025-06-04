package kr.hhplus.be.server.domain.outbox;

import java.util.Optional;

public interface OutBoxOffsetRepository {

    Optional<OutboxOffset> findById(String topic);

    void save(OutboxOffset offset);
}
