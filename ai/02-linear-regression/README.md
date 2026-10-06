# 02 · Linear Regression from Scratch

Fits `y = w·x + b` two ways and checks that they agree:

1. **Gradient descent**, which repeatedly nudges the weights downhill on the mean squared error.
2. **The normal equation**, the closed-form least-squares solution.

```bash
python linreg.py        # needs numpy
```

```
true            : y = 3.000*x1 - 2.000*x2 + 5.000
gradient descent: y = 3.017*x1 -1.968*x2 +5.004   (R^2 = 0.9981)
normal equation : y = 3.017*x1 -1.968*x2 +5.004
loss: 160.908 -> 0.235 over 1000 epochs
```

The data is synthetic, a known line plus Gaussian noise, so you can see how close each method gets to the truth.

## How it works

- Features are standardised before training, so gradient descent converges even when features are on wildly different scales (a test covers a 0-1 feature next to a 0-10,000 one).
- The learned weights are mapped back to the original feature scale afterwards, so `coef_` and `intercept_` are directly interpretable.
- Constant features are handled instead of dividing by zero.
- `score` returns R², where 1.0 is a perfect fit and 0.0 is no better than predicting the mean.

## Tests

```bash
python -m unittest
```
