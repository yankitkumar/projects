# 14 · Principal Component Analysis

Finds the directions along which data varies most, so you can describe high-dimensional data with just a few numbers. It is written from scratch with numpy, using the eigendecomposition of the covariance matrix.

```bash
python pca.py        # needs numpy
```

The demo makes 5 measurements that are really driven by just 2 hidden factors, plus a little noise:

```
component  variance explained  cumulative
   PC1         72.89%           72.89%
   PC2         27.08%           99.97%
   PC3          0.01%           99.98%
   ...

components kept -> reconstruction error
   1 -> 6.4867
   2 -> 0.0072
   3 -> 0.0045
   ...
   5 -> 0.0000
```

Two components capture 99.97% of the variance. The error drops by a factor of about 900 from one component to two, then barely changes, which reveals the true dimensionality.

## How it works

1. Subtract the mean from each feature.
2. Compute the covariance matrix and take its eigenvectors, which are the principal components. Each eigenvalue is the variance along its component.
3. Sort by eigenvalue and keep the top `k`.
4. `transform` projects data onto those components, and `inverse_transform` maps it back, minus whatever the dropped components carried.

Eigenvectors are only defined up to sign, so each component is flipped to make its largest entry positive. That keeps the output stable between runs.

## Tests

```bash
python -m unittest
```

Tests check that components are orthonormal, that the explained variances match the singular values from `numpy.linalg.svd`, that a full-rank round trip is exact, that error never rises as components are added, and that a noisy 45° line gives a first component of (1, 1)/√2.
