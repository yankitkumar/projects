"""A* pathfinding on an ASCII grid, compared against breadth-first search."""

import heapq
import sys
from collections import deque

MAZE = """\
S . . . # . . . . .
. # # . # . # # # .
. # . . . . . . # .
. # . # # # # . # .
. . . # . . . . # .
# # . # . # # # # .
. . . . . . . . . G
"""


def parse(text):
    """Parse a maze: '#' wall, 'S' start, 'G' goal, anything else is open floor."""
    grid = [line.split() for line in text.strip().splitlines()]
    start = goal = None
    for r, row in enumerate(grid):
        for c, cell in enumerate(row):
            if cell == "S":
                start = (r, c)
            elif cell == "G":
                goal = (r, c)
    if start is None or goal is None:
        raise ValueError("maze needs both an S and a G")
    return grid, start, goal


def neighbours(grid, pos):
    r, c = pos
    for dr, dc in ((-1, 0), (1, 0), (0, -1), (0, 1)):
        nr, nc = r + dr, c + dc
        if 0 <= nr < len(grid) and 0 <= nc < len(grid[nr]) and grid[nr][nc] != "#":
            yield nr, nc


def manhattan(a, b):
    return abs(a[0] - b[0]) + abs(a[1] - b[1])


def astar(grid, start, goal):
    """Return (path, nodes_expanded). `path` is None when the goal is unreachable.

    Manhattan distance never overestimates on a 4-connected grid with unit costs,
    so the first path A* finds is a shortest one.
    """
    # Queue entries are (f, -g, pos): among equal f, prefer the node deeper along its path
    # (closer to the goal). Without that tie-break A* degrades to BFS on open grids.
    frontier = [(manhattan(start, goal), 0, start)]
    came_from = {start: None}
    cost = {start: 0}
    expanded = 0
    while frontier:
        _, neg_g, pos = heapq.heappop(frontier)
        g = -neg_g
        if g > cost[pos]:  # stale queue entry
            continue
        expanded += 1
        if pos == goal:
            return reconstruct(came_from, goal), expanded
        for nxt in neighbours(grid, pos):
            new_g = g + 1
            if new_g < cost.get(nxt, float("inf")):
                cost[nxt] = new_g
                came_from[nxt] = pos
                heapq.heappush(frontier, (new_g + manhattan(nxt, goal), -new_g, nxt))
    return None, expanded


def bfs(grid, start, goal):
    """Reference shortest-path search with no heuristic. Returns (path, nodes_expanded)."""
    queue = deque([start])
    came_from = {start: None}
    expanded = 0
    while queue:
        pos = queue.popleft()
        expanded += 1
        if pos == goal:
            return reconstruct(came_from, goal), expanded
        for nxt in neighbours(grid, pos):
            if nxt not in came_from:
                came_from[nxt] = pos
                queue.append(nxt)
    return None, expanded


def reconstruct(came_from, goal):
    path, pos = [], goal
    while pos is not None:
        path.append(pos)
        pos = came_from[pos]
    return path[::-1]


def render(grid, path):
    on_path = set(path or [])
    return "\n".join(
        " ".join("*" if (r, c) in on_path and cell == "." else cell for c, cell in enumerate(row))
        for r, row in enumerate(grid)
    )


if __name__ == "__main__":
    text = open(sys.argv[1]).read() if len(sys.argv) > 1 else MAZE
    grid, start, goal = parse(text)
    path, expanded = astar(grid, start, goal)
    if path is None:
        print("No path from S to G.")
    else:
        print(render(grid, path))
        print("\npath length: %d steps" % (len(path) - 1))
    _, bfs_expanded = bfs(grid, start, goal)
    print("nodes expanded: A* = %d, BFS = %d" % (expanded, bfs_expanded))
