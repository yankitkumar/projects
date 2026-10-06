# 16 · Byte-Pair Encoding Tokenizer

The algorithm that turns text into tokens for most large language models, built from scratch. It learns which pieces of text occur often enough to deserve their own token.

```bash
python bpe.py
python bpe.py "the cat sat on the mat"
```

```
learned 44 merges; the first ten:
  'a'   + 't'   -> 'at'
  'h'   + 'e'   -> 'he'
  't'   + 'he'  -> 'the'
  ' '   + 'the' -> ' the'
  ...

text   : 'the cat saw the rat on the mat'
pieces : ['the', ' ', 'c', 'at ', 's', 'aw', ' the r', 'at on the m', 'at']
30 bytes -> 9 tokens (3.3x compression); round trip ok: True
```

## How it works

1. Start with the 256 possible byte values as the base vocabulary. The text is first encoded as UTF-8 bytes.
2. Count every adjacent pair of tokens. Replace the most frequent pair with a new token, and record the merge.
3. Repeat until the vocabulary reaches the target size, or until no pair occurs twice.
4. To **encode** new text, start from its bytes and replay the learned merges, earliest first.
5. To **decode**, concatenate the bytes each token stands for and decode them as UTF-8.

Working on bytes rather than characters means no input is ever "unknown": accents, other scripts and emoji all round-trip exactly, and a test checks this. Ties between equally frequent pairs go to the smallest token ids, so training is deterministic. A test checks the algorithm against the standard `aaabdaaabac` worked example.

## About the demo

The training text is tiny and very repetitive, so it learns long merges like `' the cat '`. Real tokenizers train on gigabytes and use a vocabulary of tens of thousands of tokens, and real ones usually split text into words first so merges don't span spaces.

## Tests

```bash
python -m unittest
```
