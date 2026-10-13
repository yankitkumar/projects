package com.practice.bank;

/** The lifecycle of a scheduled payment (Level 3). */
public enum PaymentStatus {
    /** Waiting for its due time. */
    PENDING,
    /** Ran at its due time and the money left the account. */
    EXECUTED,
    /** Ran at its due time but the account did not have enough money. */
    FAILED,
    /** Cancelled before its due time. */
    CANCELLED
}
