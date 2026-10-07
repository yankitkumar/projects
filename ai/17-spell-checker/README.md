# 17 · Spelling Corrector

Fixes typos by finding the most likely word the writer meant. It uses edit distance plus word frequency, in the style of Peter Norvig's essay "How to Write a Spelling Corrector".

```bash
python spell.py
python spell.py "Teh wether was beutiful"
```

```
The wether was beutiful and we definately wanted to recieve the guests.
  -> The weather was beautiful and we definitely wanted to receive the guests.
Becuase of the rian we stayed in teh car.
  -> Because of the rain we stayed in the car.
```

## How it works

A typo is usually a real word with one or two small slips, and an **edit** is one of four slips: delete a letter, insert one, replace one, or swap two neighbours.

1. If the word is already in the vocabulary, leave it alone.
2. Otherwise generate every string one edit away, and keep those that are real words.
3. If there are none, repeat for two edits.
4. Among the candidates, pick the most frequent word, with ties broken alphabetically so the result is deterministic.

Fewer edits always beat higher frequency, because a closer word is more probable than a distant but common one. Capitalisation is preserved (`Teh` becomes `The`, `TEH` becomes `THE`), and `correct_text` leaves punctuation and spacing untouched.

`edit_distance` is a separate Damerau-Levenshtein implementation. The tests use it to check that every string `edits1` generates really is at most one edit away.

## Limitations

The vocabulary comes from `corpus.txt`, which is only 379 words (176 distinct), so the checker knows just those. A correctly spelled word outside it is "corrected" into a known word whenever one is within two edits: "tea" becomes "the" and "story" becomes "shore". Words with nothing nearby, like "guests" above, are left alone, and so are words with a letter outside a-z, like "café" or "naïve", because edits only use a-z. It also looks at each word on its own, so it can't tell "their" from "there". Point `SpellChecker` at a much bigger text to fix the first problem.

## Tests

```bash
python -m unittest
```
