import unittest
from pathlib import Path

from markov import MarkovChain

CORPUS = Path(__file__).with_name("corpus.txt").read_text()


class MarkovTests(unittest.TestCase):
    def test_every_generated_ngram_exists_in_training_text(self):
        for order in (1, 2, 3):
            chain = MarkovChain(order).train(CORPUS)
            words = chain.generate(80, seed=order).split()
            for i in range(len(words) - order):
                state = tuple(words[i:i + order])
                self.assertIn(words[i + order], chain.transitions[state])

    def test_same_seed_is_deterministic(self):
        chain = MarkovChain(2).train(CORPUS)
        self.assertEqual(chain.generate(50, seed=1), chain.generate(50, seed=1))

    def test_different_seeds_differ(self):
        chain = MarkovChain(1).train(CORPUS)
        self.assertNotEqual(chain.generate(50, seed=1), chain.generate(50, seed=2))

    def test_length_is_respected(self):
        chain = MarkovChain(2).train(CORPUS)
        self.assertEqual(len(chain.generate(30, seed=0).split()), 30)

    def test_dead_end_stops_early(self):
        chain = MarkovChain(1).train("a b c")
        self.assertEqual(chain.generate(10, start=("a",)), "a b c")

    def test_start_words_are_used(self):
        chain = MarkovChain(2).train(CORPUS)
        self.assertTrue(chain.generate(10, start=("A", "neural")).startswith("A neural"))

    def test_unknown_start_raises(self):
        chain = MarkovChain(2).train(CORPUS)
        with self.assertRaises(KeyError):
            chain.generate(10, start=("not", "present"))

    def test_too_short_text_or_bad_order_raises(self):
        with self.assertRaises(ValueError):
            MarkovChain(3).train("only three words")
        with self.assertRaises(ValueError):
            MarkovChain(0)


if __name__ == "__main__":
    unittest.main()
