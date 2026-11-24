#!/usr/bin/env bash
# Point git at tools/githooks so the pre-commit architecture check runs on every commit.
# Git does not install hooks on clone — run this once per clone (tools/new-app.sh does it).
set -euo pipefail
ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
git -C "$ROOT" config core.hooksPath tools/githooks
echo "hooks installed: core.hooksPath=tools/githooks"
