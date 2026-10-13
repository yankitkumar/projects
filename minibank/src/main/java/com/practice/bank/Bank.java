package com.practice.bank;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.NavigableMap;
import java.util.Objects;
import java.util.Optional;
import java.util.PriorityQueue;
import java.util.TreeMap;

/**
 * Reference solution for all six levels. SOLUTION_NOTES.md explains the thinking behind it.
 *
 * Thread safety (Level 5): every public method is synchronized on the bank, so only one
 * operation runs at a time. That is the simplest design that is correct; the notes discuss
 * finer-grained alternatives and their trade-offs.
 */
public class Bank {

    private final Map<String, Account> accounts = new HashMap<>();
    private final List<Transaction> transactions = new ArrayList<>();

    // Level 3: every payment by id, plus a queue of the ones still waiting, earliest due first.
    private final Map<String, ScheduledPayment> payments = new HashMap<>();
    private final PriorityQueue<ScheduledPayment> paymentQueue = new PriorityQueue<>(
            Comparator.comparingLong((ScheduledPayment p) -> p.dueAt)
                      .thenComparingLong(p -> p.sequence));   // NOT the id string: "payment10" < "payment2"
    private long paymentSequence = 0;

    // Level 4: per account, timestamp -> balance after the last change at that timestamp.
    private final Map<String, NavigableMap<Long, Long>> balanceHistory = new HashMap<>();
    private final Map<String, Long> closedAt = new HashMap<>();

    // Level 6: every transferOnce request seen so far, with its result.
    private final Map<String, TransferRequest> transferRequests = new HashMap<>();

    // =====================================================================
    // Level 1
    // =====================================================================

    public synchronized boolean createAccount(long timestamp, String accountId) {
        runDuePayments(timestamp);
        if (accounts.containsKey(accountId)) {
            return false;
        }
        Account account = new Account(accountId);
        accounts.put(accountId, account);
        balanceHistory.put(accountId, new TreeMap<>());
        recordBalance(account, timestamp);
        return true;
    }

    public synchronized Optional<Long> deposit(long timestamp, String accountId, long amountCents) {
        runDuePayments(timestamp);
        Account account = accounts.get(accountId);
        if (account == null || amountCents <= 0) {
            return Optional.empty();
        }
        account.balanceCents += amountCents;
        record(timestamp, TransactionType.DEPOSIT, accountId, null, amountCents);
        recordBalance(account, timestamp);
        return Optional.of(account.balanceCents);
    }

    public synchronized Optional<Long> withdraw(long timestamp, String accountId, long amountCents) {
        runDuePayments(timestamp);
        Account account = accounts.get(accountId);
        if (account == null || amountCents <= 0) {
            return Optional.empty();
        }
        if (account.balanceCents < amountCents) {   // FIX 1: was <=, which refused to empty the account
            return Optional.empty();
        }
        account.balanceCents -= amountCents;
        record(timestamp, TransactionType.WITHDRAWAL, accountId, null, amountCents);
        recordBalance(account, timestamp);
        return Optional.of(account.balanceCents);
    }

    public synchronized Optional<Long> transfer(long timestamp, String fromId, String toId, long amountCents) {
        runDuePayments(timestamp);
        Account from = accounts.get(fromId);
        Account to = accounts.get(toId);
        // FIX 2: was fromId == toId, which compares String objects, not their text
        if (from == null || to == null || fromId.equals(toId) || amountCents <= 0) {
            return Optional.empty();
        }
        if (from.balanceCents < amountCents) {
            return Optional.empty();
        }
        from.balanceCents -= amountCents;
        to.balanceCents += amountCents;
        record(timestamp, TransactionType.TRANSFER, fromId, toId, amountCents);
        recordBalance(from, timestamp);
        recordBalance(to, timestamp);
        return Optional.of(from.balanceCents);
    }

    public synchronized Optional<Long> getBalance(long timestamp, String accountId) {
        runDuePayments(timestamp);
        return Optional.ofNullable(accounts.get(accountId)).map(account -> account.balanceCents);
    }

    public synchronized BankStats getStatistics(long timestamp) {
        runDuePayments(timestamp);
        long volume = 0;   // FIX 3: was int; "int += long" compiles silently and overflows above ~$21M
        for (Transaction t : transactions) {
            volume += t.amountCents();
        }
        return new BankStats(accounts.size(), transactions.size(), volume);
    }

    // =====================================================================
    // Level 2
    // =====================================================================

    public synchronized List<SpenderRank> getTopSpenders(long timestamp, int k) {
        runDuePayments(timestamp);
        if (k <= 0) {
            return List.of();
        }
        Map<String, Long> spent = new HashMap<>();
        for (String accountId : accounts.keySet()) {
            spent.put(accountId, 0L);   // accounts that spent nothing still appear
        }
        for (Transaction t : transactions) {
            if (t.type() != TransactionType.DEPOSIT && spent.containsKey(t.accountId())) {
                spent.merge(t.accountId(), t.amountCents(), Long::sum);
            }
        }
        return spent.entrySet().stream()
                .map(e -> new SpenderRank(e.getKey(), e.getValue()))
                .sorted(Comparator.comparingLong(SpenderRank::totalSpentCents).reversed()
                                  .thenComparing(SpenderRank::accountId))
                .limit(k)
                .toList();
    }

    public synchronized Map<TransactionType, Long> getTotalsByType(long timestamp, String accountId) {
        runDuePayments(timestamp);
        Map<TransactionType, Long> totals = new EnumMap<>(TransactionType.class);
        if (!accounts.containsKey(accountId)) {
            return totals;
        }
        for (Transaction t : transactions) {
            if (t.accountId().equals(accountId)) {
                totals.merge(t.type(), t.amountCents(), Long::sum);
            }
        }
        return totals;
    }

    // =====================================================================
    // Level 3
    // =====================================================================

    public synchronized Optional<String> schedulePayment(long timestamp, String accountId,
                                                         long amountCents, long delay) {
        runDuePayments(timestamp);
        if (!accounts.containsKey(accountId) || amountCents <= 0 || delay <= 0) {
            return Optional.empty();
        }
        long sequence = ++paymentSequence;
        ScheduledPayment payment = new ScheduledPayment(
                "payment" + sequence, sequence, accountId, amountCents, timestamp + delay);
        payments.put(payment.id, payment);
        paymentQueue.add(payment);
        return Optional.of(payment.id);
    }

    public synchronized boolean cancelPayment(long timestamp, String accountId, String paymentId) {
        runDuePayments(timestamp);   // a payment due right now runs first, so it can no longer be cancelled
        ScheduledPayment payment = payments.get(paymentId);
        if (payment == null || payment.status != PaymentStatus.PENDING || !payment.accountId.equals(accountId)) {
            return false;
        }
        payment.status = PaymentStatus.CANCELLED;   // left in the queue; runDuePayments skips it
        return true;
    }

    public synchronized Optional<PaymentStatus> getPaymentStatus(long timestamp, String paymentId) {
        runDuePayments(timestamp);
        return Optional.ofNullable(payments.get(paymentId)).map(payment -> payment.status);
    }

    /** Called first by every public method: runs every pending payment due at or before now. */
    private void runDuePayments(long now) {
        while (!paymentQueue.isEmpty() && paymentQueue.peek().dueAt <= now) {
            ScheduledPayment payment = paymentQueue.poll();
            if (payment.status != PaymentStatus.PENDING) {
                continue;   // cancelled while it was waiting
            }
            Account account = accounts.get(payment.accountId);
            if (account == null || account.balanceCents < payment.amountCents) {
                payment.status = PaymentStatus.FAILED;
                continue;
            }
            account.balanceCents -= payment.amountCents;
            payment.status = PaymentStatus.EXECUTED;
            // Recorded at its due time, not at "now": it logically happened then (matters for Level 4).
            record(payment.dueAt, TransactionType.PAYMENT, payment.accountId, null, payment.amountCents);
            recordBalance(account, payment.dueAt);
        }
    }

    // =====================================================================
    // Level 4
    // =====================================================================

    public synchronized boolean mergeAccounts(long timestamp, String targetId, String sourceId) {
        runDuePayments(timestamp);
        if (targetId.equals(sourceId)) {
            return false;
        }
        Account target = accounts.get(targetId);
        Account source = accounts.get(sourceId);
        if (target == null || source == null) {
            return false;
        }

        target.balanceCents += source.balanceCents;
        accounts.remove(sourceId);
        closedAt.put(sourceId, timestamp);
        recordBalance(target, timestamp);

        for (ScheduledPayment payment : payments.values()) {
            if (payment.status == PaymentStatus.PENDING && payment.accountId.equals(sourceId)) {
                payment.accountId = targetId;
            }
        }
        // The target takes over the source's history for reporting.
        transactions.replaceAll(t -> t.accountId().equals(sourceId)
                ? new Transaction(t.timestamp(), t.type(), targetId, t.counterpartyId(), t.amountCents())
                : t);
        return true;
    }

    public synchronized Optional<Long> getBalanceAt(long timestamp, String accountId, long atTimestamp) {
        runDuePayments(timestamp);
        NavigableMap<Long, Long> history = balanceHistory.get(accountId);
        if (history == null) {
            return Optional.empty();
        }
        Long closed = closedAt.get(accountId);
        if (closed != null && atTimestamp >= closed) {
            return Optional.empty();
        }
        Map.Entry<Long, Long> latest = history.floorEntry(atTimestamp);   // last change at or before atTimestamp
        return latest == null ? Optional.empty() : Optional.of(latest.getValue());
    }

    // =====================================================================
    // Level 6
    // =====================================================================

    public synchronized Optional<Long> transferOnce(long timestamp, String requestId,
                                                    String fromId, String toId, long amountCents) {
        runDuePayments(timestamp);
        TransferRequest previous = transferRequests.get(requestId);
        if (previous != null) {
            if (!previous.isSameTransfer(fromId, toId, amountCents)) {
                throw new IllegalArgumentException(
                        "requestId " + requestId + " was already used for a different transfer");
            }
            return previous.result();
        }
        Optional<Long> result = transfer(timestamp, fromId, toId, amountCents);
        transferRequests.put(requestId, new TransferRequest(fromId, toId, amountCents, result));
        return result;
    }

    // =====================================================================
    // Helpers
    // =====================================================================

    private void record(long timestamp, TransactionType type, String accountId,
                        String counterpartyId, long amountCents) {
        transactions.add(new Transaction(timestamp, type, accountId, counterpartyId, amountCents));
    }

    private void recordBalance(Account account, long timestamp) {
        balanceHistory.get(account.id).put(timestamp, account.balanceCents);
    }

    private static final class ScheduledPayment {
        final String id;
        final long sequence;
        final long amountCents;
        final long dueAt;
        String accountId;   // changes if the account is merged into another
        PaymentStatus status = PaymentStatus.PENDING;

        ScheduledPayment(String id, long sequence, String accountId, long amountCents, long dueAt) {
            this.id = id;
            this.sequence = sequence;
            this.accountId = accountId;
            this.amountCents = amountCents;
            this.dueAt = dueAt;
        }
    }

    private record TransferRequest(String fromId, String toId, long amountCents, Optional<Long> result) {
        boolean isSameTransfer(String otherFrom, String otherTo, long otherAmount) {
            return Objects.equals(fromId, otherFrom) && Objects.equals(toId, otherTo) && amountCents == otherAmount;
        }
    }
}
