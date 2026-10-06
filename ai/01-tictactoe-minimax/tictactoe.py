"""Unbeatable Tic-Tac-Toe using minimax with alpha-beta pruning."""

import sys

LINES = [
    (0, 1, 2), (3, 4, 5), (6, 7, 8),  # rows
    (0, 3, 6), (1, 4, 7), (2, 5, 8),  # columns
    (0, 4, 8), (2, 4, 6),             # diagonals
]


def winner(board):
    """Return 'X' or 'O' if someone has three in a row, else None."""
    for a, b, c in LINES:
        if board[a] != " " and board[a] == board[b] == board[c]:
            return board[a]
    return None


def moves(board):
    return [i for i, cell in enumerate(board) if cell == " "]


def other(player):
    return "O" if player == "X" else "X"


def minimax(board, to_move, me, depth=0, alpha=-10, beta=10):
    """Score `board` from `me`'s point of view, with `to_move` about to play.

    Wins score 10 - depth, so the AI prefers quicker wins and slower losses.
    """
    w = winner(board)
    if w == me:
        return 10 - depth
    if w is not None:
        return depth - 10
    free = moves(board)
    if not free:
        return 0

    maximizing = to_move == me
    best = -10 if maximizing else 10
    for m in free:
        board[m] = to_move
        score = minimax(board, other(to_move), me, depth + 1, alpha, beta)
        board[m] = " "
        if maximizing:
            best = max(best, score)
            alpha = max(alpha, best)
        else:
            best = min(best, score)
            beta = min(beta, best)
        if beta <= alpha:
            break
    return best


def best_move(board, player):
    """Return the index of the best move for `player` (ties go to the lowest index)."""
    board = list(board)
    best_score, choice = None, None
    for m in moves(board):
        board[m] = player
        score = minimax(board, other(player), player, depth=1)
        board[m] = " "
        if best_score is None or score > best_score:
            best_score, choice = score, m
    return choice


def render(board):
    rows = []
    for r in range(3):
        cells = [board[3 * r + c] if board[3 * r + c] != " " else str(3 * r + c + 1) for c in range(3)]
        rows.append(" " + " | ".join(cells))
    return "\n---+---+---\n".join(rows)


def play(human="X"):
    board = [" "] * 9
    ai = other(human)
    turn = "X"
    print("You are %s. Enter a square number (1-9)." % human)
    while True:
        print("\n" + render(board) + "\n")
        w = winner(board)
        if w or not moves(board):
            print("%s wins!" % w if w else "It's a draw.")
            return w
        if turn == human:
            try:
                choice = int(input("Your move: ")) - 1
            except ValueError:
                print("Please type a number from 1 to 9.")
                continue
            except EOFError:
                print()
                return None
            if choice not in moves(board):
                print("That square isn't available.")
                continue
        else:
            choice = best_move(board, ai)
            print("AI plays %d" % (choice + 1))
        board[choice] = turn
        turn = other(turn)


if __name__ == "__main__":
    play("O" if "--second" in sys.argv else "X")
