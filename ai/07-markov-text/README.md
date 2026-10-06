# 07 · Markov Chain Text Generator

Learns which words follow which in a text, then babbles new text by repeatedly picking a plausible next word. It is the idea behind language models in its simplest form.

```bash
python markov.py                              # order 2, 50 words, random seed
python markov.py --seed 3 --order 1 --length 30
python markov.py --corpus mytext.txt          # train on your own text
```

Sample output (`--seed 3 --order 1 --length 30`):

```
good, and in ordinary messages. A spam filter uses those counts how often each cluster moves towards the opponent replies well. The next word is chosen from the algorithm which
```

## How it works

- An **order-N** chain remembers the last N words and the list of words that followed them in the training text.
- Picking from that list at random weights each next word by how often it followed. Repeats in the list are the probabilities.
- A higher order stays closer to the source text and reads more coherently. A lower order is wilder.
- If the chain reaches a state that never had a successor, it stops early instead of looping.

`corpus.txt` is a short original text about the algorithms in this repo.

## Tests

```bash
python -m unittest
```

Tests check that every generated n-gram occurs in the training text, that a seed makes output deterministic, and that dead ends and bad input are handled.
