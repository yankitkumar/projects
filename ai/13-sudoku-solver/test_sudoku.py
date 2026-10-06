import unittest

from sudoku import EASY, HARD, count_solutions, is_valid_solution, parse, solve

EMPTY = "." * 81


def keeps_givens(puzzle, solution):
    return all(p in ".0" or p == s for p, s in zip(puzzle, solution))


class SudokuTests(unittest.TestCase):
    def test_solves_easy_and_hard_puzzles(self):
        for puzzle in (EASY, HARD):
            solution = solve(puzzle)
            self.assertIsNotNone(solution)
            self.assertTrue(is_valid_solution(solution))
            self.assertTrue(keeps_givens(puzzle, solution))

    def test_puzzles_have_unique_solutions(self):
        self.assertEqual(count_solutions(EASY), 1)
        self.assertEqual(count_solutions(HARD), 1)

    def test_empty_grid_has_many_solutions_but_solves(self):
        self.assertEqual(count_solutions(EMPTY, limit=2), 2)
        self.assertTrue(is_valid_solution(solve(EMPTY)))

    def test_solved_grid_returns_itself(self):
        solution = solve(EASY)
        self.assertEqual(solve(solution), solution)

    def test_conflicting_givens_have_no_solution(self):
        puzzle = "55" + "." * 79
        self.assertIsNone(parse(puzzle))
        self.assertIsNone(solve(puzzle))

    def test_locally_consistent_but_unsolvable_puzzle(self):
        # Row 1 forces a 9 into the last cell, but that column already has a 9 below it.
        puzzle = "12345678." + "........9" + "." * 63
        self.assertIsNone(solve(puzzle))

    def test_zero_and_dot_are_both_empty_and_whitespace_is_ignored(self):
        grid = "\n".join(EASY[i:i + 9] for i in range(0, 81, 9))
        self.assertEqual(solve(grid), solve(EASY.replace("0", ".")))

    def test_wrong_length_raises(self):
        with self.assertRaises(ValueError):
            solve("123")

    def test_is_valid_solution_rejects_bad_grids(self):
        good = solve(EASY)
        bad = good[:-2] + good[-1] + good[-2]  # swap the last two cells
        self.assertFalse(is_valid_solution(bad))
        self.assertFalse(is_valid_solution(good[:-1]))


if __name__ == "__main__":
    unittest.main()
