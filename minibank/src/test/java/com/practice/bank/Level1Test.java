package com.practice.bank;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DisplayName("Level 1: core operations (find and fix the bugs)")
class Level1Test {

    private Bank bank;

    @BeforeEach
    void setUp() {
        bank = new Bank();
        bank.createAccount(1, "alice");
        bank.createAccount(2, "bob");
    }

    @Test
    @DisplayName("createAccount rejects an id that is already taken")
    void createAccountRejectsDuplicates() {
        assertTrue(bank.createAccount(3, "carol"));
        assertFalse(bank.createAccount(4, "carol"));
        assertFalse(bank.createAccount(5, "alice"));
    }

    @Test
    @DisplayName("deposit returns the new balance")
    void depositReturnsNewBalance() {
        assertEquals(Optional.of(5_000L), bank.deposit(10, "alice", 5_000));
        assertEquals(Optional.of(7_500L), bank.deposit(11, "alice", 2_500));
        assertEquals(Optional.of(7_500L), bank.getBalance(12, "alice"));
    }

    @Test
    @DisplayName("deposit fails for an unknown account or a non-positive amount")
    void depositValidation() {
        assertEquals(Optional.empty(), bank.deposit(10, "nobody", 5_000));
        assertEquals(Optional.empty(), bank.deposit(11, "alice", 0));
        assertEquals(Optional.empty(), bank.deposit(12, "alice", -100));
        assertEquals(Optional.of(0L), bank.getBalance(13, "alice"));
    }

    @Test
    @DisplayName("withdraw returns the new balance")
    void withdrawReturnsNewBalance() {
        bank.deposit(10, "alice", 5_000);
        assertEquals(Optional.of(3_000L), bank.withdraw(11, "alice", 2_000));
    }

    @Test
    @DisplayName("withdraw fails without enough money and changes nothing")
    void withdrawInsufficientFunds() {
        bank.deposit(10, "alice", 5_000);
        assertEquals(Optional.empty(), bank.withdraw(11, "alice", 5_001));
        assertEquals(Optional.of(5_000L), bank.getBalance(12, "alice"));
    }

    @Test
    @DisplayName("withdraw can take out the entire balance")
    void withdrawEntireBalance() {
        bank.deposit(10, "alice", 5_000);
        assertEquals(Optional.of(0L), bank.withdraw(11, "alice", 5_000));
    }

    @Test
    @DisplayName("transfer moves money and returns the sender's new balance")
    void transferMovesMoney() {
        bank.deposit(10, "alice", 5_000);
        assertEquals(Optional.of(3_500L), bank.transfer(11, "alice", "bob", 1_500));
        assertEquals(Optional.of(1_500L), bank.getBalance(12, "bob"));
    }

    @Test
    @DisplayName("transfer fails when either account is unknown")
    void transferUnknownAccounts() {
        bank.deposit(10, "alice", 5_000);
        assertEquals(Optional.empty(), bank.transfer(11, "alice", "nobody", 1_000));
        assertEquals(Optional.empty(), bank.transfer(12, "nobody", "alice", 1_000));
        assertEquals(Optional.of(5_000L), bank.getBalance(13, "alice"));
    }

    @Test
    @DisplayName("transfer fails without enough money and changes nothing")
    void transferInsufficientFunds() {
        bank.deposit(10, "alice", 1_000);
        assertEquals(Optional.empty(), bank.transfer(11, "alice", "bob", 1_001));
        assertEquals(Optional.of(1_000L), bank.getBalance(12, "alice"));
        assertEquals(Optional.of(0L), bank.getBalance(13, "bob"));
    }

    @Test
    @DisplayName("transfer to the same account is rejected")
    void transferToSelfIsRejected() {
        bank.deposit(10, "alice", 5_000);
        // In a real system ids arrive from outside the program, e.g. parsed from a request.
        String aliceFromRequest = new StringBuilder("ali").append("ce").toString();
        assertEquals(Optional.empty(), bank.transfer(11, "alice", aliceFromRequest, 1_000));
        assertEquals(1, bank.getStatistics(12).transactionCount(),
                "a rejected transfer must not be recorded");
    }

    @Test
    @DisplayName("statistics count successful transactions only")
    void statisticsCountSuccessfulTransactions() {
        bank.deposit(10, "alice", 5_000);
        bank.withdraw(11, "alice", 1_000);
        bank.transfer(12, "alice", "bob", 2_000);
        bank.withdraw(13, "bob", 9_999);           // fails: not enough money
        bank.transfer(14, "bob", "nobody", 100);   // fails: unknown account

        BankStats stats = bank.getStatistics(15);
        assertEquals(2, stats.accountCount());
        assertEquals(3, stats.transactionCount());
        assertEquals(8_000L, stats.totalVolumeCents());
        assertEquals(8_000.0 / 3, stats.averageTransactionCents(), 1e-9);
    }

    @Test
    @DisplayName("statistics on a bank with no transactions")
    void statisticsWhenEmpty() {
        BankStats stats = new Bank().getStatistics(1);
        assertEquals(0, stats.accountCount());
        assertEquals(0, stats.transactionCount());
        assertEquals(0.0, stats.averageTransactionCents(), 1e-9);
    }

    @Test
    @DisplayName("statistics handle large amounts of money")
    void statisticsHandleLargeAmounts() {
        bank.deposit(10, "alice", 2_000_000_000L);   // $20 million
        bank.deposit(11, "bob", 2_000_000_000L);
        assertEquals(4_000_000_000L, bank.getStatistics(12).totalVolumeCents());
    }
}
