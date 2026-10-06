# 03 · K-Means Clustering

Unsupervised clustering written from scratch. It groups points around `k` centres, with no labels needed.

```bash
python kmeans.py        # needs numpy
```

The demo clusters three blobs and draws them in the terminal. Digits are cluster members and `#` marks a centroid:

```
centroids:
  cluster 0: (5.83, 1.02)  size=60
  cluster 1: (3.00, 5.94)  size=60
  cluster 2: (-0.00, 0.10)  size=60
inertia: 180.7 -> 129.4 in 2 iterations
```

## How it works

- **k-means++ initialisation** picks each starting centroid with probability proportional to its squared distance from the centroids already chosen, which avoids the bad starts plain random initialisation can give.
- Each iteration assigns every point to its nearest centroid, then moves each centroid to the mean of its points. It stops when the centroids stop moving.
- An empty cluster is restarted at the point farthest from its centroid.
- `inertia_` (the sum of squared distances to the nearest centroid) never increases, and a test checks that.

## Tests

```bash
python -m unittest
```
