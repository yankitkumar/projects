import itertools
import math
import random
import unittest

from hmm import HMM, WEATHER, log


def random_hmm(rng, n_states=3, n_symbols=3):
    def dist(keys):
        raw = [rng.random() + 0.05 for _ in keys]
        return {k: v / sum(raw) for k, v in zip(keys, raw)}

    states = ["s%d" % i for i in range(n_states)]
    symbols = ["o%d" % i for i in range(n_symbols)]
    return HMM(dist(states), {s: dist(states) for s in states}, {s: dist(symbols) for s in states}), symbols


def joint_log_prob(model, path, obs):
    total = log(model.start[path[0]]) + log(model.emit[path[0]][obs[0]])
    for prev, state, o in zip(path, path[1:], obs[1:]):
        total += log(model.trans[prev][state]) + log(model.emit[state][o])
    return total


class HMMTests(unittest.TestCase):
    def test_classic_weather_example(self):
        path, logp = WEATHER.viterbi(["walk", "shop", "clean"])
        self.assertEqual(path, ["Sunny", "Rainy", "Rainy"])
        self.assertAlmostEqual(math.exp(logp), 0.01344)

    def test_viterbi_and_forward_match_brute_force(self):
        rng = random.Random(0)
        for _ in range(25):
            model, symbols = random_hmm(rng)
            obs = [rng.choice(symbols) for _ in range(rng.randint(1, 6))]
            scores = {path: joint_log_prob(model, path, obs)
                      for path in itertools.product(model.states, repeat=len(obs))}
            best_path = max(scores, key=scores.get)

            path, logp = model.viterbi(obs)
            self.assertEqual(tuple(path), best_path)
            self.assertAlmostEqual(logp, scores[best_path])
            expected_total = math.log(sum(math.exp(v) for v in scores.values()))
            self.assertAlmostEqual(model.forward_log(obs), expected_total)

    def test_total_probability_is_at_least_the_best_path(self):
        obs = ["walk", "walk", "shop", "clean", "clean"]
        self.assertGreaterEqual(WEATHER.forward_log(obs), WEATHER.viterbi(obs)[1])

    def test_probabilities_over_all_sequences_sum_to_one(self):
        total = sum(math.exp(WEATHER.forward_log(list(obs)))
                    for obs in itertools.product(["walk", "shop", "clean"], repeat=3))
        self.assertAlmostEqual(total, 1.0)

    def test_long_sequences_do_not_underflow(self):
        obs = ["walk", "shop", "clean"] * 700
        path, logp = WEATHER.viterbi(obs)
        self.assertEqual(len(path), 2100)
        self.assertTrue(math.isfinite(logp) and math.isfinite(WEATHER.forward_log(obs)))
        self.assertEqual(math.exp(logp), 0.0)  # the plain product would have underflowed to zero

    def test_single_observation(self):
        path, logp = WEATHER.viterbi(["walk"])
        self.assertEqual(path, ["Sunny"])  # 0.4 * 0.6 beats 0.6 * 0.1
        self.assertAlmostEqual(math.exp(logp), 0.24)

    def test_perfectly_informative_emissions_reveal_the_states(self):
        model = HMM(start={"A": 0.5, "B": 0.5},
                    trans={"A": {"A": 0.5, "B": 0.5}, "B": {"A": 0.5, "B": 0.5}},
                    emit={"A": {"a": 1.0, "b": 0.0}, "B": {"a": 0.0, "b": 1.0}})
        self.assertEqual(model.viterbi(list("abba"))[0], ["A", "B", "B", "A"])

    def test_impossible_observation_has_zero_probability(self):
        model = HMM(start={"A": 1.0}, trans={"A": {"A": 1.0}}, emit={"A": {"x": 1.0, "y": 0.0}})
        self.assertEqual(model.forward_log(["x", "y"]), float("-inf"))

    def test_invalid_input_raises(self):
        with self.assertRaises(ValueError):
            WEATHER.viterbi([])
        with self.assertRaises(ValueError):
            WEATHER.forward_log([])
        with self.assertRaises(KeyError):
            WEATHER.viterbi(["dance"])
        with self.assertRaises(ValueError):
            HMM({"A": 0.5}, {"A": {"A": 1.0}}, {"A": {"x": 1.0}})   # start doesn't sum to 1


if __name__ == "__main__":
    unittest.main()
