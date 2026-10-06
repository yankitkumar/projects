# 09 · k-Nearest Neighbours

Classify a point by a vote among its `k` closest training points. There is no training step, because the data is the model. The demo uses cross-validation to choose `k`.

```bash
python knn.py        # needs numpy
```

```
5-fold cross-validated accuracy by k:
  k=1  0.770
  k=3  0.800
  k=5  0.822
  k=7  0.844
  k=9  0.859 <- best
  k=11 0.859
  ...
held-out test accuracy with k=9: 0.800
```

The data is three overlapping Gaussian blobs, so no `k` reaches 100%.

## How it works

- `k=1` memorises the training data and is sensitive to noise. A larger `k` smooths the decision boundary.
- `weighted=True` weights each neighbour's vote by `1 / distance`, so closer neighbours count for more. A test shows a case where this flips the answer.
- On a tie, the class of the single closest neighbour wins.
- `standardize` scales features using training-set statistics only. k-NN depends entirely on distances, so a noisy feature measured in thousands drowns out a useful one. A test shows accuracy below 80% without scaling and above 90% with it.
- `best_k` scores each candidate `k` with k-fold cross-validation, so the held-out test set is never used to choose `k`.

## Tests

```bash
python -m unittest
```
