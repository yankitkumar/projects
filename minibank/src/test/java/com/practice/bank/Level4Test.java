package com.practice.bank;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static com.practice.bank.PaymentStatus.CANCELLED;
import static com.practice.bank.PaymentStatus.EXECUTED;
import static com.practice.bank.TransactionType.DEPOSIT;
import static com.practice.bank.TransactionType.TRANSFER;
import static com.practice.bank.TransactionType.WITHDRAWAL;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DisplayName("Level 4: merging accounts and balance history")
class Level4Test {

    private Bank bank;

    @BeforeEach
    void setUp() {
        bank = new Bank();
        bank.createAccount(1, "mia");
        bank.createAccount(2, "leo");
    }

    // ---------------------------------------------------------------- balance history

    @Test
    @DisplayName("getBalanceAt returns the balance after everything that happened up to that time")
    void balanceHistory() {
        bank.deposit(5, "mia", 10_000);
        bank.withdraw(10, "mia", 3_000);
        bank.deposit(10, "mia", 500);          // same timestamp as the withdrawal
        bank.transfer(20, "mia", "leo", 2_000);

        assertEquals(Optional.of(0L), bank.getBalanceAt(30, "mia", 1));
        assertEquals(Optional.of(0L), bank.getBalanceAt(30, "mia", 4));
        assertEquals(Optional.of(10_000L), bank.getBalanceAt(30, "mia", 5));
        assertEquals(Optional.of(10_000L), bank.getBalanceAt(30, "mia", 9));
        assertEquals(Optional.of(7_500L), bank.getBalanceAt(30, "mia", 10));
        assertEquals(Optional.of(5_500L), bank.getBalanceAt(30, "mia", 25));
        assertEquals(Optional.of(0L), bank.getBalanceAt(30, "leo", 19));
        assertEquals(Optional.of(2_000L), bank.getBalanceAt(30, "leo", 20));
    }

    @Test
    @DisplayName("getBalanceAt is empty before the account was created, or for an unknown account")
    void balanceBeforeCreation() {
        assertEquals(Optional.empty(), bank.getBalanceAt(10, "leo", 1));
        assertEquals(Optional.empty(), bank.getBalanceAt(10, "nobody", 5));
    }

    @Test
    @DisplayName("a scheduled payment changes the history at its due time, even if it ran later")
    void paymentRecordedAtDueTime() {
        bank.deposit(2, "mia", 10_000);
        bank.schedulePayment(2, "mia", 1_000, 8);   // due at 10
        bank.deposit(50, "mia", 1);                 // the payment actually runs during this call

        assertEquals(Optional.of(10_000L), bank.getBalanceAt(60, "mia", 9));
        assertEquals(Optional.of(9_000L), bank.getBalanceAt(60, "mia", 10));
        assertEquals(Optional.of(9_000L), bank.getBalanceAt(60, "mia", 49));
        assertEquals(Optional.of(9_001L), bank.getBalanceAt(60, "mia", 50));
    }

    @Test
    @DisplayName("getBalanceAt runs due payments first, too")
    void balanceAtRunsDuePayments() {
        bank.deposit(2, "mia", 10_000);
        bank.schedulePayment(3, "mia", 1_000, 7);   // due at 10, nothing else happens before the query

        assertEquals(Optional.of(9_000L), bank.getBalanceAt(40, "mia", 10));
    }

    // ---------------------------------------------------------------- merging

    @Test
    @DisplayName("merging adds the balances together and closes the source account")
    void mergeCombinesAndCloses() {
        bank.deposit(3, "mia", 10_000);
        bank.deposit(4, "leo", 4_000);

        assertTrue(bank.mergeAccounts(10, "mia", "leo"));
        assertEquals(Optional.of(14_000L), bank.getBalance(11, "mia"));
        assertEquals(Optional.empty(), bank.getBalance(11, "leo"));
        assertEquals(Optional.empty(), bank.deposit(12, "leo", 100));
        assertEquals(Optional.empty(), bank.transfer(13, "mia", "leo", 100));

        BankStats stats = bank.getStatistics(14);
        assertEquals(1, stats.accountCount());
        assertEquals(2, stats.transactionCount(), "a merge is not a transaction");
    }

    @Test
    @DisplayName("merge fails for the same account twice or an unknown account")
    void mergeValidation() {
        assertFalse(bank.mergeAccounts(3, "mia", "mia"));
        assertFalse(bank.mergeAccounts(4, "mia", "nobody"));
        assertFalse(bank.mergeAccounts(5, "nobody", "mia"));
        assertTrue(bank.mergeAccounts(6, "mia", "leo"));
        assertFalse(bank.mergeAccounts(7, "mia", "leo"), "leo is already closed");
    }

    @Test
    @DisplayName("the source's pending payments move to the target account")
    void pendingPaymentsMove() {
        bank.deposit(3, "mia", 10_000);
        bank.deposit(4, "leo", 4_000);
        bank.schedulePayment(5, "leo", 3_000, 18);   // payment1, due at 23
        bank.schedulePayment(6, "leo", 500, 19);     // payment2, due at 25
        assertTrue(bank.mergeAccounts(10, "mia", "leo"));

        assertFalse(bank.cancelPayment(11, "leo", "payment2"), "leo no longer exists");
        assertTrue(bank.cancelPayment(12, "mia", "payment2"), "the payment now belongs to mia");

        assertEquals(Optional.of(EXECUTED), bank.getPaymentStatus(30, "payment1"));
        assertEquals(Optional.of(CANCELLED), bank.getPaymentStatus(30, "payment2"));
        assertEquals(Optional.of(11_000L), bank.getBalance(30, "mia"));
    }

    @Test
    @DisplayName("the target takes over the source's transaction history in reports")
    void targetInheritsHistory() {
        bank.createAccount(3, "ava");
        bank.deposit(4, "mia", 10_000);
        bank.deposit(5, "leo", 10_000);
        bank.deposit(6, "ava", 10_000);
        bank.withdraw(7, "mia", 1_000);
        bank.withdraw(8, "leo", 2_500);
        bank.withdraw(9, "ava", 3_000);
        bank.transfer(10, "leo", "mia", 1_000);
        // Spent so far: mia 1,000, leo 3,500, ava 3,000

        assertTrue(bank.mergeAccounts(20, "mia", "leo"));

        assertEquals(List.of(new SpenderRank("mia", 4_500), new SpenderRank("ava", 3_000)),
                bank.getTopSpenders(21, 5));
        assertEquals(Map.of(DEPOSIT, 20_000L, WITHDRAWAL, 3_500L, TRANSFER, 1_000L),
                bank.getTotalsByType(22, "mia"));
        assertTrue(bank.getTotalsByType(23, "leo").isEmpty());
    }

    @Test
    @DisplayName("balance history before and after a merge")
    void historyAcrossMerge() {
        bank.deposit(3, "mia", 10_000);
        bank.deposit(4, "leo", 4_000);
        assertTrue(bank.mergeAccounts(10, "mia", "leo"));

        assertEquals(Optional.of(4_000L), bank.getBalanceAt(20, "leo", 9));
        assertEquals(Optional.empty(), bank.getBalanceAt(20, "leo", 10));
        assertEquals(Optional.empty(), bank.getBalanceAt(20, "leo", 15));
        assertEquals(Optional.of(10_000L), bank.getBalanceAt(20, "mia", 9));
        assertEquals(Optional.of(14_000L), bank.getBalanceAt(20, "mia", 10));
    }
}
