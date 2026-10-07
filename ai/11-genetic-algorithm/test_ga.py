import unittest

from ga import CAPACITY, ITEMS, evolve, knapsack_optimum, solve_knapsack, solve_string


class GATests(unittest.TestCase):
    def test_solves_the_string(self):
        best, history = solve_string("genetic algorithms evolve", seed=1)
        self.assertEqual(best, "genetic algorithms evolve")
        self.assertEqual(history[-1], len("genetic algorithms evolve"))

    def test_solves_targets_outside_lowercase_letters(self):
        for target in ("Hello, World!", "café", "A"):
            with self.subTest(target=target):
                best, _ = solve_string(target, seed=0)
                self.assertEqual(best, target)

    def test_elitism_makes_fitness_monotonic(self):
        # Heavy mutation and no early stop, so without elitism the best genome does get lost.
        def drops(elite, seed):
            _, history = evolve(
                fitness=sum,
                random_genome=lambda rng: [rng.randint(0, 1) for _ in range(20)],
                crossover=lambda a, b, rng: a[:10] + b[10:],
                mutate=lambda g, rng: [1 - x if rng.random() < 0.1 else x for x in g],
                pop_size=20, generations=30, elite=elite, seed=seed,
            )
            return any(b < a for a, b in zip(history, history[1:]))

        self.assertTrue(any(drops(0, seed) for seed in range(10)))
        self.assertFalse(any(drops(2, seed) for seed in range(10)))

    def test_stops_early_at_target(self):
        _, history = solve_string("abc", seed=0, generations=500)
        self.assertLess(len(history), 100)
        self.assertEqual(history[-1], 3)

    def test_same_seed_reproduces_the_run(self):
        a = solve_string("repeatable", seed=7, generations=30)
        b = solve_string("repeatable", seed=7, generations=30)
        self.assertEqual(a, b)

    def test_knapsack_result_is_feasible_and_near_optimal(self):
        best, history, value = solve_knapsack(seed=0)
        weight = sum(w for (w, _), bit in zip(ITEMS, best) if bit)
        self.assertLessEqual(weight, CAPACITY)
        optimum = knapsack_optimum(ITEMS, CAPACITY)
        self.assertLessEqual(value, optimum)
        self.assertGreaterEqual(value, 0.95 * optimum)

    def test_one_gene_genomes_can_be_crossed_over(self):
        best, history = solve_string("a", seed=0, pop_size=10)
        self.assertEqual(best, "a")
        best, history, value = solve_knapsack(items=[(3, 5)], capacity=10)
        self.assertEqual((best, value), ([1], 5))

    def test_knapsack_optimum_on_a_known_case(self):
        # Items are (weight, value). At capacity 5 the first two (value 7) tie with the third alone.
        self.assertEqual(knapsack_optimum([(2, 3), (3, 4), (5, 7)], 5), 7)
        self.assertEqual(knapsack_optimum([(2, 3), (3, 4)], 1), 0)

    def test_generic_evolve_maximises_ones_in_a_bitstring(self):
        best, history = evolve(
            fitness=sum,
            random_genome=lambda rng: [rng.randint(0, 1) for _ in range(20)],
            crossover=lambda a, b, rng: a[:10] + b[10:],
            mutate=lambda g, rng: [1 - x if rng.random() < 0.05 else x for x in g],
            pop_size=50, generations=100, target=20, seed=3,
        )
        self.assertEqual(sum(best), 20)


if __name__ == "__main__":
    unittest.main()
