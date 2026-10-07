"""A small multi-layer perceptron with backpropagation, written with numpy only.

Hidden layers use tanh; the output is a sigmoid trained with binary cross-entropy.
"""

import numpy as np


def sigmoid(z):
    return 1.0 / (1.0 + np.exp(-z))


class MLP:
    def __init__(self, layer_sizes, seed=0):
        rng = np.random.default_rng(seed)
        self.W = [rng.normal(0, np.sqrt(1.0 / m), (m, n))
                  for m, n in zip(layer_sizes[:-1], layer_sizes[1:])]
        self.b = [np.zeros(n) for n in layer_sizes[1:]]

    def forward(self, X):
        """Return the activations of every layer (the first entry is X itself)."""
        acts = [np.asarray(X, dtype=float)]
        for i, (W, b) in enumerate(zip(self.W, self.b)):
            z = acts[-1] @ W + b
            acts.append(sigmoid(z) if i == len(self.W) - 1 else np.tanh(z))
        return acts

    def predict_proba(self, X):
        return self.forward(X)[-1]

    def predict(self, X):
        return (self.predict_proba(X) >= 0.5).astype(int)

    def loss(self, X, y):
        p = np.clip(self.predict_proba(X), 1e-12, 1 - 1e-12)
        y = np.asarray(y, dtype=float).reshape(p.shape)
        return float(-np.mean(y * np.log(p) + (1 - y) * np.log(1 - p)))

    def gradients(self, X, y):
        acts = self.forward(X)
        y = np.asarray(y, dtype=float).reshape(acts[-1].shape)
        delta = (acts[-1] - y) / y.size  # d(loss)/d(z): loss averages over every sample and output
        gW, gb = [None] * len(self.W), [None] * len(self.b)
        for i in reversed(range(len(self.W))):
            gW[i] = acts[i].T @ delta
            gb[i] = delta.sum(axis=0)
            if i > 0:
                delta = (delta @ self.W[i].T) * (1 - acts[i] ** 2)  # tanh' = 1 - tanh^2
        return gW, gb

    def train(self, X, y, epochs=5000, lr=0.5, log_every=0):
        history = []
        for epoch in range(epochs):
            gW, gb = self.gradients(X, y)
            for i in range(len(self.W)):
                self.W[i] -= lr * gW[i]
                self.b[i] -= lr * gb[i]
            history.append(self.loss(X, y))
            if log_every and epoch % log_every == 0:
                print("epoch %5d  loss %.4f" % (epoch, history[-1]))
        return history


XOR_X = np.array([[0, 0], [0, 1], [1, 0], [1, 1]], dtype=float)
XOR_Y = np.array([[0], [1], [1], [0]], dtype=float)

if __name__ == "__main__":
    net = MLP([2, 4, 1], seed=1)
    net.train(XOR_X, XOR_Y, epochs=3000, lr=0.5, log_every=500)
    print("\n x1 x2 | target | P(1)   | pred")
    for x, t, p in zip(XOR_X, XOR_Y, net.predict_proba(XOR_X)):
        print("  %d  %d |   %d    | %.4f |  %d" % (x[0], x[1], t[0], p[0], p[0] >= 0.5))
