import unittest

from spell import SpellChecker, default_checker, edit_distance, edits1, edits2


class EditDistanceTests(unittest.TestCase):
    def test_known_distances(self):
        self.assertEqual(edit_distance("kitten", "sitting"), 3)
        self.assertEqual(edit_distance("", "abc"), 3)
        self.assertEqual(edit_distance("abc", ""), 3)
        self.assertEqual(edit_distance("same", "same"), 0)
        self.assertEqual(edit_distance("teh", "the"), 1)       # one adjacent swap
        self.assertEqual(edit_distance("flaw", "lawn"), 2)

    def test_symmetric(self):
        for a, b in [("receive", "recieve"), ("abc", "xyz"), ("", "q"), ("ab", "ba")]:
            self.assertEqual(edit_distance(a, b), edit_distance(b, a))

    def test_edits1_are_exactly_distance_one_or_less(self):
        for word in ["cat", "ab", "a", "weather"]:
            for e in edits1(word):
                self.assertLessEqual(edit_distance(word, e), 1, (word, e))

    def test_edits2_reach_two_mistakes(self):
        self.assertIn("beautiful", edits2("beutifl"))
        self.assertNotIn("beautiful", edits1("beutifl"))


class SpellCheckerTests(unittest.TestCase):
    @classmethod
    def setUpClass(cls):
        cls.checker = default_checker()

    def test_known_words_are_left_alone(self):
        for word in ["weather", "the", "beautiful", "family"]:
            self.assertEqual(self.checker.correct(word), word)

    def test_fixes_each_kind_of_typo(self):
        cases = {
            "becuase": "because",        # swapped letters
            "beutiful": "beautiful",     # missing letter
            "thhe": "the",               # extra letter
            "definately": "definitely",  # wrong letter
            "recieve": "receive",        # swapped letters
            "seperate": "separate",      # wrong letter
            "beleive": "believe",        # swapped letters
        }
        for typo, expected in cases.items():
            self.assertEqual(self.checker.correct(typo), expected, typo)

    def test_two_mistakes_are_still_corrected(self):
        self.assertEqual(self.checker.correct("beutifl"), "beautiful")

    def test_words_with_no_candidate_are_returned_unchanged(self):
        self.assertEqual(self.checker.correct("zzzzzzzz"), "zzzzzzzz")

    def test_capitalisation_is_preserved(self):
        self.assertEqual(self.checker.correct("Teh"), "The")
        self.assertEqual(self.checker.correct("TEH"), "THE")
        self.assertEqual(self.checker.correct("teh"), "the")

    def test_correct_text_keeps_punctuation_and_spacing(self):
        self.assertEqual(self.checker.correct_text("Teh wether, was beutiful!"),
                         "The weather, was beautiful!")

    def test_more_frequent_word_wins_among_equals(self):
        checker = SpellChecker("cat cat cat cut")
        self.assertEqual(checker.correct("cot"), "cat")   # "cat" and "cut" are both one edit away

    def test_ties_are_broken_alphabetically(self):
        self.assertEqual(SpellChecker("cut cat").correct("cot"), "cat")

    def test_fewer_edits_beat_higher_frequency(self):
        checker = SpellChecker("abxy abxy abxy abxy abcd")
        self.assertEqual(checker.correct("abcx"), "abcd")  # one edit to abcd, two to the commoner abxy


if __name__ == "__main__":
    unittest.main()
