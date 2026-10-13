package com.practice.bank;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * An in-memory bank. README.md has the full specification for every level.
 *
 * Rules that apply to every level:
 *  - Every operation receives the current time as its first argument.
 *    Timestamps never decrease from one call to the next.
 *  - Money is a whole number of cents (a long), so there is no rounding.
 *  - An operation that cannot be completed returns Optional.empty() or false
 *    and changes nothing.
 */
public class Bank {

    private final Map<String, Account> accounts = new HashMap<>();
    private final List<Transaction> transactions = new ArrayList<>();

    // =====================================================================
    // Level 1: core operations. This code works, except for a few bugs.
    // =====================================================================

    /** Opens an empty account. Returns false if the id is already taken. */
    public boolean createAccount(long timestamp, String accountId) {
        if (accounts.containsKey(accountId)) {
            return false;
        }
        accounts.put(accountId, new Account(accountId));
        return true;
    }

    /**
     * Adds money to an account and returns the new balance.
     * Fails if the account does not exist or the amount is not positive.
     */
    public Optional<Long> deposit(long timestamp, String accountId, long amountCents) {
        Account account = accounts.get(accountId);
        if (account == null || amountCents <= 0) {
            return Optional.empty();
        }
        account.balanceCents += amountCents;
        transactions.add(new Transaction(timestamp, TransactionType.DEPOSIT, accountId, null, amountCents));
        return Optional.of(account.balanceCents);
    }

    /**
     * Takes money out of an account and returns the new balance.
     * Fails if the account does not exist, the amount is not positive,
     * or the account does not have enough money.
     */
    public Optional<Long> withdraw(long timestamp, String accountId, long amountCents) {
        Account account = accounts.get(accountId);
        if (account == null || amountCents <= 0) {
            return Optional.empty();
        }
        if (account.balanceCents <= amountCents) {
            return Optional.empty();
        }
        account.balanceCents -= amountCents;
        transactions.add(new Transaction(timestamp, TransactionType.WITHDRAWAL, accountId, null, amountCents));
        return Optional.of(account.balanceCents);
    }

    /**
     * Moves money from one account to a different account and returns the sender's new balance.
     * Fails if either account does not exist, both ids are the same account,
     * the amount is not positive, or the sender does not have enough money.
     */
    public Optional<Long> transfer(long timestamp, String fromId, String toId, long amountCents) {
        Account from = accounts.get(fromId);
        Account to = accounts.get(toId);
        if (from == null || to == null || fromId == toId || amountCents <= 0) {
            return Optional.empty();
        }
        if (from.balanceCents < amountCents) {
            return Optional.empty();
        }
        from.balanceCents -= amountCents;
        to.balanceCents += amountCents;
        transactions.add(new Transaction(timestamp, TransactionType.TRANSFER, fromId, toId, amountCents));
        return Optional.of(from.balanceCents);
    }

    /** Returns the current balance, or empty if the account does not exist. */
    public Optional<Long> getBalance(long timestamp, String accountId) {
        Account account = accounts.get(accountId);
        if (account == null) {
            return Optional.empty();
        }
        return Optional.of(account.balanceCents);
    }

    /** Bank-wide numbers. A transfer counts as one transaction. */
    public BankStats getStatistics(long timestamp) {
        int volume = 0;
        for (Transaction t : transactions) {
            volume += t.amountCents();
        }
        return new BankStats(accounts.size(), transactions.size(), volume);
    }

    // =====================================================================
    // Level 2: reporting
    // =====================================================================

    /**
     * The k accounts that spent the most money, highest first.
     * Spending = successful withdrawals + transfers sent + executed scheduled payments.
     * Ties go to the alphabetically smaller account id. Accounts that spent nothing
     * are included. Returns every account if there are fewer than k; empty if k <= 0.
     */
    public List<SpenderRank> getTopSpenders(long timestamp, int k) {
        throw new UnsupportedOperationException("Level 2: not implemented yet");
    }

    /**
     * Total amount per transaction type, for the transactions this account started.
     * Only types with at least one transaction appear. Unknown account: empty map.
     */
    public Map<TransactionType, Long> getTotalsByType(long timestamp, String accountId) {
        throw new UnsupportedOperationException("Level 2: not implemented yet");
    }

    // =====================================================================
    // Level 3: scheduled payments (see README for the full rules)
    // =====================================================================

    /**
     * Schedules a payment out of the account, due at timestamp + delay.
     * Returns its id: "payment1", "payment2", ... counting successful calls bank-wide.
     * Fails if the account does not exist, the amount is not positive, or delay is not positive.
     */
    public Optional<String> schedulePayment(long timestamp, String accountId, long amountCents, long delay) {
        throw new UnsupportedOperationException("Level 3: not implemented yet");
    }

    /** Cancels a pending payment that belongs to this account. Returns true if it was cancelled. */
    public boolean cancelPayment(long timestamp, String accountId, String paymentId) {
        throw new UnsupportedOperationException("Level 3: not implemented yet");
    }

    /** The payment's status, or empty if there is no payment with this id. */
    public Optional<PaymentStatus> getPaymentStatus(long timestamp, String paymentId) {
        throw new UnsupportedOperationException("Level 3: not implemented yet");
    }

    // =====================================================================
    // Level 4: merging accounts and balance history (see README)
    // =====================================================================

    /** Moves everything from sourceId into targetId and closes sourceId. */
    public boolean mergeAccounts(long timestamp, String targetId, String sourceId) {
        throw new UnsupportedOperationException("Level 4: not implemented yet");
    }

    /** The account's balance at the end of atTimestamp, or empty if it did not exist then. */
    public Optional<Long> getBalanceAt(long timestamp, String accountId, long atTimestamp) {
        throw new UnsupportedOperationException("Level 4: not implemented yet");
    }

    // =====================================================================
    // Level 5: thread safety. No new methods: make every method above safe
    // to call from many threads at the same time.
    // =====================================================================

    // =====================================================================
    // Level 6: idempotent transfers (see README)
    // =====================================================================

    /**
     * Like transfer, but safe to retry: a repeated requestId returns the first result
     * and moves no money. Reusing a requestId for a different transfer throws
     * IllegalArgumentException.
     */
    public Optional<Long> transferOnce(long timestamp, String requestId,
                                       String fromId, String toId, long amountCents) {
        throw new UnsupportedOperationException("Level 6: not implemented yet");
    }
}
