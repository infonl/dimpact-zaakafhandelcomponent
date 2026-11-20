#!/usr/bin/env bash
#
# SPDX-FileCopyrightText: 2026 INFO.nl
# SPDX-License-Identifier: EUPL-1.2+
#
# Checks out a run ref from resolve.sh in a detached worktree under <main checkout>/.claude/worktrees/proof-<name>,
# so the user's own checkout stays untouched. Reuses and updates the worktree when it already exists.
# Prints the worktree path.
#
# Usage: bash checkout.sh <runRef: refs/pull/1234/head | merge commit sha> <name, e.g. pr-1234>
set -euo pipefail

run_ref="${1:?usage: checkout.sh <runRef> <name>}"
name="${2:?usage: checkout.sh <runRef> <name>}"

git_common_dir=$(git -C "$(dirname "$0")" rev-parse --path-format=absolute --git-common-dir)
main_checkout=$(dirname "$git_common_dir")
worktree="$main_checkout/.claude/worktrees/proof-$name"

git -C "$main_checkout" fetch --quiet origin "$run_ref"
commit=$(git -C "$main_checkout" rev-parse FETCH_HEAD)

if [ -d "$worktree" ]; then
  git -C "$worktree" checkout --quiet --detach "$commit"
else
  git -C "$main_checkout" worktree add --quiet --detach "$worktree" "$commit" >&2
fi

echo "Checked out $(git -C "$worktree" log -1 --format='%h %s' "$commit")" >&2
echo "$worktree"
