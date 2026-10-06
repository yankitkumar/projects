"""Byte-pair encoding (BPE) tokenizer from scratch: the algorithm behind most LLM tokenizers.

Start with raw UTF-8 bytes (256 base tokens). Repeatedly find the most frequent adjacent pair of
tokens and replace it with a new token. Encoding replays those merges, earliest first.
Working on bytes means any text, in any language, round-trips exactly.
"""

import sys
from collections import Counter


def merge(ids, pair, new_id):
    """Replace every non-overlapping occurrence of `pair` in `ids` with `new_id`, left to right."""
    out, i = [], 0
    while i < len(ids):
        if i + 1 < len(ids) and (ids[i], ids[i + 1]) == pair:
            out.append(new_id)
            i += 2
        else:
            out.append(ids[i])
            i += 1
    return out


class BPETokenizer:
    def __init__(self):
        self.merges = {}                                  # (id, id) -> new id, in the order learned
        self.vocab = {i: bytes([i]) for i in range(256)}  # id -> the bytes it stands for

    def train(self, text, vocab_size):
        if vocab_size < 256:
            raise ValueError("vocab_size must be at least 256 (the byte alphabet)")
        ids = list(text.encode("utf-8"))
        for new_id in range(256, vocab_size):
            counts = Counter(zip(ids, ids[1:]))
            if not counts:
                break
            # Most frequent pair; ties go to the smallest ids so training is deterministic.
            pair = max(counts, key=lambda p: (counts[p], -p[0], -p[1]))
            if counts[pair] < 2:
                break  # nothing repeats, so another merge would not compress anything
            ids = merge(ids, pair, new_id)
            self.merges[pair] = new_id
            self.vocab[new_id] = self.vocab[pair[0]] + self.vocab[pair[1]]
        return self

    def encode(self, text):
        ids = list(text.encode("utf-8"))
        while len(ids) >= 2:
            # Apply the earliest-learned merge available (merge ids grow in learning order).
            pair = min(zip(ids, ids[1:]), key=lambda p: self.merges.get(p, float("inf")))
            if pair not in self.merges:
                break
            ids = merge(ids, pair, self.merges[pair])
        return ids

    def decode(self, ids):
        return b"".join(self.vocab[i] for i in ids).decode("utf-8", errors="replace")

    def pieces(self, text):
        """The text split into the strings its tokens stand for (handy for display)."""
        return [self.vocab[i].decode("utf-8", errors="replace") for i in self.encode(text)]


SAMPLE = (
    "the cat sat on the mat. the cat saw the rat. the rat ran from the cat, "
    "and the cat ran after the rat. then the cat sat on the mat again. "
) * 4

if __name__ == "__main__":
    text = " ".join(sys.argv[1:]) or "the cat saw the rat on the mat"
    tok = BPETokenizer().train(SAMPLE, vocab_size=300)
    print("learned %d merges; the first ten:" % len(tok.merges))
    for (a, b), new_id in list(tok.merges.items())[:10]:
        print("  %-5r + %-5r -> %r" % (tok.vocab[a].decode(errors="replace"),
                                       tok.vocab[b].decode(errors="replace"),
                                       tok.vocab[new_id].decode(errors="replace")))
    ids = tok.encode(text)
    print("\ntext   :", repr(text))
    print("pieces :", tok.pieces(text))
    print("%d bytes -> %d tokens (%.1fx compression); round trip ok: %s"
          % (len(text.encode()), len(ids), len(text.encode()) / len(ids), tok.decode(ids) == text))
