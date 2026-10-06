# AI Projects

Eighteen small, self-contained AI projects. Each one is a single readable file you can run, with unit tests alongside it.
Seventeen use only the Python standard library and `numpy`, with the algorithms written from scratch.
The other, 08, is a terminal chatbot built on the Claude API.

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
| 09 | [k-Nearest neighbours](09-knn-classifier) | Classify by majority vote of the closest points, with cross-validated `k` | numpy |
| 10 | [Decision tree](10-decision-tree) | CART classifier using Gini impurity, with a readable text export | numpy |
| 11 | [Genetic algorithm](11-genetic-algorithm) | Evolve a string and solve a knapsack by selection, crossover and mutation | stdlib |
| 12 | [Multi-armed bandits](12-multi-armed-bandits) | ε-greedy vs. UCB1 vs. Thompson sampling, compared by regret | stdlib |
| 13 | [Sudoku solver](13-sudoku-solver) | Constraint propagation plus backtracking search | stdlib |
| 14 | [PCA](14-pca) | Dimensionality reduction via the covariance eigendecomposition | numpy |
| 15 | [TF-IDF search](15-tfidf-search) | Rank documents by cosine similarity of TF-IDF vectors | stdlib |
| 16 | [BPE tokenizer](16-bpe-tokenizer) | Byte-pair encoding, the tokenizer algorithm behind most LLMs | stdlib |
| 17 | [Spell checker](17-spell-checker) | Edit distance plus word frequency | stdlib |
| 18 | [HMM and Viterbi](18-hmm-viterbi) | Decode hidden states from observations, in log space | stdlib |

## Setup

```bash
pip install -r requirements.txt   # numpy for 02, 03, 05, 09, 10 and 14; anthropic for 08
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
