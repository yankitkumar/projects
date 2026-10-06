# 15 · TF-IDF Search Engine

Ranks documents against a query using **TF-IDF** weighting and **cosine similarity**. This is the classic way to build a search engine with no machine learning model at all.

```bash
python search.py                      # runs some sample queries
python search.py "merge a git branch"
```

```
query: 'how do I bake bread'
  0.387  Baking sourdough bread

query: 'robot exploring Mars'
  0.445  The Mars rover

query: 'learn guitar chords'
  0.418  Learning guitar
```

The collection is 16 short hand-written documents (`docs.py`) on cooking, space, programming, sport, gardening, music and neural networks.

## How it works

- **TF** (term frequency) says how often a word appears in a document. It is dampened with `1 + log(count)`, so a word repeated 10 times isn't worth 10 times as much.
- **IDF** (inverse document frequency) says how rare a word is across the collection. Rare words like "quicksort" are far more informative than common ones, so they get more weight.
- Each document and the query become sparse vectors of `tf × idf` weights, scaled to length 1.
- The score is the **cosine similarity** between query and document, which is just the dot product of those unit vectors. It runs from 0 (nothing in common) to 1 (identical).
- Common words and question words ("the", "how", "do") are dropped first. Documents that share no words with the query are not returned at all.

## Limitations

There is no stemming, so "learn" does not match "learning" and "exploring" does not match "explore". Real engines add a stemmer or switch to embeddings, which also match synonyms.

## Tests

```bash
python -m unittest
```
