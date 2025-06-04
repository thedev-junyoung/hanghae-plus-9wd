package kr.hhplus.be.server.domain.outbox;

import jakarta.persistence.*;
import kr.hhplus.be.server.domain.common.AggregateRoot;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.ToString;

import java.time.LocalDateTime;

@Entity
@Table(name = "outbox")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@ToString
public class OutboxMessage extends AggregateRoot<String> {

    @Id
    private String id;

    @Column(nullable = false)
    private String aggregateId;

    @Column(nullable = false)
    private String eventType;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String payload;

    @Column(nullable = false)
    private LocalDateTime occurredAt;

    public OutboxMessage(String eventId, String aggregateId, String eventType, String payload, LocalDateTime occurredAt) {
        this.id = eventId;
        this.aggregateId = aggregateId;
        this.eventType = eventType;
        this.payload = payload;
        this.occurredAt = occurredAt;
    }
}
