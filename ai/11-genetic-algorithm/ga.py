"""A small genetic algorithm, demonstrated on string matching and the 0/1 knapsack problem."""

import random
import string


def evolve(fitness, random_genome, crossover, mutate, pop_size=100, generations=200,
           elite=2, tournament=3, target=None, seed=None):
    """Evolve a population and return (best genome, best fitness per generation).

    Each generation keeps the `elite` best genomes unchanged, then fills the rest with children:
    two parents chosen by tournament selection are crossed over and the child is mutated.
    Stops early once the best fitness reaches `target`.
    """
    rng = random.Random(seed)
    population = [random_genome(rng) for _ in range(pop_size)]
    history = []
    for generation in range(generations + 1):
        scored = sorted(((fitness(g), g) for g in population), key=lambda p: p[0], reverse=True)
        history.append(scored[0][0])
        if (target is not None and scored[0][0] >= target) or generation == generations:
            return scored[0][1], history

        def pick():
            return max(rng.sample(scored, tournament), key=lambda p: p[0])[1]

        population = [g for _, g in scored[:elite]]
        while len(population) < pop_size:
            population.append(mutate(crossover(pick(), pick(), rng), rng))


# ---- Demo 1: evolve a string ------------------------------------------------------------

ALPHABET = string.ascii_lowercase + " "


def solve_string(target, seed=0, pop_size=200, generations=500):
    def fitness(genome):
        return sum(a == b for a, b in zip(genome, target))

    def random_genome(rng):
        return "".join(rng.choice(ALPHABET) for _ in target)

    def crossover(a, b, rng):
        cut = rng.randrange(1, len(a))
        return a[:cut] + b[cut:]

    def mutate(genome, rng):
        return "".join(rng.choice(ALPHABET) if rng.random() < 1 / len(genome) else c for c in genome)

    return evolve(fitness, random_genome, crossover, mutate, pop_size=pop_size,
                  generations=generations, target=len(target), seed=seed)


# ---- Demo 2: 0/1 knapsack ---------------------------------------------------------------

ITEMS = [(12, 4), (2, 2), (1, 1), (1, 2), (4, 10), (7, 13), (3, 7), (9, 11), (5, 8), (6, 9),
         (8, 12), (2, 3), (10, 14), (4, 5), (3, 6), (11, 15), (1, 3), (6, 7), (5, 9), (7, 8)]
CAPACITY = 40


def knapsack_optimum(items, capacity):
    """Exact answer by dynamic programming, to check the GA against."""
    best = [0] * (capacity + 1)
    for weight, value in items:
        for c in range(capacity, weight - 1, -1):
            best[c] = max(best[c], best[c - weight] + value)
    return best[capacity]


def solve_knapsack(items=ITEMS, capacity=CAPACITY, seed=0, pop_size=100, generations=150):
    def fitness(genome):
        weight = sum(w for (w, _), bit in zip(items, genome) if bit)
        value = sum(v for (_, v), bit in zip(items, genome) if bit)
        # Feasible packs score their value. Overweight packs score negative, so every feasible
        # pack beats every infeasible one, and lighter infeasible packs beat heavier ones.
        return value if weight <= capacity else capacity - weight

    def random_genome(rng):
        return [int(rng.random() < 0.2) for _ in items]

    def crossover(a, b, rng):
        cut = rng.randrange(1, len(a))
        return a[:cut] + b[cut:]

    def mutate(genome, rng):
        return [1 - bit if rng.random() < 1 / len(genome) else bit for bit in genome]

    best, history = evolve(fitness, random_genome, crossover, mutate, pop_size=pop_size,
                           generations=generations, seed=seed)
    return best, history, fitness(best)


if __name__ == "__main__":
    target = "genetic algorithms evolve"
    best, history = solve_string(target)
    print("string:   %r after %d generations (fitness %d/%d)" % (best, len(history) - 1, history[-1], len(target)))

    best, history, value = solve_knapsack()
    weight = sum(w for (w, _), bit in zip(ITEMS, best) if bit)
    print("knapsack: value %d, weight %d/%d  (exact optimum: %d)"
          % (value, weight, CAPACITY, knapsack_optimum(ITEMS, CAPACITY)))
