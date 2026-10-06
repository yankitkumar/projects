"""Hidden Markov model: the forward algorithm and Viterbi decoding, in log space.

The states are hidden; you only see what each state emits. Two questions come up constantly:
  * forward: how likely is this observation sequence under the model?
  * Viterbi: what is the single most likely sequence of hidden states behind it?
"""

import math
import sys


def log(x):
    return math.log(x) if x > 0 else float("-inf")


def logsumexp(values):
    top = max(values)
    if top == float("-inf"):
        return top
    return top + math.log(sum(math.exp(v - top) for v in values))


class HMM:
    """start[s] = P(first state), trans[s][t] = P(next state t | s), emit[s][o] = P(observe o | s)."""

    def __init__(self, start, trans, emit):
        self.states = list(start)
        self.start, self.trans, self.emit = start, trans, emit
        for name, row in [("start", start)] + [("trans[%s]" % s, trans[s]) for s in self.states] \
                + [("emit[%s]" % s, emit[s]) for s in self.states]:
            if abs(sum(row.values()) - 1.0) > 1e-9:
                raise ValueError("%s must sum to 1" % name)

    def forward_log(self, obs):
        """log P(obs), summing over every possible state sequence."""
        if not obs:
            raise ValueError("need at least one observation")
        alpha = {s: log(self.start[s]) + log(self.emit[s][obs[0]]) for s in self.states}
        for o in obs[1:]:
            alpha = {t: logsumexp([alpha[s] + log(self.trans[s][t]) for s in self.states])
                        + log(self.emit[t][o]) for t in self.states}
        return logsumexp(list(alpha.values()))

    def viterbi(self, obs):
        """Return (most likely state sequence, its log probability)."""
        if not obs:
            raise ValueError("need at least one observation")
        best = {s: log(self.start[s]) + log(self.emit[s][obs[0]]) for s in self.states}
        back = []
        for o in obs[1:]:
            nxt, pointer = {}, {}
            for t in self.states:
                prev = max(self.states, key=lambda s: best[s] + log(self.trans[s][t]))
                nxt[t] = best[prev] + log(self.trans[prev][t]) + log(self.emit[t][o])
                pointer[t] = prev
            best = nxt
            back.append(pointer)
        last = max(self.states, key=lambda s: best[s])
        path = [last]
        for pointer in reversed(back):
            path.append(pointer[path[-1]])
        return path[::-1], best[last]


# The classic weather example: you can't see the weather, only what your friend does each day.
WEATHER = HMM(
    start={"Rainy": 0.6, "Sunny": 0.4},
    trans={"Rainy": {"Rainy": 0.7, "Sunny": 0.3}, "Sunny": {"Rainy": 0.4, "Sunny": 0.6}},
    emit={"Rainy": {"walk": 0.1, "shop": 0.4, "clean": 0.5},
          "Sunny": {"walk": 0.6, "shop": 0.3, "clean": 0.1}},
)

if __name__ == "__main__":
    obs = sys.argv[1:] or ["walk", "shop", "clean"]
    path, logp = WEATHER.viterbi(obs)
    print("observed :", " ".join(obs))
    print("weather  :", " ".join(path))
    print("P(path and observations) = %.5f" % math.exp(logp))
    print("P(observations)          = %.5f  (summed over every possible weather sequence)"
          % math.exp(WEATHER.forward_log(obs)))
