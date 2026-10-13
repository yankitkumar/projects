package com.practice.bank;

public enum TransactionType {
    DEPOSIT,
    WITHDRAWAL,
    TRANSFER,
    /** A scheduled payment that was executed (Level 3). */
    PAYMENT
}
