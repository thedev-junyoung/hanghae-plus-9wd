package kr.hhplus.be.server.domain.common;

import java.time.LocalDateTime;
import java.util.UUID;

public abstract class AbstractDomainEvent implements DomainEvent {

    private final String id;
    private final String aggregateId;
    private final LocalDateTime occurredAt;
    private final String eventDescription;

    protected AbstractDomainEvent(String aggregateId, String eventDescription) {
        this.id = UUID.randomUUID().toString();
        this.aggregateId = aggregateId;
        this.occurredAt = LocalDateTime.now();
        this.eventDescription = eventDescription;
    }

    @Override
    public String getId() {
        return id;
    }

    @Override
    public String getAggregateId() {
        return aggregateId;
    }

    @Override
    public LocalDateTime getOccurredAt() {
        return occurredAt;
    }

    @Override
    public String getEventType() {
        return this.getClass().getSimpleName();
    }

    @Override
    public String getEventDescription() {
        return eventDescription;
    }
}
