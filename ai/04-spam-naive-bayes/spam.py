"""Multinomial Naive Bayes spam classifier, written from scratch."""

import math
import re
import sys
from collections import Counter

from data import HAM, SPAM


def tokenize(text):
    # Keep apostrophes only inside words ("i'll"), so quote marks around 'free' are dropped.
    text = text.lower().replace("’", "'")  # typographic apostrophe -> ASCII
    return re.findall(r"[a-z0-9$]+(?:'[a-z0-9$]+)*", text)


class NaiveBayes:
    def __init__(self, alpha=1.0):
        self.alpha = alpha  # Laplace smoothing: pretend every word was seen `alpha` extra times
        self.word_counts = {}
        self.class_counts = Counter()
        self.vocab = set()

    def fit(self, texts, labels):
        for text, label in zip(texts, labels):
            self.class_counts[label] += 1
            counts = self.word_counts.setdefault(label, Counter())
            for tok in tokenize(text):
                counts[tok] += 1
                self.vocab.add(tok)
        return self

    def log_scores(self, text):
        total_docs = sum(self.class_counts.values())
        scores = {}
        for label, counts in self.word_counts.items():
            denom = sum(counts.values()) + self.alpha * len(self.vocab)
            score = math.log(self.class_counts[label] / total_docs)
            for tok in tokenize(text):
                if tok in self.vocab:  # words never seen in training carry no information
                    score += math.log((counts[tok] + self.alpha) / denom)
            scores[label] = score
        return scores

    def predict_proba(self, text):
        scores = self.log_scores(text)
        top = max(scores.values())
        exp = {label: math.exp(s - top) for label, s in scores.items()}  # log-sum-exp for stability
        z = sum(exp.values())
        return {label: v / z for label, v in exp.items()}

    def predict(self, text):
        scores = self.log_scores(text)
        return max(scores, key=scores.get)

    def top_words(self, label, n=8):
        """Words most indicative of `label` (highest log-odds vs. the other classes)."""
        others = [c for lab, c in self.word_counts.items() if lab != label]
        mine = self.word_counts[label]
        my_total = sum(mine.values()) + self.alpha * len(self.vocab)
        other_total = sum(sum(c.values()) for c in others) + self.alpha * len(self.vocab)

        def odds(w):
            return math.log((mine[w] + self.alpha) / my_total) - math.log(
                (sum(c[w] for c in others) + self.alpha) / other_total)

        # Break ties alphabetically; set order changes with the hash seed from run to run.
        return sorted(self.vocab, key=lambda w: (-odds(w), w))[:n]


def split(data, every=4):
    """Hold out every `every`-th message as a test set (deterministic)."""
    train = [x for i, x in enumerate(data) if i % every]
    test = [x for i, x in enumerate(data) if i % every == 0]
    return train, test


def build_model():
    spam_train, spam_test = split(SPAM)
    ham_train, ham_test = split(HAM)
    texts = spam_train + ham_train
    labels = ["spam"] * len(spam_train) + ["ham"] * len(ham_train)
    test = [(t, "spam") for t in spam_test] + [(t, "ham") for t in ham_test]
    return NaiveBayes().fit(texts, labels), test


if __name__ == "__main__":
    model, test = build_model()
    correct = sum(model.predict(t) == label for t, label in test)
    print("held-out accuracy: %d/%d = %.0f%%" % (correct, len(test), 100 * correct / len(test)))
    print("most spammy words:", ", ".join(model.top_words("spam")))
    print("most hammy words :", ", ".join(model.top_words("ham")))
    for text in sys.argv[1:]:
        p = model.predict_proba(text)
        print("\n%r\n  -> %s (spam probability %.1f%%)" % (text, model.predict(text), 100 * p["spam"]))
