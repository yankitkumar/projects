"""A tiny search engine: TF-IDF vectors ranked by cosine similarity."""

import math
import re
import sys
import unicodedata
from collections import Counter

from docs import DOCS

STOPWORDS = set(
    "a an and are as at be but by for from has have in into is it its of on or that the their "
    "then there this to was when where which while with you your not no so than too can will "
    "how do does did i me my we us what who whom why if about".split()
)


def tokenize(text):
    # Unicode letters and digits (NFC joins decomposed accents first), so "café" stays one word.
    text = unicodedata.normalize("NFC", text.lower())
    return [w for w in re.findall(r"[^\W_]+", text) if w not in STOPWORDS]


class SearchEngine:
    def __init__(self, docs):
        self.docs = list(docs)
        tokenized = [tokenize(title + " " + text) for title, text in self.docs]
        n = len(tokenized)
        doc_freq = Counter(word for tokens in tokenized for word in set(tokens))
        # Smoothed inverse document frequency: rare words are worth more than common ones.
        self.idf = {w: math.log((1 + n) / (1 + df)) + 1 for w, df in doc_freq.items()}
        self.vectors = [self._vectorize(tokens) for tokens in tokenized]

    def _vectorize(self, tokens):
        """Sparse, length-normalised TF-IDF vector as {word: weight}."""
        counts = Counter(t for t in tokens if t in self.idf)
        vec = {w: (1 + math.log(c)) * self.idf[w] for w, c in counts.items()}
        norm = math.sqrt(sum(v * v for v in vec.values()))
        return {w: v / norm for w, v in vec.items()} if norm else {}

    def search(self, query, k=3):
        """Return up to k (score, title, text) results, best first. Scores are cosine similarities."""
        q = self._vectorize(tokenize(query))
        scored = []
        for (title, text), vec in zip(self.docs, self.vectors):
            score = sum(weight * vec.get(word, 0.0) for word, weight in q.items())
            if score > 0:
                scored.append((score, title, text))
        scored.sort(key=lambda item: -item[0])
        return scored[:k]


if __name__ == "__main__":
    engine = SearchEngine(DOCS)
    queries = [" ".join(sys.argv[1:])] if len(sys.argv) > 1 else [
        "how do I bake bread", "robot exploring Mars", "learn guitar chords", "grow tomatoes"]
    for query in queries:
        print("query: %r" % query)
        results = engine.search(query)
        for score, title, _ in results:
            print("  %.3f  %s" % (score, title))
        if not results:
            print("  (no matches)")
        print()
