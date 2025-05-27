package kr.hhplus.be.server.domain.balance;

import jakarta.persistence.*;
import kr.hhplus.be.server.application.balance.RecordBalanceChargeEvent;
import kr.hhplus.be.server.common.vo.Money;
import kr.hhplus.be.server.domain.common.AggregateRoot;
import lombok.*;
import lombok.extern.slf4j.Slf4j;

import java.time.LocalDateTime;

@Entity
@Table(name = "balance")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Slf4j
public class Balance extends AggregateRoot<Long> {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @EqualsAndHashCode.Include
    private Long id;

    @Column(nullable = false)
    private Long userId;

    @Column(nullable = false)
    private long amount;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(nullable = false)
    private LocalDateTime updatedAt;

    public Balance(Long id, Long userId, Money amount) {
        LocalDateTime now = LocalDateTime.now();
        this.id = id;
        this.userId = userId;
        this.amount = amount.value();
        this.createdAt = now;
        this.updatedAt = now;
    }

    public static Balance createNew(Long userId, Money amount) {
        LocalDateTime now = LocalDateTime.now();
        Balance balance = new Balance();
        balance.userId = userId;
        balance.amount = amount.value();
        balance.createdAt = now;
        balance.updatedAt = now;
        return balance;
    }

    public void charge(Money value, String reason, String requestId) {
        log.info("[Balance] 충전 요청 - userId: {}, amount: {}, reason: {}, requestId: {}", userId, value.value(), reason, requestId);
        Policy.validateMinimumCharge(value);
        this.amount = Money.wons(amount).add(value).value();
        this.updatedAt = LocalDateTime.now();
        registerEvent(RecordBalanceChargeEvent.of(this.userId, value.value(), reason, requestId));
    }

    public void decrease(Money value) {
        Money current = Money.wons(amount);
        if (!current.isGreaterThanOrEqual(value)) {
            throw new BalanceException.NotEnoughBalanceException();
        }
        this.amount = current.subtract(value).value();
        this.updatedAt = LocalDateTime.now();
    }

    public boolean hasEnough(Money value) {
        return Money.wons(amount).isGreaterThanOrEqual(value);
    }

    // 정책 내장 클래스
    public static class Policy {
        private static final long MINIMUM_CHARGE_AMOUNT = 1_000;

        public static void validateMinimumCharge(Money amount) {
            if (amount.value() < MINIMUM_CHARGE_AMOUNT) {
                throw new BalanceException.MinimumChargeAmountException(MINIMUM_CHARGE_AMOUNT);
            }
        }
    }
}
