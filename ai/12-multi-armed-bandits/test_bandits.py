import unittest

from bandits import (FACTORIES, BernoulliBandit, EpsilonGreedy, RandomAgent, ThompsonSampling,
                     UCB1, compare, run)

PROBS = [0.1, 0.25, 0.3, 0.5, 0.45]


class BanditTests(unittest.TestCase):
    def test_bandit_pays_out_at_its_probability(self):
        bandit = BernoulliBandit([0.2, 0.9], seed=0)
        for arm, p in enumerate([0.2, 0.9]):
            mean = sum(bandit.pull(arm) for _ in range(5000)) / 5000
            self.assertAlmostEqual(mean, p, delta=0.03)

    def test_update_bookkeeping(self):
        agent = RandomAgent(3)
        agent.update(1, 1)
        agent.update(1, 0)
        agent.update(2, 1)
        self.assertEqual(agent.counts, [0, 2, 1])
        self.assertEqual(agent.mean(1), 0.5)
        self.assertEqual(agent.mean(0), 0.0)
        self.assertEqual(agent.t, 3)

    def test_ucb_tries_every_arm_first(self):
        agent, bandit = UCB1(5, seed=0), BernoulliBandit(PROBS, seed=0)
        run(agent, bandit, 5)
        self.assertEqual(agent.counts, [1] * 5)

    def test_regret_never_decreases(self):
        for make in FACTORIES.values():
            curve = run(make(5, 0), BernoulliBandit(PROBS, seed=0), 300)
            self.assertTrue(all(b >= a for a, b in zip(curve, curve[1:])))
            self.assertGreaterEqual(curve[0], 0)

    def test_learning_strategies_beat_random(self):
        results = compare(FACTORIES, PROBS, steps=1000, runs=20)
        baseline = results["random"][0]
        for name in ("epsilon-greedy", "ucb1", "thompson"):
            self.assertLess(results[name][0], 0.6 * baseline, name)

    def test_ucb_and_thompson_settle_on_the_best_arm(self):
        results = compare(FACTORIES, [0.2, 0.8], steps=1000, runs=20)
        for name in ("ucb1", "thompson"):
            self.assertGreater(results[name][1], 0.8, name)

    def test_every_arm_tied_for_best_counts_as_a_best_arm_pull(self):
        for name, (regret, share) in compare(FACTORIES, [0.5, 0.5], steps=200, runs=5).items():
            self.assertEqual(regret, 0.0, name)
            self.assertAlmostEqual(share, 1.0, msg=name)
        results = compare(FACTORIES, [0.5, 0.5, 0.1], steps=1000, runs=20)
        for name in ("epsilon-greedy", "ucb1", "thompson"):
            self.assertGreater(results[name][1], 0.9, name)

    def test_epsilon_one_explores_uniformly(self):
        agent = EpsilonGreedy(4, epsilon=1.0, seed=0)
        run(agent, BernoulliBandit([0.1, 0.2, 0.3, 0.9], seed=0), 4000)
        for count in agent.counts:
            self.assertAlmostEqual(count / 4000, 0.25, delta=0.05)

    def test_same_seeds_reproduce_a_run(self):
        a = run(ThompsonSampling(5, seed=3), BernoulliBandit(PROBS, seed=4), 100)
        b = run(ThompsonSampling(5, seed=3), BernoulliBandit(PROBS, seed=4), 100)
        self.assertEqual(a, b)


if __name__ == "__main__":
    unittest.main()
