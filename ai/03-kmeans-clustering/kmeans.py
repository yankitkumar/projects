"""K-means clustering from scratch (k-means++ initialisation) with an ASCII scatter plot."""

import numpy as np


class KMeans:
    def __init__(self, k, max_iter=100, tol=1e-6, seed=0):
        self.k = k
        self.max_iter = max_iter
        self.tol = tol
        self.rng = np.random.default_rng(seed)
        self.centroids = None
        self.labels_ = None
        self.inertia_history = []

    def _init_centroids(self, X):
        """k-means++: pick each new centroid with probability proportional to squared distance."""
        centroids = [X[self.rng.integers(len(X))]]
        for _ in range(1, self.k):
            d2 = np.min([np.sum((X - c) ** 2, axis=1) for c in centroids], axis=0)
            if d2.sum() == 0:  # every point already sits on a centroid
                centroids.append(X[self.rng.integers(len(X))])
                continue
            centroids.append(X[self.rng.choice(len(X), p=d2 / d2.sum())])
        return np.array(centroids, dtype=float)

    def _assign(self, X):
        d2 = np.sum((X[:, None, :] - self.centroids[None, :, :]) ** 2, axis=2)
        return d2.argmin(axis=1), d2.min(axis=1)

    def fit(self, X):
        X = np.asarray(X, dtype=float)
        if not 1 <= self.k <= len(X):
            raise ValueError("k must be between 1 and the number of samples (%d)" % len(X))
        self.centroids = self._init_centroids(X)
        self.inertia_history = []
        for _ in range(self.max_iter):
            labels, d2 = self._assign(X)
            self.inertia_history.append(float(d2.sum()))
            new = self.centroids.copy()
            for j in range(self.k):
                members = X[labels == j]
                if len(members):
                    new[j] = members.mean(axis=0)
                else:  # empty cluster: restart it at the point farthest from its centroid
                    new[j] = X[d2.argmax()]
            shift = np.linalg.norm(new - self.centroids)
            self.centroids = new
            if shift < self.tol:
                break
        self.labels_, d2 = self._assign(X)
        self.inertia_history.append(float(d2.sum()))
        return self

    def predict(self, X):
        return self._assign(np.asarray(X, dtype=float))[0]

    @property
    def inertia_(self):
        return self.inertia_history[-1]


def make_blobs(centers, n_per=60, spread=0.6, seed=0):
    rng = np.random.default_rng(seed)
    centers = np.asarray(centers, dtype=float)
    X = np.vstack([c + rng.normal(0, spread, (n_per, 2)) for c in centers])
    y = np.repeat(np.arange(len(centers)), n_per)
    return X, y


def ascii_plot(X, labels, centroids, width=64, height=22):
    """Draw points as their cluster digit (0-9) and centroids as '#'."""
    # Scale to the centroids too: they can lie outside the points (e.g. when plotting new data).
    allp = np.vstack([X, centroids])
    lo, hi = allp.min(axis=0), allp.max(axis=0)
    span = np.where(hi - lo == 0, 1, hi - lo)

    def cell(p):
        col = int((p[0] - lo[0]) / span[0] * (width - 1))
        row = int((hi[1] - p[1]) / span[1] * (height - 1))
        return row, col

    grid = [[" "] * width for _ in range(height)]
    for p, lab in zip(X, labels):
        r, c = cell(p)
        grid[r][c] = str(lab % 10)
    for p in centroids:
        r, c = cell(p)
        grid[r][c] = "#"
    return "\n".join("".join(row) for row in grid)


if __name__ == "__main__":
    X, _ = make_blobs([(0, 0), (6, 1), (3, 6)])
    model = KMeans(k=3, seed=1).fit(X)
    print(ascii_plot(X, model.labels_, model.centroids))
    print("\ncentroids:")
    for j, c in enumerate(model.centroids):
        print("  cluster %d: (%.2f, %.2f)  size=%d" % (j, c[0], c[1], int((model.labels_ == j).sum())))
    print("inertia: %.1f -> %.1f in %d iterations"
          % (model.inertia_history[0], model.inertia_, len(model.inertia_history) - 1))
