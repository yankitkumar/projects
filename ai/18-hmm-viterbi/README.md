# 18 · Hidden Markov Model and Viterbi

A model where the state of the world is hidden and you only see its side effects. This project implements the two standard algorithms for working with one.

```bash
python hmm.py
python hmm.py walk walk shop clean clean
```

The demo is the classic weather example. You can't see the weather, only what a friend does each day (walk, shop or clean):

```
observed : walk shop clean
weather  : Sunny Rainy Rainy
P(path and observations) = 0.01344
P(observations)          = 0.03361  (summed over every possible weather sequence)
```

## The two algorithms

- **Viterbi** finds the single most likely sequence of hidden states. For each state at each step it keeps the best score of any path ending there, plus a back-pointer, then follows the pointers back from the best final state.
- **Forward** computes the probability of the observations by summing over all possible state sequences, so it answers how well the model explains the data.

The forward total (0.0336) is larger than the best single path (0.0134), as it must be: the best path is just one term in the sum.

Both work in **log space**. Multiplying thousands of probabilities underflows to zero, whereas adding their logs does not. A test runs 2,100 observations and checks that the log-probabilities stay finite while the plain probability is 0.0.

## About the model

An HMM is three tables: `start` (where it begins), `trans` (how states follow each other) and `emit` (what each state produces). The constructor checks that every state has a `trans` row and an `emit` row, and that every row sums to 1. Entries left out of a row count as probability 0, so sparse tables need no explicit zeros. An observation that no state can emit raises `KeyError`. The same code handles other problems, such as part-of-speech tagging, speech recognition or decoding noisy sensors. Only the tables change.

## Tests

```bash
python -m unittest
```

On many random models, the tests check Viterbi and Forward against brute-force enumeration of every state sequence. They also check the textbook answer (0.01344) and that probabilities over all possible observation sequences sum to 1.
