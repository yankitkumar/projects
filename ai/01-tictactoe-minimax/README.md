# 01 · Tic-Tac-Toe with Minimax

A Tic-Tac-Toe opponent that cannot be beaten. It searches the whole game tree with **minimax** and skips branches that cannot change the result using **alpha-beta pruning**.

```bash
python tictactoe.py            # you play X and move first
python tictactoe.py --second   # you play O, the AI opens
```

Squares are numbered 1-9, left to right and top to bottom.

## How it works

- `minimax` scores a position from the AI's point of view. A win is worth `10 - depth`, so the AI prefers quick wins and slow losses. A draw is 0.
- Alpha-beta pruning stops exploring a branch as soon as it can't beat a result already found.
- `best_move` picks the highest-scoring move, with ties going to the lowest square, so the AI is deterministic.

## Tests

```bash
python -m unittest
```

The key test is exhaustive. It tries every possible sequence of human moves against the AI, as both first and second player, and checks the human can never win.
