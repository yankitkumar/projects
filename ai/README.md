# AI Projects

Eight small, self-contained AI projects. Each one is a single readable file you can run, with unit tests alongside it.
Seven use only the Python standard library and `numpy`, with the algorithms written from scratch.
The eighth is a terminal chatbot built on the Claude API.

| # | Project | Idea | Needs |
|---|---------|------|-------|
| 01 | [Tic-Tac-Toe minimax](01-tictactoe-minimax) | Game-playing AI that never loses (minimax + alpha-beta pruning) | stdlib |
| 02 | [Linear regression](02-linear-regression) | Gradient descent vs. the closed-form solution | numpy |
| 03 | [K-means clustering](03-kmeans-clustering) | k-means++ initialisation, with an ASCII scatter plot | numpy |
| 04 | [Spam filter](04-spam-naive-bayes) | Naive Bayes text classifier | stdlib |
| 05 | [Neural network](05-neural-net-xor) | A multi-layer perceptron with backprop that learns XOR | numpy |
| 06 | [A* pathfinding](06-astar-pathfinding) | Shortest path through a maze, compared with BFS | stdlib |
| 07 | [Markov text generator](07-markov-text) | Order-N Markov chain that babbles in the style of a corpus | stdlib |
| 08 | [Claude chatbot](08-claude-chatbot) | Streaming terminal chat with multi-turn memory | `anthropic` + API key |

## Setup

```bash
pip install -r requirements.txt   # numpy for 02, 03 and 05; anthropic for 08
```

Python 3.10 or newer. These projects were developed and tested on Python 3.13.

## Run a project

Each project has its own README with the exact command. For example:

```bash
cd 01-tictactoe-minimax && python tictactoe.py
```

## Run all the tests

```bash
./run_tests.sh
```

Or run one project's tests with `cd <project> && python -m unittest`.
The tests use `unittest`, so there is nothing extra to install.
The chatbot tests run the real Anthropic SDK against a mocked HTTP transport, so they need no API key or network.

## Notes

The datasets and corpus are tiny and hand-written, so the demos are for learning, not benchmarking.
