#!/usr/bin/env bash
# Run every project's unit tests. Set PYTHON to use a specific interpreter.
cd "$(dirname "$0")" || exit 1
PYTHON="${PYTHON:-python3}"
status=0
for dir in */; do
  ls "$dir"test_*.py >/dev/null 2>&1 || continue
  echo "=== ${dir%/}"
  (cd "$dir" && "$PYTHON" -m unittest -q 2>&1) || status=1
done
exit $status
