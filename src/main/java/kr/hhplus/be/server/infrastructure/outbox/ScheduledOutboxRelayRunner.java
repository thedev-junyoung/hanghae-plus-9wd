package kr.hhplus.be.server.infrastructure.outbox;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Profile;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
@Profile("!test")
public class ScheduledOutboxRelayRunner {

    private final OutboxRelayScheduler scheduler;

    @Scheduled(fixedDelay = 1000)
    public void run() {
        scheduler.relay(); // 비즈니스 로직만 수행하는 별도 클래스를 호출
    }
}