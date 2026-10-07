import unittest

import numpy as np

from mlp import MLP, XOR_X, XOR_Y


class MLPTests(unittest.TestCase):
    def test_gradients_match_finite_differences(self):
        for sizes in ([3, 5, 4, 1], [3, 5, 2]):  # single and multiple outputs
            with self.subTest(sizes=sizes):
                rng = np.random.default_rng(0)
                X = rng.normal(size=(6, 3))
                y = rng.integers(0, 2, size=(6, sizes[-1]))
                net = MLP(sizes, seed=2)
                gW, gb = net.gradients(X, y)

                eps = 1e-6
                for params, grads in ((net.W, gW), (net.b, gb)):
                    for param, grad in zip(params, grads):
                        numeric = np.zeros_like(param)
                        for idx in np.ndindex(*param.shape):
                            original = param[idx]
                            param[idx] = original + eps
                            plus = net.loss(X, y)
                            param[idx] = original - eps
                            minus = net.loss(X, y)
                            param[idx] = original
                            numeric[idx] = (plus - minus) / (2 * eps)
                        np.testing.assert_allclose(grad, numeric, rtol=1e-5, atol=1e-8)

    def test_learns_xor(self):
        net = MLP([2, 4, 1], seed=1)
        history = net.train(XOR_X, XOR_Y, epochs=3000, lr=0.5)
        np.testing.assert_array_equal(net.predict(XOR_X), XOR_Y.astype(int))
        self.assertLess(history[-1], 0.05)
        self.assertLess(history[-1], history[0])

    def test_single_layer_cannot_learn_xor(self):
        net = MLP([2, 1], seed=1)
        net.train(XOR_X, XOR_Y, epochs=3000, lr=0.5)
        self.assertFalse(np.array_equal(net.predict(XOR_X), XOR_Y.astype(int)))

    def test_learns_and_gate_with_one_layer(self):
        y = np.array([[0], [0], [0], [1]])
        net = MLP([2, 1], seed=0)
        net.train(XOR_X, y, epochs=3000, lr=1.0)
        np.testing.assert_array_equal(net.predict(XOR_X), y)

    def test_outputs_are_probabilities(self):
        p = MLP([2, 3, 1], seed=0).predict_proba(XOR_X)
        self.assertEqual(p.shape, (4, 1))
        self.assertTrue(((p > 0) & (p < 1)).all())


if __name__ == "__main__":
    unittest.main()
