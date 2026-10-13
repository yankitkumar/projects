package com.practice.bank;

/**
 * Bank-wide numbers.
 *
 * @param accountCount     how many accounts are open
 * @param transactionCount how many successful transactions there have been
 * @param totalVolumeCents the sum of all those transactions' amounts
 */
public record BankStats(int accountCount, int transactionCount, long totalVolumeCents) {

    /** The average transaction amount, or 0.0 if there have been no transactions. */
    public double averageTransactionCents() {
        return transactionCount == 0 ? 0.0 : (double) totalVolumeCents / transactionCount;
    }
}
