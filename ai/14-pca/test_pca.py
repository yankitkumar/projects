import unittest

import numpy as np

from pca import PCA, make_data


class PCATests(unittest.TestCase):
    def setUp(self):
        self.X = make_data(seed=1)

    def test_components_are_orthonormal(self):
        c = PCA().fit(self.X).components_
        np.testing.assert_allclose(c @ c.T, np.eye(5), atol=1e-10)

    def test_variance_ratios_are_sorted_and_sum_to_one(self):
        ratio = PCA().fit(self.X).explained_variance_ratio_
        self.assertTrue(all(a >= b for a, b in zip(ratio, ratio[1:])))
        self.assertAlmostEqual(ratio.sum(), 1.0)

    def test_two_hidden_factors_explain_almost_everything(self):
        ratio = PCA(2).fit(self.X).explained_variance_ratio_
        self.assertGreater(ratio.sum(), 0.99)

    def test_full_rank_round_trip_is_exact(self):
        pca = PCA().fit(self.X)
        np.testing.assert_allclose(pca.inverse_transform(pca.transform(self.X)), self.X, atol=1e-9)

    def test_error_shrinks_as_components_are_added(self):
        errors = [PCA(k).fit(self.X).reconstruction_error(self.X) for k in range(1, 6)]
        self.assertTrue(all(b <= a + 1e-12 for a, b in zip(errors, errors[1:])))
        self.assertLess(errors[1], 0.05 * errors[0])   # the second factor matters a lot
        self.assertAlmostEqual(errors[-1], 0.0, places=9)

    def test_projection_is_centred_with_the_reported_variances(self):
        pca = PCA(3).fit(self.X)
        z = pca.transform(self.X)
        np.testing.assert_allclose(z.mean(axis=0), 0, atol=1e-9)
        np.testing.assert_allclose(z.var(axis=0, ddof=1), pca.explained_variance_, rtol=1e-8)

    def test_matches_singular_value_decomposition(self):
        centered = self.X - self.X.mean(axis=0)
        singular = np.linalg.svd(centered, compute_uv=False)
        expected = singular ** 2 / (len(self.X) - 1)
        np.testing.assert_allclose(PCA().fit(self.X).explained_variance_, expected, rtol=1e-8)

    def test_finds_the_direction_of_a_diagonal_line(self):
        t = np.linspace(-5, 5, 50)
        X = np.column_stack([t, t]) + np.random.default_rng(0).normal(0, 0.01, (50, 2))
        first = PCA(1).fit(X).components_[0]
        np.testing.assert_allclose(first, [1 / np.sqrt(2), 1 / np.sqrt(2)], atol=1e-3)

    def test_constant_data_does_not_divide_by_zero(self):
        pca = PCA(1).fit(np.ones((10, 3)))
        np.testing.assert_array_equal(pca.explained_variance_ratio_, [0.0])

    def test_invalid_arguments_raise(self):
        with self.assertRaises(ValueError):
            PCA(0).fit(self.X)
        with self.assertRaises(ValueError):
            PCA(6).fit(self.X)
        with self.assertRaises(ValueError):
            PCA().fit(self.X[:1])


if __name__ == "__main__":
    unittest.main()
