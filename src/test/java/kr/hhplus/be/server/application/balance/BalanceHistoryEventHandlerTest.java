package kr.hhplus.be.server.application.balance;

import kr.hhplus.be.server.domain.common.DomainEvent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.mockito.Mockito.*;

class BalanceHistoryEventHandlerTest {

    private BalanceHistoryEventHandler handler;
    private BalanceHistoryUseCase balanceHistoryUseCase;

    @BeforeEach
    void setUp() {
        balanceHistoryUseCase = mock(BalanceHistoryUseCase.class);
        handler = new BalanceHistoryEventHandler(balanceHistoryUseCase);
    }

    @Test
    @DisplayName("이벤트 수신 시 BalanceHistoryUseCase.recordHistory가 호출된다")
    void handle_shouldDelegateToUseCase() {
        // given
        DomainEvent event = RecordBalanceChargeEvent.of(1L, 10000L, "유저 직접 충전 요청", "REQ-1234");

        // when
        handler.handle((RecordBalanceChargeEvent) event);

        // then
        verify(balanceHistoryUseCase).recordHistory(
                argThat(cmd ->
                        cmd.userId().equals(1L) &&
                                cmd.amount() == 10000L &&
                                cmd.reason().equals("유저 직접 충전 요청") &&
                                cmd.requestId().equals("REQ-1234")
                )
        );
    }
}
