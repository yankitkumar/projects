package com.practice.bank;

/**
 * One successful movement of money.
 *
 * @param timestamp      when it happened
 * @param type           what kind of movement it was
 * @param accountId      the account that started it: the depositor, the withdrawer,
 *                       the sender of a transfer, or the payer of a scheduled payment
 * @param counterpartyId the receiving account for a TRANSFER, otherwise null
 * @param amountCents    how much money moved, always positive
 */
public record Transaction(long timestamp, TransactionType type, String accountId,
                          String counterpartyId, long amountCents) {
}
