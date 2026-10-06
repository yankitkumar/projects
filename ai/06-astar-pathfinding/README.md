# 06 · A* Pathfinding

Finds the shortest route through an ASCII maze with **A\***, and compares it with plain breadth-first search (BFS).

```bash
python astar.py              # built-in maze
python astar.py my_maze.txt  # your own: '#' wall, 'S' start, 'G' goal, '.' floor, space-separated
```

```
S . . . # . . . . .
* # # . # . # # # .
* # . . . . . . # .
* # . # # # # . # .
* * * # . . . . # .
# # * # . # # # # .
. . * * * * * * * G

path length: 15 steps
nodes expanded: A* = 27, BFS = 42
```

## How it works

- A* always expands the cell with the lowest `f = g + h`, where `g` is the cost so far and `h` is the Manhattan-distance guess to the goal.
- Manhattan distance never overestimates on a 4-connected grid, so the first path A* finds is a shortest one.
- Among cells with equal `f`, it prefers the one with the larger `g`. Without that tie-break A* degrades to BFS on open grids. On an empty 15×15 grid this version expands 29 cells where BFS expands 225.

## Tests

```bash
python -m unittest
```

A property test generates 200 random mazes and checks A* and BFS always agree on whether a path exists and on its length. Other tests check path validity, unreachable goals, and that A* expands fewer cells than BFS.
