package com.practice.bank;

/** One row of the top-spenders report (Level 2). */
public record SpenderRank(String accountId, long totalSpentCents) {
}
