import unittest

import numpy as np

from tree import FEATURES, DecisionTree, gini, make_loans


class GiniTests(unittest.TestCase):
    def test_known_values(self):
        self.assertEqual(gini(np.array([5, 0])), 0.0)
        self.assertAlmostEqual(gini(np.array([5, 5])), 0.5)
        self.assertAlmostEqual(gini(np.array([1, 1, 1])), 2 / 3)
        self.assertEqual(gini(np.array([0, 0])), 0.0)


class DecisionTreeTests(unittest.TestCase):
    def test_separable_data_needs_one_split(self):
        X = np.arange(10, dtype=float).reshape(-1, 1)
        y = (X[:, 0] >= 5).astype(int)
        tree = DecisionTree().fit(X, y)
        self.assertEqual(tree.score(X, y), 1.0)
        self.assertEqual(tree.depth(), 1)
        self.assertAlmostEqual(tree.root["threshold"], 4.5)

    def test_splits_values_one_ulp_apart(self):
        # The float midpoint of 0.3 and 0.1 + 0.2 rounds up to the larger value.
        X, y = [[0.3], [0.1 + 0.2]], ["low", "high"]
        tree = DecisionTree().fit(X, y)
        self.assertEqual(tree.depth(), 1)
        self.assertEqual(tree.score(X, y), 1.0)

    def test_max_depth_is_respected(self):
        X, y = make_loans(seed=1)
        for depth in (1, 2, 4):
            self.assertLessEqual(DecisionTree(max_depth=depth).fit(X, y).depth(), depth)

    def test_unbounded_tree_memorises_distinct_points(self):
        rng = np.random.default_rng(0)
        X = rng.normal(size=(60, 3))
        y = rng.integers(0, 3, 60)
        self.assertEqual(DecisionTree().fit(X, y).score(X, y), 1.0)

    def test_shallow_tree_generalises_on_noisy_data(self):
        X, y = make_loans(n=600, seed=2)
        tree = DecisionTree(max_depth=3).fit(X[:400], y[:400])
        self.assertGreater(tree.score(X[400:], y[400:]), 0.85)

    def test_importance_finds_informative_features(self):
        X, y = make_loans(n=600, noise=0.0, seed=3)
        tree = DecisionTree(max_depth=3).fit(X, y, FEATURES)
        self.assertAlmostEqual(tree.importances_.sum(), 1.0)
        self.assertEqual(int(tree.importances_.argmax()), 0)       # income matters most
        self.assertLess(tree.importances_[2], 0.05)                # years employed is noise

    def test_single_class_gives_a_leaf(self):
        tree = DecisionTree().fit([[1], [2], [3]], ["a", "a", "a"])
        self.assertEqual(tree.depth(), 0)
        self.assertEqual(list(tree.predict([[100]])), ["a"])

    def test_identical_features_with_mixed_labels_gives_majority_leaf(self):
        tree = DecisionTree().fit([[1], [1], [1]], ["a", "b", "b"])
        self.assertEqual(tree.depth(), 0)
        self.assertEqual(list(tree.predict([[1]])), ["b"])

    def test_string_labels_and_export_text(self):
        X, y = make_loans(seed=4)
        tree = DecisionTree(max_depth=2).fit(X, y, FEATURES)
        text = tree.export_text()
        self.assertIn("income_k <=", text)
        self.assertIn("-> approve", text)
        self.assertTrue(set(tree.predict(X)) <= {"approve", "decline"})

    def test_feature_names_can_be_a_numpy_array(self):
        X, y = make_loans(seed=4)
        tree = DecisionTree(max_depth=2).fit(X, y, np.array(FEATURES))
        self.assertEqual(tree.feature_names, FEATURES)
        self.assertIn("income_k <=", tree.export_text())


if __name__ == "__main__":
    unittest.main()
