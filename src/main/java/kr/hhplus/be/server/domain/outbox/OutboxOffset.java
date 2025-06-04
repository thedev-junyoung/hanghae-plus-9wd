package kr.hhplus.be.server.domain.outbox;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "outbox_offset")
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class OutboxOffset {

    @Id
    @Column(name = "topic_name", nullable = false)
    private String topicName;

    @Column(name = "last_processed_id", nullable = false)
    private String lastProcessedId;

    @Column(name = "updated_at", nullable = false, updatable = false, insertable = false)
    private LocalDateTime updatedAt;


    public static OutboxOffset create(String topicName, String lastProcessedId) {
        return new OutboxOffset(topicName, lastProcessedId, LocalDateTime.now());
    }


    public void updateLastProcessedId(String newId) {
        this.lastProcessedId = newId;
    }
}
