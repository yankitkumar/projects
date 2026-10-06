"""Decision tree classifier (CART with Gini impurity), written from scratch."""

import numpy as np


def gini(counts):
    """Gini impurity of a class-count vector: 0 for a pure node, up to 1 - 1/k when mixed."""
    n = counts.sum()
    return 0.0 if n == 0 else float(1.0 - np.sum((counts / n) ** 2))


class DecisionTree:
    def __init__(self, max_depth=None, min_samples_split=2):
        self.max_depth = max_depth
        self.min_samples_split = min_samples_split

    def fit(self, X, y, feature_names=None):
        X = np.asarray(X, dtype=float)
        y = np.asarray(y)
        self.classes_ = np.unique(y)
        self.feature_names = feature_names or ["x%d" % j for j in range(X.shape[1])]
        self.importances_ = np.zeros(X.shape[1])
        self.root = self._build(X, np.searchsorted(self.classes_, y), depth=0)
        total = self.importances_.sum()
        if total > 0:
            self.importances_ /= total
        return self

    def _build(self, X, y, depth):
        counts = np.bincount(y, minlength=len(self.classes_))
        node = {"counts": counts, "pred": int(counts.argmax())}
        too_deep = self.max_depth is not None and depth >= self.max_depth
        if counts.max() == len(y) or len(y) < self.min_samples_split or too_deep:
            return node
        split = self._best_split(X, y, counts)
        if split is None:
            return node
        feature, threshold, gain = split
        self.importances_[feature] += gain * len(y)
        go_left = X[:, feature] <= threshold
        node.update(
            feature=feature,
            threshold=threshold,
            left=self._build(X[go_left], y[go_left], depth + 1),
            right=self._build(X[~go_left], y[~go_left], depth + 1),
        )
        return node

    @staticmethod
    def _best_split(X, y, counts):
        """Scan every feature's sorted values for the threshold with the largest Gini gain."""
        n = len(y)
        parent = gini(counts)
        best, best_gain = None, 1e-12  # a split must strictly improve purity
        for j in range(X.shape[1]):
            order = np.argsort(X[:, j], kind="stable")
            xs, ys = X[order, j], y[order]
            left, right = np.zeros_like(counts), counts.copy()
            for i in range(n - 1):
                left[ys[i]] += 1
                right[ys[i]] -= 1
                if xs[i] == xs[i + 1]:
                    continue  # can't separate equal values
                weighted = ((i + 1) * gini(left) + (n - i - 1) * gini(right)) / n
                gain = parent - weighted
                if gain > best_gain:
                    best_gain, best = gain, (j, (xs[i] + xs[i + 1]) / 2, gain)
        return best

    def _predict_one(self, x):
        node = self.root
        while "feature" in node:
            node = node["left"] if x[node["feature"]] <= node["threshold"] else node["right"]
        return self.classes_[node["pred"]]

    def predict(self, X):
        return np.array([self._predict_one(x) for x in np.asarray(X, dtype=float)])

    def score(self, X, y):
        return float(np.mean(self.predict(X) == np.asarray(y)))

    def depth(self):
        def walk(node):
            return 0 if "feature" not in node else 1 + max(walk(node["left"]), walk(node["right"]))
        return walk(self.root)

    def export_text(self):
        lines = []

        def walk(node, indent):
            pad = "|   " * indent
            if "feature" not in node:
                lines.append("%s-> %s  (%s)" % (pad, self.classes_[node["pred"]], int(node["counts"].sum())))
                return
            name, t = self.feature_names[node["feature"]], node["threshold"]
            lines.append("%s%s <= %.2f" % (pad, name, t))
            walk(node["left"], indent + 1)
            lines.append("%s%s >  %.2f" % (pad, name, t))
            walk(node["right"], indent + 1)

        walk(self.root, 0)
        return "\n".join(lines)


FEATURES = ["income_k", "debt_ratio", "years_employed"]


def make_loans(n=300, noise=0.05, seed=0):
    """Synthetic loan decisions: approve when income is high, or income is OK and debt is low."""
    rng = np.random.default_rng(seed)
    X = np.column_stack([
        rng.uniform(15, 120, n),    # income, in thousands
        rng.uniform(0.0, 0.8, n),   # debt-to-income ratio
        rng.uniform(0, 20, n),      # years employed (irrelevant by design)
    ])
    y = ((X[:, 0] > 90) | ((X[:, 0] > 50) & (X[:, 1] < 0.4))).astype(int)
    flip = rng.random(n) < noise
    y = np.where(flip, 1 - y, y)
    return X, np.where(y == 1, "approve", "decline")


if __name__ == "__main__":
    X, y = make_loans()
    cut = 225
    tree = DecisionTree(max_depth=3).fit(X[:cut], y[:cut], FEATURES)
    print(tree.export_text())
    print("\ntrain accuracy: %.3f   test accuracy: %.3f"
          % (tree.score(X[:cut], y[:cut]), tree.score(X[cut:], y[cut:])))
    print("feature importance:", ", ".join("%s=%.2f" % p for p in zip(FEATURES, tree.importances_)))
