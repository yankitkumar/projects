package com.practice.bank;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

@DisplayName("Level 6: idempotent transfers")
class Level6Test {

    private Bank bank;

    @BeforeEach
    void setUp() {
        bank = new Bank();
        bank.createAccount(1, "mia");
        bank.createAccount(2, "leo");
        bank.deposit(3, "mia", 10_000);
    }

    @Test
    @DisplayName("a retried request returns the first result and moves money only once")
    void retryReturnsFirstResult() {
        assertEquals(Optional.of(7_000L), bank.transferOnce(10, "req-1", "mia", "leo", 3_000));
        bank.deposit(11, "mia", 500);
        assertEquals(Optional.of(7_000L), bank.transferOnce(12, "req-1", "mia", "leo", 3_000),
                "a retry returns the original result, not the current balance");

        assertEquals(Optional.of(7_500L), bank.getBalance(13, "mia"));
        assertEquals(Optional.of(3_000L), bank.getBalance(13, "leo"));
        assertEquals(3, bank.getStatistics(14).transactionCount());
    }

    @Test
    @DisplayName("different request ids are different transfers")
    void differentRequestIds() {
        assertEquals(Optional.of(7_000L), bank.transferOnce(10, "req-1", "mia", "leo", 3_000));
        assertEquals(Optional.of(4_000L), bank.transferOnce(11, "req-2", "mia", "leo", 3_000));
        assertEquals(Optional.of(6_000L), bank.getBalance(12, "leo"));
    }

    @Test
    @DisplayName("reusing a request id for a different transfer is an error")
    void reusedIdForDifferentTransfer() {
        bank.transferOnce(10, "req-1", "mia", "leo", 3_000);
        bank.createAccount(11, "ava");

        assertThrows(IllegalArgumentException.class,
                () -> bank.transferOnce(12, "req-1", "mia", "leo", 2_000));
        assertThrows(IllegalArgumentException.class,
                () -> bank.transferOnce(13, "req-1", "mia", "ava", 3_000));
        assertEquals(Optional.of(7_000L), bank.getBalance(14, "mia"));
    }

    @Test
    @DisplayName("a failed request is remembered too: retrying it fails again")
    void failedRequestIsRemembered() {
        assertEquals(Optional.empty(), bank.transferOnce(10, "req-9", "mia", "leo", 50_000));
        bank.deposit(11, "mia", 100_000);
        assertEquals(Optional.empty(), bank.transferOnce(12, "req-9", "mia", "leo", 50_000));
        assertEquals(Optional.of(0L), bank.getBalance(13, "leo"));
    }

    @Test
    @DisplayName("transferOnce follows the normal transfer rules")
    void normalRulesApply() {
        assertEquals(Optional.empty(), bank.transferOnce(10, "req-1", "mia", "nobody", 100));
        assertEquals(Optional.empty(), bank.transferOnce(11, "req-2", "mia", "mia", 100));
        assertEquals(Optional.empty(), bank.transferOnce(12, "req-3", "mia", "leo", 0));
    }

    @Test
    @DisplayName("the same request arriving on many threads at once moves money only once")
    void concurrentDuplicates() throws Exception {
        for (int round = 1; round <= 50; round++) {
            String requestId = "req-" + round;
            Set<Optional<Long>> results = ConcurrentHashMap.newKeySet();

            Level5Test.runConcurrently(() -> results.add(bank.transferOnce(20, requestId, "mia", "leo", 100)));

            assertEquals(Set.of(Optional.of(10_000L - 100L * round)), results,
                    requestId + ": every caller must get the same result, and money must move once");
        }
        assertEquals(Optional.of(5_000L), bank.getBalance(21, "leo"));
    }
}
