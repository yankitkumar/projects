"""Sudoku solver: constraint propagation plus backtracking search (in the style of Peter Norvig's essay).

Each cell holds the set of digits it could still be. Placing a digit removes it from the cell's
peers, and two rules fire automatically:
  * if a cell is down to one candidate, remove that digit from its peers;
  * if a digit fits only one cell in a row/column/box, put it there.
When propagation stalls, the search guesses in the cell with the fewest candidates.
"""

import sys
import time
from itertools import chain, islice

DIGITS = "123456789"

ROW_UNITS = [[9 * r + c for c in range(9)] for r in range(9)]
COL_UNITS = [[9 * r + c for r in range(9)] for c in range(9)]
BOX_UNITS = [[9 * (3 * br + r) + 3 * bc + c for r in range(3) for c in range(3)]
             for br in range(3) for bc in range(3)]
UNITS = ROW_UNITS + COL_UNITS + BOX_UNITS
UNITS_OF = {s: [u for u in UNITS if s in u] for s in range(81)}
PEERS = {s: set(chain.from_iterable(UNITS_OF[s])) - {s} for s in range(81)}


def parse(puzzle):
    """Turn an 81-character puzzle ('.' or '0' = empty; other characters ignored) into candidate sets.

    Returns None if the givens already contradict each other.
    """
    chars = [c for c in puzzle if c in "0123456789."]
    if len(chars) != 81:
        raise ValueError("expected 81 cells, got %d" % len(chars))
    values = {s: set(DIGITS) for s in range(81)}
    for s, c in enumerate(chars):
        if c in DIGITS and not assign(values, s, c):
            return None
    return values


def assign(values, s, d):
    """Fix cell s to digit d by eliminating every other candidate. False means a contradiction."""
    return all(eliminate(values, s, other) for other in values[s] - {d})


def eliminate(values, s, d):
    if d not in values[s]:
        return True
    values[s].discard(d)
    if not values[s]:
        return False
    if len(values[s]) == 1:
        only = next(iter(values[s]))
        if not all(eliminate(values, p, only) for p in PEERS[s]):
            return False
    for unit in UNITS_OF[s]:
        places = [p for p in unit if d in values[p]]
        if not places:
            return False
        if len(places) == 1 and not assign(values, places[0], d):
            return False
    return True


def search(values, stats=None):
    """Yield every solution (as a fully-resolved candidate dict) reachable from `values`."""
    if values is None:
        return
    unsolved = [s for s in values if len(values[s]) > 1]
    if not unsolved:
        yield values
        return
    s = min(unsolved, key=lambda cell: len(values[cell]))
    for d in sorted(values[s]):
        if stats is not None:
            stats["guesses"] += 1
        trial = {cell: set(options) for cell, options in values.items()}
        if assign(trial, s, d):
            yield from search(trial, stats)


def to_string(values):
    return "".join(next(iter(values[s])) for s in range(81))


def solve(puzzle, stats=None):
    """Return the solution as an 81-character string, or None if there isn't one."""
    solution = next(search(parse(puzzle), stats), None)
    return to_string(solution) if solution else None


def count_solutions(puzzle, limit=2):
    """Count solutions, stopping at `limit`. A well-formed puzzle has exactly one."""
    return len(list(islice(search(parse(puzzle)), limit)))


def is_valid_solution(grid):
    return len(grid) == 81 and all(sorted(grid[s] for s in unit) == list(DIGITS) for unit in UNITS)


def pretty(grid):
    rows = []
    for r in range(9):
        cells = [grid[9 * r + c].replace("0", ".") for c in range(9)]
        rows.append(" | ".join(" ".join(cells[i:i + 3]) for i in (0, 3, 6)))
        if r in (2, 5):
            rows.append("------+-------+------")
    return "\n".join(rows)


EASY = "003020600900305001001806400008102900700000008006708200002609500800203009005010300"
HARD = "8..........36......7..9.2...5...7.......457.....1...3...1....68..85...1..9....4.."

if __name__ == "__main__":
    puzzles = sys.argv[1:] or [EASY, HARD]
    for puzzle in puzzles:
        stats = {"guesses": 0}
        start = time.perf_counter()
        solution = solve(puzzle, stats)
        elapsed = time.perf_counter() - start
        if solution is None:
            print("No solution.\n")
            continue
        print(pretty(solution))
        print("\nsolved in %.3fs with %d guesses\n" % (elapsed, stats["guesses"]))
