import unittest

import numpy as np

from kmeans import KMeans, ascii_plot, make_blobs

CENTERS = [(0, 0), (8, 0), (4, 7)]


class KMeansTests(unittest.TestCase):
    def setUp(self):
        self.X, self.y = make_blobs(CENTERS, n_per=50, spread=0.5, seed=7)

    def test_recovers_well_separated_clusters(self):
        model = KMeans(k=3, seed=0).fit(self.X)
        # Each true blob must map onto exactly one learned cluster.
        for blob in range(3):
            self.assertEqual(len(set(model.labels_[self.y == blob])), 1)
        self.assertEqual(len(set(model.labels_)), 3)

    def test_centroids_close_to_true_centers(self):
        model = KMeans(k=3, seed=0).fit(self.X)
        for center in CENTERS:
            nearest = np.min(np.linalg.norm(model.centroids - center, axis=1))
            self.assertLess(nearest, 0.4)

    def test_inertia_never_increases(self):
        model = KMeans(k=3, seed=3).fit(self.X)
        h = model.inertia_history
        self.assertTrue(all(b <= a + 1e-9 for a, b in zip(h, h[1:])))

    def test_same_seed_gives_same_result(self):
        a = KMeans(k=3, seed=5).fit(self.X)
        b = KMeans(k=3, seed=5).fit(self.X)
        np.testing.assert_array_equal(a.labels_, b.labels_)

    def test_k_equal_to_n_gives_zero_inertia(self):
        X = np.array([[0.0, 0.0], [1.0, 1.0], [5.0, 5.0]])
        self.assertAlmostEqual(KMeans(k=3, seed=0).fit(X).inertia_, 0.0)

    def test_invalid_k_raises(self):
        with self.assertRaises(ValueError):
            KMeans(k=0).fit(self.X)
        with self.assertRaises(ValueError):
            KMeans(k=len(self.X) + 1).fit(self.X)

    def test_predict_assigns_new_points_to_nearest_centroid(self):
        model = KMeans(k=3, seed=0).fit(self.X)
        labels = model.predict(np.array(CENTERS, dtype=float))
        self.assertEqual(len(set(labels)), 3)

    def test_duplicate_points_do_not_crash(self):
        X = np.zeros((10, 2))
        model = KMeans(k=3, seed=0).fit(X)
        self.assertAlmostEqual(model.inertia_, 0.0)

    def test_ascii_plot_keeps_centroids_outside_the_points_on_the_grid(self):
        # Happens when plotting new points with model.predict labels and the fitted centroids.
        X = np.array([[1.0, 1.0], [5.0, 3.0], [3.0, 4.0]])
        centroids = np.array([[-1.0, 0.0], [7.0, 5.0]])
        lines = ascii_plot(X, [0, 1, 2], centroids, width=20, height=6).split("\n")
        self.assertEqual([len(line) for line in lines], [20] * 6)
        self.assertEqual(lines[-1][0], "#")  # bottom-left, not wrapped to the right edge
        self.assertEqual(lines[0][-1], "#")  # top-right


if __name__ == "__main__":
    unittest.main()
