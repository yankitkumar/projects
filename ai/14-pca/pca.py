"""Principal component analysis from scratch, via the eigendecomposition of the covariance matrix."""

import numpy as np


class PCA:
    def __init__(self, n_components=None):
        self.n_components = n_components

    def fit(self, X):
        X = np.asarray(X, dtype=float)
        n, d = X.shape
        k = d if self.n_components is None else self.n_components
        if n < 2:
            raise ValueError("need at least two samples")
        if not 1 <= k <= d:
            raise ValueError("n_components must be between 1 and the number of features (%d)" % d)

        self.mean_ = X.mean(axis=0)
        centered = X - self.mean_
        covariance = centered.T @ centered / (n - 1)
        eigenvalues, eigenvectors = np.linalg.eigh(covariance)  # ascending order
        order = np.argsort(eigenvalues)[::-1]
        eigenvalues = np.clip(eigenvalues[order], 0, None)      # tiny negatives are rounding noise
        components = eigenvectors[:, order].T

        # Eigenvectors are only defined up to sign; make the largest entry positive for stable output.
        for row in components:
            if row[np.argmax(np.abs(row))] < 0:
                row *= -1

        total = eigenvalues.sum()
        self.components_ = components[:k]
        self.explained_variance_ = eigenvalues[:k]
        self.explained_variance_ratio_ = eigenvalues[:k] / total if total > 0 else np.zeros(k)
        return self

    def transform(self, X):
        return (np.asarray(X, dtype=float) - self.mean_) @ self.components_.T

    def inverse_transform(self, Z):
        return np.asarray(Z, dtype=float) @ self.components_ + self.mean_

    def reconstruction_error(self, X):
        """Mean squared distance between X and its projection onto the kept components."""
        X = np.asarray(X, dtype=float)
        return float(np.mean(np.sum((X - self.inverse_transform(self.transform(X))) ** 2, axis=1)))


def make_data(n=300, noise=0.05, seed=0):
    """Five measurements driven by just two hidden factors, plus a little noise."""
    rng = np.random.default_rng(seed)
    latent = rng.normal(size=(n, 2)) * [3.0, 1.5]
    mixing = np.array([[1.0, 0.5, -0.8, 0.2, 0.0],
                       [0.0, 1.0, 0.6, -0.4, 1.2]])
    return latent @ mixing + rng.normal(0, noise, (n, 5))


if __name__ == "__main__":
    X = make_data()
    pca = PCA().fit(X)
    print("component  variance explained  cumulative")
    cumulative = np.cumsum(pca.explained_variance_ratio_)
    for i, (ratio, cum) in enumerate(zip(pca.explained_variance_ratio_, cumulative), start=1):
        print("   PC%d        %6.2f%%          %6.2f%%" % (i, 100 * ratio, 100 * cum))

    print("\ncomponents kept -> reconstruction error")
    for k in range(1, 6):
        print("   %d -> %.4f" % (k, PCA(k).fit(X).reconstruction_error(X)))
