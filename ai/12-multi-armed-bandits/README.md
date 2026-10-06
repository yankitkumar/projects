# 12 · Multi-Armed Bandits

You face several slot machines with unknown payout rates. Every pull is a choice between **exploiting** the arm that looks best and **exploring** one that might be better. This project compares three strategies, plus a random baseline.

```bash
python bandits.py
```

```
arm win rates: [0.1, 0.25, 0.3, 0.5, 0.45] (best is arm 3)

strategy            mean regret     best-arm pulls
random                    180.5                20%
epsilon-greedy             35.5                70%
ucb1                       71.3                49%
thompson                   32.1                66%
```

Averages over 50 runs of 1,000 pulls. **Regret** is the reward lost by not always pulling the best arm. Lower is better.

## The strategies

- **ε-greedy** pulls a random arm 10% of the time and the best-looking arm otherwise. It is simple, but it keeps exploring at a fixed rate forever.
- **UCB1** pulls the arm with the highest optimistic estimate, `mean + sqrt(2 ln t / n)`. Rarely-tried arms get a bonus that shrinks as evidence accumulates. It is conservative and only settles on the best arm after a long time, which is why it trails here at 1,000 pulls.
- **Thompson sampling** keeps a Beta distribution over each arm's win rate, samples one plausible rate per arm, and pulls the arm with the best sample. Exploration fades on its own as the posteriors narrow.

Results depend on the arm rates and the horizon. Re-run with your own `probs`.

## Tests

```bash
python -m unittest
```

Tests check the UCB1 warm-up, that regret never decreases, that every learning strategy beats random, that UCB1 and Thompson settle on the best arm on an easy problem, and that runs are reproducible from a seed.
