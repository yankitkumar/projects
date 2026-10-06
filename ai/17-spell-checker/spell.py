"""Spelling corrector: edit distance plus word frequency (in the style of Peter Norvig's essay).

Idea: a typo is a known word that picked up a few small mistakes. So generate every string one
edit away, keep the ones that are real words, and pick the most common. If none, try two edits.
"""

import re
import sys
from collections import Counter
from pathlib import Path

LETTERS = "abcdefghijklmnopqrstuvwxyz"


def words(text):
    return re.findall(r"[a-z']+", text.lower())


def edit_distance(a, b):
    """Damerau-Levenshtein distance (optimal string alignment): insert, delete, substitute or
    swap two adjacent letters, each costing 1."""
    d = [[0] * (len(b) + 1) for _ in range(len(a) + 1)]
    for i in range(len(a) + 1):
        d[i][0] = i
    for j in range(len(b) + 1):
        d[0][j] = j
    for i in range(1, len(a) + 1):
        for j in range(1, len(b) + 1):
            cost = a[i - 1] != b[j - 1]
            d[i][j] = min(d[i - 1][j] + 1, d[i][j - 1] + 1, d[i - 1][j - 1] + cost)
            if i > 1 and j > 1 and a[i - 1] == b[j - 2] and a[i - 2] == b[j - 1]:
                d[i][j] = min(d[i][j], d[i - 2][j - 2] + 1)
    return d[len(a)][len(b)]


def edits1(word):
    """Every string one edit away from `word`."""
    splits = [(word[:i], word[i:]) for i in range(len(word) + 1)]
    deletes = [l + r[1:] for l, r in splits if r]
    swaps = [l + r[1] + r[0] + r[2:] for l, r in splits if len(r) > 1]
    replaces = [l + c + r[1:] for l, r in splits if r for c in LETTERS]
    inserts = [l + c + r for l, r in splits for c in LETTERS]
    return set(deletes + swaps + replaces + inserts)


def edits2(word):
    return {e2 for e1 in edits1(word) for e2 in edits1(e1)}


class SpellChecker:
    def __init__(self, text):
        self.freq = Counter(words(text))

    def known(self, candidates):
        return {w for w in candidates if w in self.freq}

    def candidates(self, word):
        """Prefer the word itself, then known words one edit away, then two, else give up."""
        return (self.known([word]) or self.known(edits1(word))
                or self.known(edits2(word)) or {word})

    def correct(self, word):
        lower = word.lower()
        best = min(self.candidates(lower), key=lambda w: (-self.freq[w], w))  # most common, then A-Z
        if word.isupper() and len(word) > 1:
            return best.upper()
        return best.capitalize() if word[:1].isupper() else best

    def correct_text(self, text):
        """Correct every word in `text`, leaving punctuation and spacing alone."""
        return re.sub(r"[A-Za-z']+", lambda m: self.correct(m.group()), text)


def default_checker():
    return SpellChecker(Path(__file__).with_name("corpus.txt").read_text())


if __name__ == "__main__":
    checker = default_checker()
    sentences = [" ".join(sys.argv[1:])] if len(sys.argv) > 1 else [
        "The wether was beutiful and we definately wanted to recieve the guests.",
        "Becuase of the rian we stayed in teh car.",
    ]
    for sentence in sentences:
        print(sentence)
        print("  ->", checker.correct_text(sentence))
