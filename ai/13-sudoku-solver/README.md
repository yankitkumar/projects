# 13 · Sudoku Solver

A solver that reasons before it guesses. It combines **constraint propagation** with **backtracking search**, in the style of Peter Norvig's well-known essay.

```bash
python sudoku.py                  # solves a built-in easy and a hard puzzle
python sudoku.py "<81 characters, . or 0 for empty>"
```

```
8 1 2 | 7 5 3 | 6 4 9
9 4 3 | 6 8 2 | 1 7 5
...
solved in 0.019s with 172 guesses
```

The easy puzzle is solved by propagation alone, with 0 guesses. The hard one is a puzzle widely described as one of the hardest and needs 172 guesses.

## How it works

Every cell holds the set of digits it could still be. Two rules fire automatically whenever a digit is removed from a cell:

1. If a cell is left with a single candidate, remove that digit from all of its peers (same row, column and box).
2. If a digit fits only one cell in a row, column or box, put it there.

When that stalls, the search picks the unsolved cell with the **fewest** candidates and tries each one on a copy of the board. Choosing the most constrained cell keeps the search tree small.

`count_solutions` runs the same search to find every solution, up to a limit. That lets the tests check that a puzzle has exactly one solution, and that an empty grid has many.

## Tests

```bash
python -m unittest
```

Tests check that solutions are valid and keep the givens, that both puzzles are unique, and that contradictory or subtly unsolvable puzzles return `None` instead of looping or crashing.
