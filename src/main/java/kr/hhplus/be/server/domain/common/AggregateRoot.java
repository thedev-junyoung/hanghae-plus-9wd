package kr.hhplus.be.server.domain.common;

import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Slf4j
public abstract class AggregateRoot<ID> {
    private final List<DomainEvent> domainEvents = new ArrayList<>();


    protected void registerEvent(DomainEvent event) {
        log.info("[AggregateRoot] 이벤트 등록 - type={} | description={} | aggregateId={}",
                event.getEventType(),
                event.getEventDescription(),
                event.getAggregateId());
        this.domainEvents.add(event);
    }

    public List<DomainEvent> getDomainEvents() {
        return Collections.unmodifiableList(domainEvents);
    }

    public void clearEvents() {
        if (!domainEvents.isEmpty()) {
            log.info("[AggregateRoot] 도메인 이벤트 초기화 - 총 {}건", domainEvents.size());
            for (DomainEvent event : domainEvents) {
                log.info(" └─ {} | aggregateId={} | description={}",
                        event.getEventType(),
                        event.getAggregateId(),
                        event.getEventDescription());
            }
        } else {
            log.info("[AggregateRoot] 초기화할 도메인 이벤트 없음");
        }

        this.domainEvents.clear();
    }

    public abstract ID getId();
}
