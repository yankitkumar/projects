# 04 · Spam Filter with Naive Bayes

A text classifier that learns which words signal spam, using a multinomial **Naive Bayes** model written from scratch with only the standard library.

```bash
python spam.py "You won a free prize, click to claim" "Can you send me the notes after the meeting?"
```

```
held-out accuracy: 13/14 = 93%
most spammy words: free, now, claim, click, win, link, account, cash
most hammy words : i, can, me, when, at, come, i'll, meeting

'You won a free prize, click to claim'
  -> spam (spam probability 100.0%)

'Can you send me the notes after the meeting?'
  -> ham (spam probability 0.0%)
```

## How it works

- Training counts how often each word appears in spam and in ordinary ("ham") messages.
- To classify, it adds up `log P(class) + Σ log P(word | class)` for each class and picks the larger. Working in log space avoids numeric underflow.
- **Laplace smoothing** adds one to every count, so a word seen only in spam can't force the ham probability to zero.
- Probabilities are normalised with a log-sum-exp step, so they sum to 1.

## About the data

`data.py` holds 56 short hand-written messages (28 spam, 28 ham), one in four held out as a test set. That is enough to see the algorithm work, but the accuracy is optimistic because the test messages are in the same style as the training ones. For real use, train on a proper corpus.

## Tests

```bash
python -m unittest
```
