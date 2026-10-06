# 10 · Decision Tree

A classifier that learns a flowchart of yes/no questions about the features. It is a CART-style tree using Gini impurity, written from scratch.

```bash
python tree.py       # needs numpy
```

The demo learns loan approvals from synthetic data: approve when income is high, or when income is OK and debt is low. 5% of the labels are flipped as noise.

```
income_k <= 89.22
|   debt_ratio <= 0.39
|   |   income_k <= 49.43
|   |   |   -> decline  (28)
|   |   income_k >  49.43
|   |   |   -> approve  (33)
|   debt_ratio >  0.39
|   |   ...
income_k >  89.22
|   ...

train accuracy: 0.956   test accuracy: 0.907
feature importance: income_k=0.79, debt_ratio=0.19, years_employed=0.02
```

It recovers the rule used to generate the data. The thresholds land near 90 and 50 for income and 0.4 for debt, and `years_employed`, which is irrelevant by design, gets almost no importance.

## How it works

- **Gini impurity** measures how mixed a node is: 0 when every sample has the same class.
- At each node the tree tries every feature and every midpoint between neighbouring sorted values, and takes the split that lowers weighted impurity the most.
- It stops at a pure node, at `max_depth`, below `min_samples_split` samples, or when no split strictly improves purity.
- Feature importance is the total impurity reduction from each feature's splits, normalised to sum to 1.

## Things to know

- With no depth limit the tree memorises noise (a test confirms 100% training accuracy on random labels). Cap `max_depth` for real data.
- The tree is greedy. A perfectly balanced XOR pattern offers no first split that helps on its own, so it is not learned.

## Tests

```bash
python -m unittest
```
