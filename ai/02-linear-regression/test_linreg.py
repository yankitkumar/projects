import unittest

import numpy as np

from linreg import LinearRegression, make_data, normal_equation


class LinearRegressionTests(unittest.TestCase):
    def test_recovers_true_coefficients(self):
        X, y = make_data(noise=0.1, seed=1)
        model = LinearRegression().fit(X, y)
        np.testing.assert_allclose(model.coef_, [3.0, -2.0], atol=0.05)
        self.assertAlmostEqual(model.intercept_, 5.0, delta=0.05)

    def test_matches_normal_equation(self):
        X, y = make_data(seed=2)
        model = LinearRegression(epochs=2000).fit(X, y)
        coef, intercept = normal_equation(X, y)
        np.testing.assert_allclose(model.coef_, coef, atol=1e-4)
        self.assertAlmostEqual(model.intercept_, intercept, places=4)

    def test_loss_decreases(self):
        X, y = make_data(seed=3)
        model = LinearRegression().fit(X, y)
        self.assertLess(model.loss_history[-1], model.loss_history[0] / 100)

    def test_handles_features_on_very_different_scales(self):
        rng = np.random.default_rng(4)
        X = np.column_stack([rng.uniform(0, 1, 100), rng.uniform(0, 10_000, 100)])
        y = 40 * X[:, 0] + 0.002 * X[:, 1] + 1
        model = LinearRegression(epochs=2000).fit(X, y)
        self.assertGreater(model.score(X, y), 0.999)

    def test_constant_feature_does_not_break_fit(self):
        X = np.column_stack([np.arange(10.0), np.ones(10)])
        y = 2 * X[:, 0] + 1
        model = LinearRegression(epochs=2000).fit(X, y)
        self.assertGreater(model.score(X, y), 0.999)

    def test_constant_feature_with_inexact_float_mean(self):
        # The float std of a column of 0.1s is ~1e-17, not exactly 0.
        for value in (0.1, 0.3, 1.1, 7.7):
            X = np.column_stack([np.arange(10.0), np.full(10, value)])
            y = 2 * X[:, 0] + 1
            model = LinearRegression(epochs=2000).fit(X, y)
            self.assertGreater(model.score(X, y), 0.999)
            self.assertAlmostEqual(model.predict([[3.0, value]])[0], 7.0, places=4)

    def test_r2_is_zero_for_mean_predictor(self):
        X = np.arange(10.0).reshape(-1, 1)
        y = np.array([1.0, -1.0] * 5)
        model = LinearRegression(epochs=0).fit(X, y)
        model.coef_, model.intercept_ = np.zeros(1), float(y.mean())
        self.assertAlmostEqual(model.score(X, y), 0.0)


if __name__ == "__main__":
    unittest.main()
