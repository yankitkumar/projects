import unittest

from bpe import SAMPLE, BPETokenizer, merge


class BPETests(unittest.TestCase):
    @classmethod
    def setUpClass(cls):
        cls.tok = BPETokenizer().train(SAMPLE, vocab_size=300)

    def test_merge_helper(self):
        self.assertEqual(merge([1, 2, 3, 1, 2], (1, 2), 9), [9, 3, 9])
        self.assertEqual(merge([1, 1, 1], (1, 1), 9), [9, 1])  # non-overlapping, left to right
        self.assertEqual(merge([], (1, 2), 9), [])

    def test_wikipedia_example(self):
        # "aaabdaaabac": aa -> 256, ab -> 257, (256, 257) -> 258.
        tok = BPETokenizer().train("aaabdaaabac", vocab_size=259)
        self.assertEqual(list(tok.merges.items()), [((97, 97), 256), ((97, 98), 257), ((256, 257), 258)])
        self.assertEqual(tok.encode("aaabdaaabac"), [258, 100, 258, 97, 99])

    def test_round_trip_for_any_text(self):
        for text in ["", "a", "the cat sat", "line one\nline two\ttabbed", "café naïve", "日本語のテキスト",
                     "emoji 🎉 and 👍🏽", "unseen words: zebra quartz"]:
            self.assertEqual(self.tok.decode(self.tok.encode(text)), text, text)

    def test_training_compresses_repetitive_text(self):
        self.assertLess(len(self.tok.encode(SAMPLE)), len(SAMPLE.encode()) / 2)

    def test_vocab_size_is_an_upper_bound_and_exact_when_text_allows(self):
        self.assertLessEqual(len(self.tok.vocab), 300)
        self.assertEqual(len(self.tok.vocab), 256 + len(self.tok.merges))
        # The premise: SAMPLE has repeated pairs for more than 300 tokens, so 300 is reached exactly.
        self.assertGreater(len(BPETokenizer().train(SAMPLE, vocab_size=1000).vocab), 300)
        self.assertEqual(len(self.tok.vocab), 300)
        for size in (257, 280):
            self.assertEqual(len(BPETokenizer().train(SAMPLE, vocab_size=size).vocab), size)
        big = BPETokenizer().train("abab" * 200, vocab_size=300)
        self.assertLess(len(big.vocab), 300)  # runs out of repeated pairs well before 300

    def test_no_repeats_means_no_merges(self):
        tok = BPETokenizer().train("abcdefg", vocab_size=300)
        self.assertEqual(tok.merges, {})
        self.assertEqual(tok.encode("abc"), [97, 98, 99])

    def test_training_is_deterministic(self):
        again = BPETokenizer().train(SAMPLE, vocab_size=300)
        self.assertEqual(again.merges, self.tok.merges)

    def test_a_frequent_word_becomes_one_token(self):
        self.assertEqual(len(self.tok.encode("the")), 1)
        self.assertLess(len(self.tok.encode("the cat")), len("the cat"))

    def test_training_again_starts_from_scratch(self):
        tok = BPETokenizer().train("hello hello hello", vocab_size=260).train("xyz xyz xyz", vocab_size=260)
        fresh = BPETokenizer().train("xyz xyz xyz", vocab_size=260)
        self.assertEqual(tok.merges, fresh.merges)
        self.assertEqual(tok.vocab, fresh.vocab)
        for text in ["help", "yellow", "the cat"]:
            self.assertEqual(tok.decode(tok.encode(text)), text, text)

    def test_vocab_below_byte_alphabet_raises(self):
        with self.assertRaises(ValueError):
            BPETokenizer().train("text", vocab_size=100)

    def test_unknown_token_id_raises(self):
        with self.assertRaises(KeyError):
            self.tok.decode([99999])


if __name__ == "__main__":
    unittest.main()
