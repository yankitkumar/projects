# 05 · Neural Network that Learns XOR

A small multi-layer perceptron with **backpropagation**, written with numpy only. XOR is the classic task a single neuron can't solve, because the classes aren't linearly separable. A hidden layer fixes that.

```bash
python mlp.py           # needs numpy
```

```
epoch     0  loss 0.6861
epoch   500  loss 0.0122
epoch  1000  loss 0.0053
...
 x1 x2 | target | P(1)   | pred
  0  0 |   0    | 0.0002 |  0
  0  1 |   1    | 0.9982 |  1
  1  0 |   1    | 0.9982 |  1
  1  1 |   0    | 0.0026 |  0
```

## How it works

- `MLP([2, 4, 1])` means 2 inputs, one hidden layer of 4 tanh units and 1 sigmoid output.
- The loss is binary cross-entropy. With a sigmoid output, the gradient at the output simplifies to `prediction - target`.
- `gradients` runs backprop layer by layer and `train` does plain gradient descent.

## Tests

```bash
python -m unittest
```

- A **gradient check** compares every analytic gradient against a finite-difference estimate. This is how you know backprop is right.
- The 2-4-1 network learns XOR.
- A single-layer network fails on XOR but learns AND, which shows why the hidden layer matters.
