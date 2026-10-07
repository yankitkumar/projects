"""k-nearest-neighbours classifier with cross-validated choice of k."""

from collections import defaultdict

import numpy as np


class KNN:
    def __init__(self, k=3, weighted=False):
        if k < 1:
            raise ValueError("k must be at least 1")
        self.k = k
        self.weighted = weighted  # weight each neighbour's vote by 1 / distance

    def fit(self, X, y):
        self.X = np.asarray(X, dtype=float)
        self.y = np.asarray(y)
        if self.k > len(self.X):
            raise ValueError("k (%d) is larger than the training set (%d)" % (self.k, len(self.X)))
        return self

    def predict_one(self, x):
        dist = np.linalg.norm(self.X - x, axis=1)
        nearest = np.argsort(dist, kind="stable")[:self.k]
        votes = defaultdict(float)
        for i in nearest:
            votes[self.y[i]] += 1.0 / (dist[i] + 1e-9) if self.weighted else 1.0
        top = max(votes.values())
        tied = [label for label, v in votes.items() if v == top]
        for i in nearest:  # on a tie, the class of the closest neighbour wins
            if self.y[i] in tied:
                return self.y[i]

    def predict(self, X):
        return np.array([self.predict_one(x) for x in np.asarray(X, dtype=float)])

    def score(self, X, y):
        return float(np.mean(self.predict(X) == np.asarray(y)))


def standardize(train, *others):
    """Scale every feature to zero mean and unit variance, using only the training statistics.

    k-NN is all about distances, so a feature measured in thousands drowns out one measured in ones.
    """
    mu, sigma = train.mean(axis=0), train.std(axis=0)
    sigma[sigma == 0] = 1.0
    return [(a - mu) / sigma for a in (train, *others)]


def kfold_indices(n, folds=5, seed=0):
    """Shuffle 0..n-1 and split it into `folds` nearly equal parts."""
    idx = np.random.default_rng(seed).permutation(n)
    return np.array_split(idx, folds)


def cross_val_score(make_model, X, y, folds=5, seed=0):
    X, y = np.asarray(X, dtype=float), np.asarray(y)
    scores = []
    for test_idx in kfold_indices(len(X), folds, seed):
        train_idx = np.setdiff1d(np.arange(len(X)), test_idx)
        model = make_model().fit(X[train_idx], y[train_idx])
        scores.append(model.score(X[test_idx], y[test_idx]))
    return scores


def best_k(X, y, ks=range(1, 16, 2), folds=5, seed=0):
    """Return (best k, {k: mean cross-validated accuracy}). Ties go to the smaller k."""
    results = {k: float(np.mean(cross_val_score(lambda: KNN(k), X, y, folds, seed))) for k in ks}
    top = max(results.values())  # equal means can differ in the last bit
    return min(k for k, acc in results.items() if acc >= top - 1e-12), results


def make_data(n_per=60, seed=0):
    """Three overlapping Gaussian classes in 2-D."""
    rng = np.random.default_rng(seed)
    centers = np.array([[0.0, 0.0], [3.0, 0.0], [1.5, 2.6]])
    X = np.vstack([c + rng.normal(0, 1.0, (n_per, 2)) for c in centers])
    y = np.repeat(np.arange(3), n_per)
    return X, y


def train_test_split(X, y, test_fraction=0.25, seed=0):
    idx = np.random.default_rng(seed).permutation(len(X))
    cut = int(len(X) * (1 - test_fraction))
    return X[idx[:cut]], y[idx[:cut]], X[idx[cut:]], y[idx[cut:]]


if __name__ == "__main__":
    X, y = make_data()
    X_train, y_train, X_test, y_test = train_test_split(X, y)
    k, results = best_k(X_train, y_train)
    print("5-fold cross-validated accuracy by k:")
    for kk, acc in results.items():
        print("  k=%-2d %.3f %s" % (kk, acc, "<- best" if kk == k else ""))
    model = KNN(k).fit(X_train, y_train)
    print("\nheld-out test accuracy with k=%d: %.3f" % (k, model.score(X_test, y_test)))
