import unittest

from docs import DOCS
from search import SearchEngine, tokenize


class SearchTests(unittest.TestCase):
    @classmethod
    def setUpClass(cls):
        cls.engine = SearchEngine(DOCS)

    def top(self, query):
        results = self.engine.search(query, k=1)
        return results[0][1] if results else None

    def test_finds_the_relevant_document(self):
        self.assertEqual(self.top("how do I bake bread"), "Baking sourdough bread")
        self.assertEqual(self.top("robot exploring Mars"), "The Mars rover")
        self.assertEqual(self.top("merge a git branch"), "Git branches")
        self.assertEqual(self.top("guitar chords"), "Learning guitar")
        self.assertEqual(self.top("train for a marathon"), "Marathon training")

    def test_scores_are_sorted_and_bounded(self):
        results = self.engine.search("soup with potatoes and pasta", k=10)
        scores = [s for s, _, _ in results]
        self.assertEqual(scores, sorted(scores, reverse=True))
        self.assertTrue(all(0 < s <= 1 + 1e-9 for s in scores))

    def test_documents_with_no_shared_words_are_excluded(self):
        titles = [t for _, t, _ in self.engine.search("quicksort", k=20)]
        self.assertEqual(titles, ["Sorting algorithms"])

    def test_empty_stopword_only_and_unknown_queries_return_nothing(self):
        for query in ("", "the and of", "zzzzqqq"):
            self.assertEqual(self.engine.search(query), [])

    def test_a_document_is_its_own_best_match_with_cosine_one(self):
        title, text = DOCS[4]
        score, found, _ = self.engine.search(title + " " + text, k=1)[0]
        self.assertEqual(found, title)
        self.assertAlmostEqual(score, 1.0)

    def test_rare_words_weigh_more_than_common_ones(self):
        doc_freq = lambda word: sum(word in tokenize(t + " " + x) for t, x in DOCS)
        self.assertEqual(doc_freq("quicksort"), 1)
        self.assertGreater(doc_freq("water"), 1)   # the premise: one rare word, one common word
        self.assertGreater(self.engine.idf["quicksort"], self.engine.idf["water"])

    def test_question_words_are_ignored(self):
        self.assertEqual(tokenize("how do I bake bread"), ["bake", "bread"])

    def test_k_limits_results(self):
        self.assertLessEqual(len(self.engine.search("the soup pasta bread", k=2)), 2)

    def test_tokenize_lowercases_and_drops_stopwords(self):
        self.assertEqual(tokenize("The Quick, Brown fox!"), ["quick", "brown", "fox"])


if __name__ == "__main__":
    unittest.main()
