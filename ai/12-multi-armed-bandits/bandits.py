"""Multi-armed bandits: epsilon-greedy, UCB1 and Thompson sampling on Bernoulli arms.

Every pull is a choice between exploiting the arm that looks best so far and exploring
an arm that might be better. The strategies below balance that trade-off differently.
"""

import math
import random


class BernoulliBandit:
    """Arm i pays 1 with probability probs[i], otherwise 0."""

    def __init__(self, probs, seed=None):
        self.probs = list(probs)
        self.best = max(self.probs)
        self.rng = random.Random(seed)

    def pull(self, arm):
        return int(self.rng.random() < self.probs[arm])


class Agent:
    def __init__(self, n_arms, seed=None):
        self.n_arms = n_arms
        self.counts = [0] * n_arms
        self.wins = [0] * n_arms
        self.rng = random.Random(seed)

    @property
    def t(self):
        return sum(self.counts)

    def mean(self, arm):
        return self.wins[arm] / self.counts[arm] if self.counts[arm] else 0.0

    def update(self, arm, reward):
        self.counts[arm] += 1
        self.wins[arm] += reward

    def argmax(self, scores):
        top = max(scores)
        return self.rng.choice([i for i, s in enumerate(scores) if s == top])  # random tie-break

    def select(self):
        raise NotImplementedError


class RandomAgent(Agent):
    """Baseline: never learns."""

    def select(self):
        return self.rng.randrange(self.n_arms)


class EpsilonGreedy(Agent):
    """Explore a random arm with probability epsilon, otherwise pull the best-looking arm."""

    def __init__(self, n_arms, epsilon=0.1, seed=None):
        super().__init__(n_arms, seed)
        self.epsilon = epsilon

    def select(self):
        if self.rng.random() < self.epsilon:
            return self.rng.randrange(self.n_arms)
        return self.argmax([self.mean(a) for a in range(self.n_arms)])


class UCB1(Agent):
    """Pull the arm with the highest optimistic estimate: mean + sqrt(2 ln t / n)."""

    def select(self):
        for arm in range(self.n_arms):
            if self.counts[arm] == 0:
                return arm
        t = self.t
        return self.argmax([self.mean(a) + math.sqrt(2 * math.log(t) / self.counts[a])
                            for a in range(self.n_arms)])


class ThompsonSampling(Agent):
    """Sample a plausible win rate for each arm from its Beta posterior and pull the best sample."""

    def select(self):
        samples = [self.rng.betavariate(1 + self.wins[a], 1 + self.counts[a] - self.wins[a])
                   for a in range(self.n_arms)]
        return self.argmax(samples)


def run(agent, bandit, steps):
    """Play `steps` rounds. Returns the cumulative (pseudo-)regret after each round.

    Regret counts what was lost by not pulling the best arm: best_prob - prob of the chosen arm.
    """
    regret, curve = 0.0, []
    for _ in range(steps):
        arm = agent.select()
        agent.update(arm, bandit.pull(arm))
        regret += bandit.best - bandit.probs[arm]
        curve.append(regret)
    return curve


def compare(factories, probs, steps=1000, runs=50):
    """Average final regret and best-arm pull rate for each strategy over many independent runs."""
    best_arms = [i for i, p in enumerate(probs) if p == max(probs)]  # all arms tied for best
    results = {}
    for name, make in factories.items():
        total_regret = best_pulls = 0.0
        for r in range(runs):
            agent = make(len(probs), 1000 + r)
            total_regret += run(agent, BernoulliBandit(probs, seed=r), steps)[-1]
            best_pulls += sum(agent.counts[a] for a in best_arms) / steps
        results[name] = (total_regret / runs, best_pulls / runs)
    return results


FACTORIES = {
    "random": lambda n, seed: RandomAgent(n, seed),
    "epsilon-greedy": lambda n, seed: EpsilonGreedy(n, 0.1, seed),
    "ucb1": lambda n, seed: UCB1(n, seed),
    "thompson": lambda n, seed: ThompsonSampling(n, seed),
}

if __name__ == "__main__":
    probs = [0.10, 0.25, 0.30, 0.50, 0.45]
    print("arm win rates:", probs, "(best is arm 3)\n")
    print("%-15s %15s %18s" % ("strategy", "mean regret", "best-arm pulls"))
    for name, (regret, share) in compare(FACTORIES, probs).items():
        print("%-15s %15.1f %17.0f%%" % (name, regret, 100 * share))
