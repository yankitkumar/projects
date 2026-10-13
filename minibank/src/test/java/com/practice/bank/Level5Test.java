package com.practice.bank;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

/**
 * These tests start many threads at the same moment. A bank that is not thread-safe
 * usually fails them, but not always: race conditions are random. Run them a few times.
 * In this level every call uses the same timestamp, so you do not need to think about
 * timestamps arriving out of order.
 */
@DisplayName("Level 5: thread safety")
class Level5Test {

    private static final int THREADS = 8;
    private static final long NOW = 1_000;

    @Test
    @DisplayName("concurrent deposits into one account are never lost")
    void concurrentDeposits() throws Exception {
        Bank bank = new Bank();
        bank.createAccount(NOW, "pool");
        int perThread = 20_000;

        runConcurrently(() -> {
            for (int i = 0; i < perThread; i++) {
                bank.deposit(NOW, "pool", 1);
            }
        });

        assertEquals(Optional.of((long) THREADS * perThread), bank.getBalance(NOW, "pool"));
        assertEquals(THREADS * perThread, bank.getStatistics(NOW).transactionCount());
    }

    @Test
    @DisplayName("concurrent transfers never create or lose money, and never deadlock")
    void concurrentTransfers() throws Exception {
        Bank bank = new Bank();
        int accounts = 10;
        long startingBalance = 10_000;
        for (int i = 0; i < accounts; i++) {
            bank.createAccount(NOW, "acc" + i);
            bank.deposit(NOW, "acc" + i, startingBalance);
        }
        AtomicInteger successfulTransfers = new AtomicInteger();

        runConcurrently(() -> {
            ThreadLocalRandom random = ThreadLocalRandom.current();
            for (int i = 0; i < 20_000; i++) {
                String from = "acc" + random.nextInt(accounts);
                String to = "acc" + random.nextInt(accounts);
                if (bank.transfer(NOW, from, to, 1 + random.nextInt(500)).isPresent()) {
                    successfulTransfers.incrementAndGet();
                }
            }
        });

        long total = 0;
        for (int i = 0; i < accounts; i++) {
            long balance = bank.getBalance(NOW, "acc" + i).orElseThrow();
            assertTrue(balance >= 0, "acc" + i + " has a negative balance: " + balance);
            total += balance;
        }
        assertEquals(accounts * startingBalance, total, "money was created or lost");
        assertEquals(accounts + successfulTransfers.get(), bank.getStatistics(NOW).transactionCount(),
                "every successful transfer must be recorded exactly once");
    }

    @Test
    @DisplayName("reports can run while money is moving")
    void reportsDuringTraffic() throws Exception {
        Bank bank = new Bank();
        for (int i = 0; i < 5; i++) {
            bank.createAccount(NOW, "acc" + i);
            bank.deposit(NOW, "acc" + i, 1_000_000);
        }
        AtomicInteger workerId = new AtomicInteger();

        runConcurrently(() -> {
            ThreadLocalRandom random = ThreadLocalRandom.current();
            if (workerId.getAndIncrement() % 2 == 0) {
                for (int i = 0; i < 200; i++) {
                    bank.getTopSpenders(NOW, 3);
                    bank.getTotalsByType(NOW, "acc" + random.nextInt(5));
                    bank.getStatistics(NOW);
                }
            } else {
                for (int i = 0; i < 5_000; i++) {
                    bank.withdraw(NOW, "acc" + random.nextInt(5), 1);
                    bank.transfer(NOW, "acc" + random.nextInt(5), "acc" + random.nextInt(5), 1);
                }
            }
        });
    }

    @Test
    @DisplayName("when two threads open the same account at once, exactly one succeeds")
    void concurrentCreateAccount() throws Exception {
        Bank bank = new Bank();
        for (int round = 0; round < 300; round++) {
            String id = "acc" + round;
            AtomicInteger winners = new AtomicInteger();
            runConcurrently(() -> {
                if (bank.createAccount(NOW, id)) {
                    winners.incrementAndGet();
                }
            });
            assertEquals(1, winners.get(), "createAccount(" + id + ") succeeded " + winners.get() + " times");
        }
    }

    /** Runs the task on THREADS threads that all start at the same moment, and waits for them. */
    static void runConcurrently(Runnable task) throws Exception {
        ExecutorService pool = Executors.newFixedThreadPool(THREADS, runnable -> {
            Thread thread = new Thread(runnable);
            thread.setDaemon(true);   // so a deadlocked test cannot stop the JVM from exiting
            return thread;
        });
        try {
            CountDownLatch startSignal = new CountDownLatch(1);
            List<Future<?>> futures = new ArrayList<>();
            for (int i = 0; i < THREADS; i++) {
                futures.add(pool.submit(() -> {
                    startSignal.await();
                    task.run();
                    return null;
                }));
            }
            startSignal.countDown();
            for (Future<?> future : futures) {
                try {
                    future.get(30, TimeUnit.SECONDS);
                } catch (TimeoutException e) {
                    fail("threads did not finish within 30 seconds: probably a deadlock");
                }
            }
        } finally {
            pool.shutdownNow();
        }
    }
}
