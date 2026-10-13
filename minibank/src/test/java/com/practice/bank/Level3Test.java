package com.practice.bank;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static com.practice.bank.PaymentStatus.CANCELLED;
import static com.practice.bank.PaymentStatus.EXECUTED;
import static com.practice.bank.PaymentStatus.FAILED;
import static com.practice.bank.PaymentStatus.PENDING;
import static com.practice.bank.TransactionType.DEPOSIT;
import static com.practice.bank.TransactionType.PAYMENT;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DisplayName("Level 3: scheduled payments")
class Level3Test {

    private Bank bank;

    @BeforeEach
    void setUp() {
        bank = new Bank();
        bank.createAccount(1, "mia");
        bank.createAccount(1, "leo");
    }

    @Test
    @DisplayName("a payment runs when its due time arrives, not before")
    void runsAtDueTime() {
        bank.deposit(2, "mia", 10_000);
        assertEquals(Optional.of("payment1"), bank.schedulePayment(3, "mia", 2_500, 10));   // due at 13

        assertEquals(Optional.of(10_000L), bank.getBalance(12, "mia"));
        assertEquals(Optional.of(PENDING), bank.getPaymentStatus(12, "payment1"));

        assertEquals(Optional.of(7_500L), bank.getBalance(13, "mia"));
        assertEquals(Optional.of(EXECUTED), bank.getPaymentStatus(13, "payment1"));
    }

    @Test
    @DisplayName("due payments run before the operation that has the same timestamp")
    void runsBeforeOperationAtSameTime() {
        bank.deposit(2, "mia", 10_000);
        bank.schedulePayment(3, "mia", 2_500, 10);   // due at 13

        assertEquals(Optional.empty(), bank.withdraw(13, "mia", 10_000));
        assertEquals(Optional.of(0L), bank.withdraw(13, "mia", 7_500));
    }

    @Test
    @DisplayName("a payment without enough money fails and is not retried")
    void failsWithoutMoney() {
        bank.deposit(2, "mia", 1_000);
        bank.schedulePayment(3, "mia", 2_500, 5);   // due at 8

        assertEquals(Optional.of(FAILED), bank.getPaymentStatus(8, "payment1"));
        assertEquals(Optional.of(6_000L), bank.deposit(9, "mia", 5_000));
        assertEquals(Optional.of(FAILED), bank.getPaymentStatus(20, "payment1"));
        assertEquals(Optional.of(6_000L), bank.getBalance(21, "mia"));
    }

    @Test
    @DisplayName("a pending payment can be cancelled once, only by its own account")
    void cancel() {
        bank.deposit(2, "mia", 10_000);
        bank.schedulePayment(3, "mia", 2_500, 10);   // due at 13

        assertFalse(bank.cancelPayment(5, "leo", "payment1"), "leo does not own this payment");
        assertTrue(bank.cancelPayment(6, "mia", "payment1"));
        assertFalse(bank.cancelPayment(7, "mia", "payment1"), "already cancelled");
        assertFalse(bank.cancelPayment(8, "mia", "payment99"), "no such payment");

        assertEquals(Optional.of(CANCELLED), bank.getPaymentStatus(20, "payment1"));
        assertEquals(Optional.of(10_000L), bank.getBalance(21, "mia"));
    }

    @Test
    @DisplayName("cancelling at the due time is too late")
    void cancelAtDueTimeIsTooLate() {
        bank.deposit(2, "mia", 10_000);
        bank.schedulePayment(3, "mia", 2_500, 10);   // due at 13

        assertFalse(bank.cancelPayment(13, "mia", "payment1"));
        assertEquals(Optional.of(EXECUTED), bank.getPaymentStatus(14, "payment1"));
        assertEquals(Optional.of(7_500L), bank.getBalance(15, "mia"));
    }

    @Test
    @DisplayName("the payment due first runs first, even if it was scheduled later")
    void earliestDueRunsFirst() {
        bank.deposit(2, "mia", 3_000);
        bank.schedulePayment(3, "mia", 2_000, 20);   // payment1, due at 23
        bank.schedulePayment(4, "mia", 2_000, 10);   // payment2, due at 14

        assertEquals(Optional.of(FAILED), bank.getPaymentStatus(30, "payment1"));
        assertEquals(Optional.of(EXECUTED), bank.getPaymentStatus(30, "payment2"));
    }

    @Test
    @DisplayName("payments due at the same time run in the order they were scheduled")
    void sameDueTimeRunsInScheduledOrder() {
        bank.deposit(2, "mia", 95_000);
        for (int t = 3; t <= 11; t++) {
            bank.schedulePayment(t, "mia", 10_000, 100 - t);   // payment1..payment9, all due at 100
        }
        bank.schedulePayment(12, "mia", 6_000, 88);            // payment10, also due at 100

        // payment1..payment9 take 90,000, leaving 5,000: not enough for payment10.
        assertEquals(Optional.of(EXECUTED), bank.getPaymentStatus(100, "payment9"));
        assertEquals(Optional.of(FAILED), bank.getPaymentStatus(100, "payment10"));
        assertEquals(Optional.of(5_000L), bank.getBalance(100, "mia"));
    }

    @Test
    @DisplayName("invalid payments are rejected and do not use up an id")
    void scheduleValidation() {
        bank.deposit(2, "mia", 1_000);
        assertEquals(Optional.empty(), bank.schedulePayment(3, "nobody", 100, 5));
        assertEquals(Optional.empty(), bank.schedulePayment(4, "mia", 0, 5));
        assertEquals(Optional.empty(), bank.schedulePayment(5, "mia", 100, 0));
        assertEquals(Optional.of("payment1"), bank.schedulePayment(6, "mia", 100, 5));
        assertEquals(Optional.of("payment2"), bank.schedulePayment(7, "leo", 100, 5));
        assertEquals(Optional.empty(), bank.getPaymentStatus(8, "payment3"));
    }

    @Test
    @DisplayName("an executed payment counts as spending and as a transaction")
    void executedPaymentCountsEverywhere() {
        bank.deposit(2, "mia", 10_000);
        bank.deposit(3, "leo", 10_000);
        bank.withdraw(4, "leo", 3_000);
        bank.schedulePayment(5, "mia", 4_000, 5);    // due at 10
        bank.schedulePayment(6, "leo", 99_999, 4);   // due at 10, will fail

        assertEquals(List.of(new SpenderRank("mia", 4_000), new SpenderRank("leo", 3_000)),
                bank.getTopSpenders(10, 2));
        assertEquals(Map.of(DEPOSIT, 10_000L, PAYMENT, 4_000L), bank.getTotalsByType(11, "mia"));
        assertEquals(4, bank.getStatistics(12).transactionCount(),
                "2 deposits + 1 withdrawal + 1 executed payment (the failed one does not count)");
    }
}
