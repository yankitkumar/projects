# 11 · Genetic Algorithm

Optimisation by simulated evolution. A population of candidate solutions is repeatedly selected, recombined and mutated, and over generations the fittest candidates take over.

```bash
python ga.py
```

```
string:   'genetic algorithms evolve' after 40 generations (fitness 25/25)
knapsack: value 74, weight 40/40  (exact optimum: 74)
```

## How it works

`evolve` is generic. You supply `fitness`, `random_genome`, `crossover` and `mutate`. Each generation it:

1. keeps the `elite` best genomes unchanged, so the best fitness can never go down;
2. picks two parents by **tournament selection**, taking the fittest of a few random candidates;
3. crosses them over at a random cut point, then mutates the child.

It stops early once the best fitness reaches `target`.

Two demos use it:

- **String matching.** It evolves random characters into a target phrase, with fitness equal to the number of matching characters. Genes are drawn from lowercase letters and space, plus any other characters the target uses, so capitals, digits and punctuation can be matched too.
- **0/1 knapsack.** It picks the most valuable items that fit in a weight limit. Overweight packs get a negative score that grows with the excess, so the search is pushed toward feasible packs instead of flat-lining at zero. A dynamic-programming solver gives the exact optimum to compare against.

A genetic algorithm gives no guarantee of the optimum. Here the seeded run happens to hit it, and the test only requires within 5%.

## Tests

```bash
python -m unittest
```
