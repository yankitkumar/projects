"""Linear regression from scratch: gradient descent vs. the closed-form solution."""

import numpy as np


class LinearRegression:
    """Ordinary least squares fit by batch gradient descent on standardized features."""

    def __init__(self, lr=0.1, epochs=1000):
        self.lr = lr
        self.epochs = epochs
        self.coef_ = None
        self.intercept_ = 0.0
        self.loss_history = []

    def fit(self, X, y):
        X = np.asarray(X, dtype=float)
        y = np.asarray(y, dtype=float)
        mu, sigma = X.mean(axis=0), X.std(axis=0)
        sigma[sigma == 0] = 1.0
        Xs = (X - mu) / sigma

        n, d = Xs.shape
        w, b = np.zeros(d), 0.0
        self.loss_history = []
        for _ in range(self.epochs):
            err = Xs @ w + b - y
            self.loss_history.append(float(np.mean(err ** 2)))
            w -= self.lr * (2 / n) * (Xs.T @ err)
            b -= self.lr * (2 / n) * err.sum()

        # Map the weights learned on standardized features back to the original scale.
        self.coef_ = w / sigma
        self.intercept_ = float(b - np.sum(w * mu / sigma))
        return self

    def predict(self, X):
        return np.asarray(X, dtype=float) @ self.coef_ + self.intercept_

    def score(self, X, y):
        """R^2: 1.0 is a perfect fit, 0.0 is no better than predicting the mean."""
        y = np.asarray(y, dtype=float)
        ss_res = np.sum((y - self.predict(X)) ** 2)
        ss_tot = np.sum((y - y.mean()) ** 2)
        return float(1 - ss_res / ss_tot)


def normal_equation(X, y):
    """Closed-form least squares. Returns (coef, intercept)."""
    X = np.asarray(X, dtype=float)
    A = np.hstack([X, np.ones((len(X), 1))])
    solution, *_ = np.linalg.lstsq(A, np.asarray(y, dtype=float), rcond=None)
    return solution[:-1], float(solution[-1])


def make_data(n=200, noise=0.5, seed=0):
    rng = np.random.default_rng(seed)
    X = rng.uniform(-5, 5, size=(n, 2))
    y = 3.0 * X[:, 0] - 2.0 * X[:, 1] + 5.0 + rng.normal(0, noise, n)
    return X, y


if __name__ == "__main__":
    X, y = make_data()
    model = LinearRegression().fit(X, y)
    coef, intercept = normal_equation(X, y)
    print("true            : y = 3.000*x1 - 2.000*x2 + 5.000")
    print("gradient descent: y = %.3f*x1 %+.3f*x2 %+.3f   (R^2 = %.4f)"
          % (*model.coef_, model.intercept_, model.score(X, y)))
    print("normal equation : y = %.3f*x1 %+.3f*x2 %+.3f" % (*coef, intercept))
    print("loss: %.3f -> %.3f over %d epochs"
          % (model.loss_history[0], model.loss_history[-1], model.epochs))
