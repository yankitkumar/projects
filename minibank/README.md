# MiniBank: a Java practice project that grows with you

MiniBank is a small in-memory bank. You build it one level at a time, and each level adds a
feature on top of the last. Level 1 has the same shape as a typical coding interview: working code
with bugs in it. Later levels move into topics senior backend interviews ask about: time-based
logic, history, thread safety and idempotency.

Each level has its own test class. A level is done when its tests pass **and every earlier
level's tests still pass**.

| Level | What you build | What it practises | Target time |
|---|---|---|---|
| 1 | Fix three bugs in the starter code | Reading code, debugging from test output | 20 min |
| 2 | Top spenders, totals by type | Grouping, aggregating, sorting with tie-breaks | 30 min |
| 3 | Scheduled payments | Priority queues, processing events in time order | 45 min |
| 4 | Merging accounts, balance at any past time | History with `TreeMap`, changing a design you already built | 60 min |
| 5 | Make it thread-safe | Locks, race conditions, deadlocks | 45 min |
| 6 | Idempotent transfers | Safe retries, as in real payment APIs | 30 min |

---

## Setup

You need **Java 17 or newer** and **Maven**. IntelliJ IDEA includes Maven: choose *File → Open*
and pick this `minibank` folder (not the repository root).

Run one level from the terminal, inside the `minibank` folder:

```bash
mvn test -Dtest=Level1Test
```

Run a level together with every earlier level:

```bash
mvn test -Dtest='Level1Test,Level2Test,Level3Test'
```

In IntelliJ, open a test class such as `Level1Test` and click the green arrow next to the class name.

Plain `mvn test` runs every level, so tests for levels you haven't reached yet will fail. That's expected.

**Use git.** Work on a branch of your own (`git checkout -b my-attempt`) and commit after each level
(`git commit -am "Level 2 done"`). That lets you compare attempts and redo a level from scratch later.

---

## How to practise

1. **Read the level's spec below, then its test file.** The tests show the edge cases. Write them
   down as a checklist before you write any code.
2. **Start a timer** using the target time in the table.
3. **Code in `Bank.java`.** You may add fields, private helper methods and new classes, and change
   `Account`. Don't change the public method signatures; the tests depend on them.
4. **Run the tests for this level and every earlier one.**
5. **If you're stuck for more than 10 minutes, open that level's hint.** Look at the solution
   only once your tests pass, or when your time is up. It's on the **`claude/minibank-solution` branch**, together
   with `SOLUTION_NOTES.md`. From this folder, compare your code with it: `git diff origin/claude/minibank-solution -- src/main`.
6. **Two or three days later, redo the level from a blank `Bank.java`** (checkout your earlier
   commit). If you can't, you memorised the answer instead of learning the pattern.

Talk through your thinking out loud as you code. In an interview, how you reason counts as much as
whether the tests pass.

---

## Rules for every level

- Every operation takes the **current time as its first argument** (`long timestamp`). Timestamps
  never go down from one call to the next.
- Money is a whole number of **cents** in a `long`. For example, `10_000` is $100.00. There are
  no rounding problems.
- An operation that can't be completed returns `Optional.empty()` or `false`, and **changes nothing**.

---

## Level 1: Find and fix the bugs

`Bank.java` already implements these operations:

| Method | Does | Fails when |
|---|---|---|
| `createAccount(ts, id)` | Opens an account with a balance of 0 | The id is already taken |
| `deposit(ts, id, amount)` | Adds money and returns the new balance | Unknown account, or amount ≤ 0 |
| `withdraw(ts, id, amount)` | Removes money and returns the new balance | Unknown account, amount ≤ 0, or not enough money |
| `transfer(ts, from, to, amount)` | Moves money and returns the **sender's** new balance | Either account unknown, both ids are the same account, amount ≤ 0, or the sender doesn't have enough |
| `getBalance(ts, id)` | Returns the current balance | Unknown account |
| `getStatistics(ts)` | Returns the account count, number of successful transactions, total volume and average amount. A transfer counts as one transaction. | Never |

The code has **three bugs**. `Level1Test` has 13 tests, and 3 of them fail. Fix the bugs without
changing the tests.

<details>
<summary>Hints</summary>

- Read each failing test's message. The expected and actual values tell you which method to look at.
- One bug is at a boundary: what if you withdraw exactly what's in the account?
- One bug is about how Java compares `String`s.
- One bug makes a large number come out negative. Look at the **type** of the variable the
  amounts are added to. Compiling with `javac -Xlint:all` gives you a warning that points at it.

</details>

---

## Level 2: Reporting

**`List<SpenderRank> getTopSpenders(ts, k)`**
- An account's spending is the total of its successful **withdrawals**, the **transfers it sent**,
  and, from Level 3, its **executed scheduled payments**. Deposits and money received don't count.
- Return the top `k` accounts as `SpenderRank(accountId, totalSpentCents)`, highest first.
- Ties: the alphabetically smaller account id comes first.
- Accounts that spent nothing are included, with 0.
- If there are fewer than `k` accounts, return all of them. If `k ≤ 0`, return an empty list.

**`Map<TransactionType, Long> getTotalsByType(ts, accountId)`**
- For each transaction type, the total amount of the successful transactions **this account
  started**. Transfers it received are not included.
- Only include types the account has at least one transaction of.
- For an unknown account, return an empty map.

<details>
<summary>Hints</summary>

- This uses the same pattern as the clinic exercise: one pass over the transactions, skip the
  ones that don't count, then group with `Map.merge`.
- So that accounts with no spending still appear, put every account into the map with 0 first.
- Sorting: `Comparator.comparingLong(...).reversed().thenComparing(...)`, then `.limit(k)` on a stream.
- `EnumMap` is the natural map when the keys are an enum.

</details>

---

## Level 3: Scheduled payments

**`Optional<String> schedulePayment(ts, accountId, amount, delay)`**
- Schedules a payment out of the account, **due at `ts + delay`**.
- Returns an id: `"payment1"`, `"payment2"`, and so on. The count goes up by one for each
  **successful** call, across the whole bank.
- Fails, without using up a number, if the account doesn't exist, the amount is ≤ 0, or the
  delay is ≤ 0.

**When payments run.** At the **start of every operation** (every public method), before it does
anything else, run every pending payment whose due time is **≤ that operation's timestamp**:
- The earliest due time runs first.
- Payments with the same due time run in the order they were scheduled.
- If the account has enough money, the money leaves the account and the status becomes `EXECUTED`.
  The payment counts as a `PAYMENT` transaction at its due time: it appears in statistics, top
  spenders and totals by type.
- If there isn't enough money, the status becomes `FAILED` and nothing changes. A failed payment is
  never retried.

**`boolean cancelPayment(ts, accountId, paymentId)`**: after running due payments, cancel the
payment if it exists, is still `PENDING`, and belongs to `accountId`. Returns whether it cancelled.
A payment due at exactly `ts` has already run, so it can't be cancelled.

**`Optional<PaymentStatus> getPaymentStatus(ts, paymentId)`**: the status after running due
payments, or empty for an unknown id.

<details>
<summary>Hints</summary>

- Use a `PriorityQueue` ordered by due time, then by a sequence number. Don't use the id
  string for the tie-break: as strings, `"payment10"` sorts before `"payment2"`.
- Write one private `runDuePayments(timestamp)` and call it on the first line of **every** public
  method, the Level 1 and 2 methods included.
- Removing an item from the middle of a `PriorityQueue` is slow. Instead, mark the payment
  `CANCELLED` and skip it when it comes out of the queue.

</details>

---

## Level 4: Merging accounts and balance history

**`boolean mergeAccounts(ts, targetId, sourceId)`**
- Fails if both ids are the same, or either account doesn't exist.
- Otherwise:
  - The source's balance is added to the target's.
  - The source is **closed**: from now on every operation treats `sourceId` as an unknown account,
    and `accountCount` in the statistics goes down by one.
  - The source's **pending** payments now belong to the target. They keep their ids and due times,
    are paid from the target's balance, and can only be cancelled using `targetId`.
  - The target **takes over the source's transaction history** in `getTopSpenders` and
    `getTotalsByType`, as if it had made those transactions itself.
- A merge isn't a transaction: the transaction count and volume don't change.
- You can assume the id of a closed account is never used again.

**`Optional<Long> getBalanceAt(ts, accountId, atTimestamp)`**
- After running due payments, returns the account's balance **at the end of `atTimestamp`**,
  meaning after everything that happened at or before that time. `atTimestamp ≤ ts`.
- An executed scheduled payment changes the balance **at its due time**, even if it actually ran
  during a later call.
- Returns empty if the account didn't exist at `atTimestamp`: it was created later, it was merged
  away at or before that time, or it never existed.

<details>
<summary>Hints</summary>

- For each account, keep a `TreeMap<Long, Long>` from timestamp to the balance after the last change
  at that timestamp. `floorEntry(atTimestamp)` gives you the answer.
- Remember when each merged account was closed.
- For the reports, the simplest approach is to rewrite the source's transactions to use the
  target's id (`List.replaceAll`). Think about what that costs, and what you'd do with millions of transactions.

</details>

---

## Level 5: Thread safety

There are no new methods. Make every method safe to call from **many threads at the same time**:
- Money is never created or lost, and no balance goes negative.
- Every successful operation is recorded exactly once.
- If several threads open the same account id at once, exactly one succeeds.
- Reports can run while money is moving, without exceptions.
- No deadlocks.

In these tests every call uses the same timestamp. Race conditions are random, so code that isn't
thread-safe usually fails these tests, but not every time. Run them a few times.

<details>
<summary>Hints</summary>

- Start with the simplest thing that's correct: make every public method `synchronized`. Work out why
  that's enough, and what it costs.
- Reading methods need the lock too. If you loop over an `ArrayList` while another thread adds to
  it, you get a `ConcurrentModificationException` or wrong results.
- **Stretch goal:** give each account its own lock, so transfers between different accounts can run
  in parallel. Two threads doing A→B and B→A at the same moment can deadlock. The standard fix is
  to always take the two locks in a fixed order, for example by account id. The transaction log
  and the payment queue are shared, so they still need their own protection.

</details>

---

## Level 6: Idempotent transfers

Over a real network, a client that gets no response can't tell whether the request failed or only
the response was lost, so it sends the request again. Without protection, the customer gets
charged twice. Payment APIs commonly solve this by having the client send a unique **request id**
with each request.

**`Optional<Long> transferOnce(ts, requestId, fromId, toId, amount)`**
- The first time a `requestId` is seen, this behaves exactly like `transfer`, and the result is
  remembered.
- If the same `requestId` arrives again with the same `fromId`, `toId` and `amount`, return the
  **remembered result** and do nothing else. That applies even if the first attempt failed and would
  now succeed: the client must use a new request id to try again.
- If the same `requestId` arrives with **different** details, throw `IllegalArgumentException`.
- It must be thread-safe: if the same request arrives on several threads at once, the money moves
  only once.

<details>
<summary>Hints</summary>

- Use a map from request id to `(fromId, toId, amount, result)`. A small `record` works well.
- Looking up the request id and storing the result must happen as **one atomic step** (check-then-act).
  If they don't, two threads can both see "not seen yet" and both move the money.

</details>

---

## Progress

- [ ] Level 1: bugs fixed
- [ ] Level 2: reporting
- [ ] Level 3: scheduled payments
- [ ] Level 4: merging and history
- [ ] Level 5: thread safety (plus the stretch goal: per-account locks)
- [ ] Level 6: idempotent transfers
- [ ] Redid Levels 1–4 from a blank file, each within its target time

---

## After Level 6: keep growing

The tests stop here, but the project can keep growing in the directions real backend work goes:

1. **Add a REST API with Spring Boot.** For example, `POST /accounts`,
   `POST /transfers` with an `Idempotency-Key` header, and `GET /accounts/{id}/balance?at=...`.
   Write the tests with MockMvc.
2. **Store the data in a database.** Use Spring Data JPA with H2, then Postgres. Replace
   `synchronized` with database transactions, and try optimistic locking with `@Version`.
   This is where Level 5 becomes real.
3. **Make the reports fast.** `getTopSpenders` currently scans every transaction. Keep running
   totals instead and measure the difference with a million transactions.
4. **Add a feature with tests written first.** Ideas: daily withdrawal limits, overdrafts,
   interest, multiple currencies.
5. **Explain your design out loud** as if to an interviewer: what would you change for 10 million
   accounts, or several servers?
