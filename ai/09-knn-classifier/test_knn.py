import unittest

import numpy as np

from knn import KNN, best_k, cross_val_score, kfold_indices, make_data, standardize, train_test_split


class KNNTests(unittest.TestCase):
    def test_k1_memorises_training_data(self):
        X, y = make_data(seed=1)
        self.assertEqual(KNN(1).fit(X, y).score(X, y), 1.0)

    def test_majority_vote(self):
        model = KNN(3).fit([[0], [1], [2]], [0, 0, 1])
        self.assertEqual(model.predict_one(np.array([1.9])), 0)

    def test_distance_weighting_can_flip_the_vote(self):
        X, y = [[0], [1], [2]], [0, 0, 1]
        query = np.array([1.9])
        self.assertEqual(KNN(3).fit(X, y).predict_one(query), 0)
        self.assertEqual(KNN(3, weighted=True).fit(X, y).predict_one(query), 1)

    def test_tie_goes_to_nearest_neighbour(self):
        model = KNN(2).fit([[0], [10]], [0, 1])
        self.assertEqual(model.predict_one(np.array([2.0])), 0)
        self.assertEqual(model.predict_one(np.array([8.0])), 1)

    def test_invalid_k_raises(self):
        with self.assertRaises(ValueError):
            KNN(0)
        with self.assertRaises(ValueError):
            KNN(5).fit([[0], [1]], [0, 1])

    def test_string_labels(self):
        model = KNN(1).fit([[0], [10]], ["cat", "dog"])
        self.assertEqual(list(model.predict([[1], [9]])), ["cat", "dog"])

    def test_folds_partition_every_index_once(self):
        folds = kfold_indices(23, folds=5, seed=3)
        self.assertEqual(len(folds), 5)
        self.assertEqual(sorted(np.concatenate(folds)), list(range(23)))

    def test_cross_val_scores(self):
        X, y = make_data(seed=2)
        scores = cross_val_score(lambda: KNN(5), X, y, folds=4)
        self.assertEqual(len(scores), 4)
        self.assertTrue(all(0.0 <= s <= 1.0 for s in scores))
        self.assertGreater(np.mean(scores), 0.6)  # far above the 1/3 chance level

    def test_best_k_picks_from_candidates_and_generalises(self):
        X, y = make_data(seed=4)
        X_train, y_train, X_test, y_test = train_test_split(X, y, seed=4)
        k, results = best_k(X_train, y_train, ks=[1, 5, 9])
        self.assertIn(k, [1, 5, 9])
        self.assertEqual(results[k], max(results.values()))
        self.assertGreater(KNN(k).fit(X_train, y_train).score(X_test, y_test), 0.6)

    def test_standardising_rescues_a_badly_scaled_feature(self):
        rng = np.random.default_rng(0)
        n = 200
        y = rng.integers(0, 2, n)
        signal = y * 4.0 + rng.normal(0, 0.5, n)       # separates the classes
        noise = rng.normal(0, 1.0, n) * 10_000          # pure noise, huge scale
        X = np.column_stack([signal, noise])
        X_train, y_train, X_test, y_test = train_test_split(X, y, seed=0)

        raw = KNN(5).fit(X_train, y_train).score(X_test, y_test)
        Xs_train, Xs_test = standardize(X_train, X_test)
        scaled = KNN(5).fit(Xs_train, y_train).score(Xs_test, y_test)
        self.assertLess(raw, 0.8)
        self.assertGreater(scaled, 0.9)


if __name__ == "__main__":
    unittest.main()
