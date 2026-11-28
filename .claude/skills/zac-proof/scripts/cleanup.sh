#!/usr/bin/env bash
#
# SPDX-FileCopyrightText: 2026 INFO.nl
# SPDX-License-Identifier: EUPL-1.2+
#
# Removes the proof worktree of one run, or of all runs. Leaves the evidence folder and the kind cluster alone.
#
# Usage: bash cleanup.sh <name, e.g. pr-1234 | --all>
set -euo pipefail

name="${1:?usage: cleanup.sh <name | --all>}"
git_common_dir=$(git -C "$(dirname "$0")" rev-parse --path-format=absolute --git-common-dir)
main_checkout=$(dirname "$git_common_dir")

if [ "$name" = "--all" ]; then
  worktrees=("$main_checkout"/.claude/worktrees/proof-*)
else
  worktrees=("$main_checkout/.claude/worktrees/proof-$name")
fi

for worktree in "${worktrees[@]}"; do
  [ -d "$worktree" ] || continue
  git -C "$main_checkout" worktree remove --force "$worktree"
  echo "Removed $worktree"
done
git -C "$main_checkout" worktree prune
