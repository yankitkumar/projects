import unittest

from tictactoe import best_move, moves, other, winner


def human_can_win(board, human, ai, turn):
    """Search every human move sequence against the AI; True if the human can ever win."""
    w = winner(board)
    if w == human:
        return True
    if w == ai or not moves(board):
        return False
    if turn == human:
        for m in moves(board):
            board[m] = human
            found = human_can_win(board, human, ai, other(turn))
            board[m] = " "
            if found:
                return True
        return False
    m = best_move(board, ai)
    board[m] = ai
    found = human_can_win(board, human, ai, other(turn))
    board[m] = " "
    return found


class TicTacToeTests(unittest.TestCase):
    def test_takes_winning_move(self):
        self.assertEqual(best_move(list("XX OO    "), "X"), 2)

    def test_blocks_opponent_win(self):
        self.assertEqual(best_move(list("OO  X    "), "X"), 2)

    def test_prefers_win_over_block(self):
        self.assertEqual(best_move(list("XX OO    "), "O"), 5)

    def test_winner_detects_all_lines(self):
        self.assertEqual(winner(list("XXXOO    ")), "X")
        self.assertEqual(winner(list("O  O  O  ")), "O")
        self.assertEqual(winner(list("X   X   X")), "X")
        self.assertIsNone(winner([" "] * 9))

    def test_ai_never_loses_playing_second(self):
        self.assertFalse(human_can_win([" "] * 9, human="X", ai="O", turn="X"))

    def test_ai_never_loses_playing_first(self):
        self.assertFalse(human_can_win([" "] * 9, human="O", ai="X", turn="X"))

    def test_best_move_does_not_mutate_board(self):
        board = list("X O      ")
        before = list(board)
        best_move(board, "X")
        self.assertEqual(board, before)


if __name__ == "__main__":
    unittest.main()
