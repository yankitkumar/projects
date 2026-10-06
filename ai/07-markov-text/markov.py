"""Order-N Markov chain text generator."""

import argparse
import random
from collections import defaultdict
from pathlib import Path


class MarkovChain:
    def __init__(self, order=2):
        if order < 1:
            raise ValueError("order must be at least 1")
        self.order = order
        self.transitions = defaultdict(list)  # tuple of `order` words -> words seen next

    def train(self, text):
        words = text.split()
        if len(words) <= self.order:
            raise ValueError("need more than %d words to train an order-%d chain" % (self.order, self.order))
        for i in range(len(words) - self.order):
            self.transitions[tuple(words[i:i + self.order])].append(words[i + self.order])
        return self

    def generate(self, length=40, seed=None, start=None):
        """Generate up to `length` words. Stops early only if the chain hits a dead end."""
        rng = random.Random(seed)
        state = tuple(start) if start else rng.choice(sorted(self.transitions))
        if state not in self.transitions:
            raise KeyError("starting words %r never appear together in the corpus" % (state,))
        out = list(state)
        while len(out) < length:
            choices = self.transitions.get(tuple(out[-self.order:]))
            if not choices:
                break
            out.append(rng.choice(choices))
        return " ".join(out)


if __name__ == "__main__":
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--corpus", default=str(Path(__file__).with_name("corpus.txt")))
    parser.add_argument("--order", type=int, default=2)
    parser.add_argument("--length", type=int, default=50)
    parser.add_argument("--seed", type=int, default=None)
    args = parser.parse_args()

    chain = MarkovChain(args.order).train(Path(args.corpus).read_text())
    print(chain.generate(args.length, seed=args.seed))
