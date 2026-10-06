import random
import unittest

from astar import MAZE, astar, bfs, manhattan, neighbours, parse


def random_maze(rng, rows=12, cols=12, wall_prob=0.3):
    cells = [["#" if rng.random() < wall_prob else "." for _ in range(cols)] for _ in range(rows)]
    cells[0][0], cells[rows - 1][cols - 1] = "S", "G"
    return "\n".join(" ".join(row) for row in cells)


class AStarTests(unittest.TestCase):
    def test_solves_sample_maze(self):
        grid, start, goal = parse(MAZE)
        path, _ = astar(grid, start, goal)
        self.assertEqual(path[0], start)
        self.assertEqual(path[-1], goal)

    def test_path_is_valid(self):
        grid, start, goal = parse(MAZE)
        path, _ = astar(grid, start, goal)
        for a, b in zip(path, path[1:]):
            self.assertEqual(manhattan(a, b), 1)
            self.assertIn(b, list(neighbours(grid, a)))

    def test_matches_bfs_length_on_random_mazes(self):
        rng = random.Random(42)
        solved = 0
        for _ in range(200):
            grid, start, goal = parse(random_maze(rng))
            a_path, _ = astar(grid, start, goal)
            b_path, _ = bfs(grid, start, goal)
            self.assertEqual(a_path is None, b_path is None)
            if a_path:
                solved += 1
                self.assertEqual(len(a_path), len(b_path))
        self.assertGreater(solved, 20)  # make sure the property test actually exercised paths

    def test_unreachable_goal_returns_none(self):
        grid, start, goal = parse("S # G")
        self.assertIsNone(astar(grid, start, goal)[0])

    def test_start_next_to_goal(self):
        grid, start, goal = parse("S G")
        self.assertEqual(astar(grid, start, goal)[0], [(0, 0), (0, 1)])

    def test_expands_fewer_nodes_than_bfs_in_open_grid(self):
        text = "\n".join(" ".join("S" if (r, c) == (0, 0) else "G" if (r, c) == (14, 14) else "."
                                  for c in range(15)) for r in range(15))
        grid, start, goal = parse(text)
        _, a_expanded = astar(grid, start, goal)
        _, b_expanded = bfs(grid, start, goal)
        self.assertLess(a_expanded, b_expanded)

    def test_missing_start_or_goal_raises(self):
        with self.assertRaises(ValueError):
            parse(". . .")


if __name__ == "__main__":
    unittest.main()
