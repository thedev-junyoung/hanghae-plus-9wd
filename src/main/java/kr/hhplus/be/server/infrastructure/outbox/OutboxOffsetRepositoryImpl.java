package kr.hhplus.be.server.infrastructure.outbox;

import kr.hhplus.be.server.domain.outbox.OutBoxOffsetRepository;
import kr.hhplus.be.server.domain.outbox.OutboxOffset;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class OutboxOffsetRepositoryImpl implements OutBoxOffsetRepository {

    private final OutboxOffsetJpaRepository jpaRepository;

    @Override
    public Optional<OutboxOffset> findById(String topic) {
        return jpaRepository.findById(topic);
    }

    @Override
    public void save(OutboxOffset offset) {
        jpaRepository.save(offset);
    }
}
