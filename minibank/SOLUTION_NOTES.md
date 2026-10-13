# MiniBank solution notes

Open these only once your own tests pass, or when your time is up. For each level they cover the
key idea, the trap the tests are built around, and the follow-up question an interviewer is likely
to ask. The code is in `src/main/java/com/practice/bank/Bank.java`.

---

## Level 1: the three bugs

| Bug | Where | Fix | The general lesson |
|---|---|---|---|
| `balance <= amount` | `withdraw` | `balance < amount` | At every boundary, ask what happens when the two values are exactly equal. |
| `fromId == toId` | `transfer` | `fromId.equals(toId)` | `==` compares object identity. String literals often share one object, so this bug hides in tests and appears in production. |
| `int volume` | `getStatistics` | `long volume` | `int += long` compiles because compound assignment adds a hidden cast. It silently overflows above 2,147,483,647 cents (about $21M). |

**Likely follow-up:** "How would you have found these without the tests?" Talk about compiler
warnings (`-Xlint:all` flags the third bug), static analysis (SpotBugs flags `==` on Strings), and
writing a test at each boundary.

---

## Level 2: reporting

This is the same pattern as the clinic exercise: one pass over the transactions, filter, group with
`Map.merge`, then shape the result.

- **Trap 1, ties:** without `.thenComparing(SpenderRank::accountId)`, the order of tied accounts
  depends on `HashMap` iteration order, which is effectively random. The test creates `ava` after
  `mia`, so code that relies on insertion order fails too.
- **Trap 2, accounts that spent nothing:** they only appear if you put every account into the map
  with 0 first.
- **Cost:** O(T + A log A) per call, for T transactions and A accounts.

**Likely follow-up:** "This scans every transaction on every call. Make it faster." Keep a running
`spentCents` on each `Account`, updated as money leaves. Each call is then O(A log A), or
O(A log k) with a size-k min-heap.

---

## Level 3: scheduled payments

- **Key idea:** a `PriorityQueue` ordered by `(dueAt, sequence)`, plus one private
  `runDuePayments(now)` called first by every public method. That method is the only place
  payments run, so the timing rules are enforced in one spot.
- **Trap 1, string ids:** comparing `"payment10"` with `"payment2"` as strings puts payment10 first.
  The test schedules ten payments due at the same time, so this ordering mistake makes the wrong one fail.
- **Trap 2, cancelling at the due time:** `cancelPayment` must run due payments *before* it checks
  the status, so a payment due exactly now has already run.
- **Lazy deletion:** cancelling only flips the status. The payment is skipped when it reaches the
  front of the queue. `PriorityQueue.remove(Object)` would be O(n).
- **Payments run "late":** a payment due at 10 may actually run during a call at 50. It is still
  recorded at time 10, which Level 4 depends on.

**Likely follow-up:** "What if this ran on a real clock instead of on each call?" A scheduler thread
(`ScheduledExecutorService`), and the locking that requires. That leads into Level 5.

---

## Level 4: merging and history

- **History:** each account has a `TreeMap<timestamp, balanceAfter>`. Writing the same timestamp
  twice overwrites the entry, so the map holds the balance at the end of that timestamp, as the spec
  requires. `floorEntry(at)` then answers "balance at time `at`" in O(log n).
- **Why the history stays in time order:** every operation runs due payments first, so a payment
  is never recorded earlier than a change already in the map.
- **Closed accounts:** the `closedAt` map makes `getBalanceAt` return empty from the merge time
  onwards, while earlier queries still see the old history.
- **Moving pending payments:** change `accountId` on each pending payment. Their place in the queue
  is unaffected, because ordering uses only `dueAt` and `sequence`.
- **Reports:** `transactions.replaceAll(...)` reassigns the source's transactions to the target.
  It's simple and O(T) per merge.

**Likely follow-up:** "Merges are frequent and there are millions of transactions." Don't rewrite
anything. Keep an alias map from closed id to the id it merged into (a union-find structure if
merges chain), and resolve ids when reading. Or keep per-account running totals, which merge in
O(1).

---

## Level 5: thread safety

- **Solution here:** every public method is `synchronized`. It's correct because only one operation
  runs at a time, so no thread ever sees a half-finished change.
- **What it costs:** there's no parallelism. Transfers between unrelated accounts still wait for
  each other.
- **Why reads need the lock:** `reportsDuringTraffic` iterates the transaction list while other
  threads add to it. Without the lock that throws `ConcurrentModificationException`, or quietly
  reads inconsistent state.
- **The stretch design, per-account locks:**
  ```java
  Account first  = from.id.compareTo(to.id) < 0 ? from : to;
  Account second = first == from ? to : from;
  first.lock.lock();
  try {
      second.lock.lock();
      try { /* check funds, move money */ } finally { second.lock.unlock(); }
  } finally { first.lock.unlock(); }
  ```
  Taking the locks in a fixed order is what prevents deadlock. Without it, thread 1 holds A and waits
  for B while thread 2 holds B and waits for A. The shared structures still need their own protection
  (`ConcurrentHashMap` for accounts, a concurrent queue or a separate lock for the transaction log and
  payment queue). That shared state is why the coarse lock is a reasonable default.

**Likely follow-up:** "How would you do this with a database?" Use a transaction per transfer, with
`SELECT ... FOR UPDATE` on both rows in a consistent order (pessimistic locking), or a `@Version`
column with a retry when it conflicts (optimistic locking).

---

## Level 6: idempotent transfers

- **Key idea:** a map from `requestId` to a record of `(from, to, amount, result)`. The lookup and
  the insert happen under the same lock, so check-then-act is atomic.
- **Why remember failures:** the client asked for "this exact operation, once". If a retry quietly
  succeeded later, two retries of the same request could have different effects, which defeats the
  point of a request id. A client that wants to try again sends a new id.
- **Why reject different details:** that's almost always a client bug, such as reusing an id. Failing
  loudly is safer than guessing which request was meant.

**Likely follow-up:** "The map grows forever." In practice, keys expire after a retention window
(for example 24 hours), and they're stored in the database in the same transaction as the transfer
itself, so a crash can't leave one without the other.
