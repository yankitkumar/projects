package com.practice.bank;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static com.practice.bank.TransactionType.DEPOSIT;
import static com.practice.bank.TransactionType.TRANSFER;
import static com.practice.bank.TransactionType.WITHDRAWAL;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DisplayName("Level 2: reporting")
class Level2Test {

    private Bank bank;

    /*
     * Who spent what in this scenario:
     *   leo  6,000  (transfer 2,000 + withdrawal 4,000)
     *   ava  3,000  (transfer 3,000)
     *   mia  3,000  (withdrawal 3,000; she also deposited and received money, which is not spending)
     *   sam      0  (his only attempt failed)
     */
    @BeforeEach
    void setUp() {
        bank = new Bank();
        bank.createAccount(1, "mia");
        bank.createAccount(2, "leo");
        bank.createAccount(3, "ava");
        bank.createAccount(4, "sam");

        bank.deposit(5, "mia", 10_000);
        bank.deposit(6, "leo", 10_000);
        bank.deposit(7, "ava", 10_000);
        bank.deposit(8, "mia", 2_500);
        bank.withdraw(9, "mia", 3_000);
        bank.transfer(10, "leo", "ava", 2_000);
        bank.transfer(11, "ava", "mia", 3_000);
        bank.withdraw(12, "leo", 4_000);
        bank.withdraw(13, "mia", 1_000_000);   // fails
        bank.transfer(14, "sam", "mia", 1);    // fails
    }

    @Test
    @DisplayName("top spender is the account that spent the most")
    void topSpender() {
        assertEquals(List.of(new SpenderRank("leo", 6_000)), bank.getTopSpenders(20, 1));
    }

    @Test
    @DisplayName("ties are broken by account id, alphabetically")
    void tiesBrokenAlphabetically() {
        assertEquals(List.of(
                new SpenderRank("leo", 6_000),
                new SpenderRank("ava", 3_000),
                new SpenderRank("mia", 3_000)), bank.getTopSpenders(20, 3));
    }

    @Test
    @DisplayName("accounts that spent nothing are included; k larger than the bank returns everyone")
    void largeKReturnsEveryone() {
        assertEquals(List.of(
                new SpenderRank("leo", 6_000),
                new SpenderRank("ava", 3_000),
                new SpenderRank("mia", 3_000),
                new SpenderRank("sam", 0)), bank.getTopSpenders(20, 10));
    }

    @Test
    @DisplayName("k of zero or less returns an empty list")
    void nonPositiveK() {
        assertTrue(bank.getTopSpenders(20, 0).isEmpty());
        assertTrue(bank.getTopSpenders(21, -3).isEmpty());
    }

    @Test
    @DisplayName("an empty bank has no top spenders")
    void emptyBank() {
        assertTrue(new Bank().getTopSpenders(1, 5).isEmpty());
    }

    @Test
    @DisplayName("totals by type add up each kind of transaction")
    void totalsByType() {
        assertEquals(Map.of(DEPOSIT, 10_000L, TRANSFER, 2_000L, WITHDRAWAL, 4_000L),
                bank.getTotalsByType(20, "leo"));
    }

    @Test
    @DisplayName("totals by type include only what the account started, not money it received")
    void totalsExcludeReceivedMoney() {
        assertEquals(Map.of(DEPOSIT, 12_500L, WITHDRAWAL, 3_000L), bank.getTotalsByType(20, "mia"));
        assertEquals(Map.of(DEPOSIT, 10_000L, TRANSFER, 3_000L), bank.getTotalsByType(21, "ava"));
    }

    @Test
    @DisplayName("totals by type are empty for an account with no transactions, or an unknown account")
    void totalsEmpty() {
        assertTrue(bank.getTotalsByType(20, "sam").isEmpty());
        assertTrue(bank.getTotalsByType(21, "nobody").isEmpty());
    }
}
