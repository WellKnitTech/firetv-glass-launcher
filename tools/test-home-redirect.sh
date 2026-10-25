#!/bin/sh
# Device smoke test: with Glass installed and the usage watcher running, HOME should return to Glass.
set -eu
DIR="$(cd "$(dirname "$0")/.." && pwd)"
TV="$DIR/tools/tv"
"$TV" key HOME
sleep "${HOME_REDIRECT_WAIT:-8}"
FOCUS=$("$TV" shell 'dumpsys activity activities' | grep 'mResumedActivity:' | grep 'com.ivantrj.glasslauncher' || true)
printf '%s\n' "$FOCUS"
case "$FOCUS" in
  *com.ivantrj.glasslauncher*) exit 0 ;;
  *) echo "Expected Glass to take focus after HOME" >&2; exit 1 ;;
esac
