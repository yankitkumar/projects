import unittest

from spam import NaiveBayes, build_model, tokenize


class SpamTests(unittest.TestCase):
    @classmethod
    def setUpClass(cls):
        cls.model, cls.test = build_model()

    def test_tokenize_lowercases_and_strips_punctuation(self):
        self.assertEqual(tokenize("FREE prize!! Click-now"), ["free", "prize", "click", "now"])

    def test_obvious_spam_and_ham(self):
        self.assertEqual(self.model.predict("Claim your free prize now, click the link"), "spam")
        self.assertEqual(self.model.predict("Are we still meeting for lunch tomorrow?"), "ham")

    def test_probabilities_sum_to_one(self):
        p = self.model.predict_proba("free lunch tomorrow")
        self.assertAlmostEqual(sum(p.values()), 1.0)
        self.assertTrue(all(0.0 <= v <= 1.0 for v in p.values()))

    def test_held_out_accuracy(self):
        correct = sum(self.model.predict(t) == label for t, label in self.test)
        self.assertGreaterEqual(correct / len(self.test), 0.8)

    def test_unseen_words_and_empty_text_do_not_crash(self):
        self.assertIn(self.model.predict("zzyzx qwertyuiop"), {"spam", "ham"})
        self.assertAlmostEqual(sum(self.model.predict_proba("").values()), 1.0)

    def test_smoothing_keeps_unseen_class_word_pairs_finite(self):
        nb = NaiveBayes().fit(["buy now", "hello friend"], ["spam", "ham"])
        scores = nb.log_scores("buy friend")
        self.assertTrue(all(s > float("-inf") for s in scores.values()))

    def test_top_words_are_indicative(self):
        self.assertIn("free", self.model.top_words("spam", n=5))


if __name__ == "__main__":
    unittest.main()
