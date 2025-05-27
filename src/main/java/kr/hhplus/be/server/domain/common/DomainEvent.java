package kr.hhplus.be.server.domain.common;


import java.time.LocalDateTime;

public interface DomainEvent {
    String getId(); // 이벤트 ID
    String getAggregateId(); // 소속 Aggregate ID
    LocalDateTime getOccurredAt(); // 발생 시각
    String getEventType(); // 이벤트 타입명 (클래스명 등)
    String getEventDescription(); // 이벤트 설명 (자연어)
}