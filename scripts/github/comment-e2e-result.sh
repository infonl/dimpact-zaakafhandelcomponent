#!/usr/bin/env bash
#
# SPDX-FileCopyrightText: 2026 INFO.nl
# SPDX-License-Identifier: EUPL-1.2+
#
# Keeps one comment on a pull request with the result of the end-to-end tests on kind. A failure creates the comment
# or updates it; a later success updates an existing comment, so that a fixed failure does not stay on the pull request.
#
# Environment: GH_TOKEN, GITHUB_REPOSITORY, JOB_STATUS, PULL_REQUEST_NUMBER, HEAD_SHA, RUN_URL, ZAC_IMAGE_DESCRIPTION
# Reads the failed scenarios from the JSON report of Playwright in src/e2e/reports/results.json, when it exists.
set -euo pipefail

marker='<!-- e2e-tests-on-kind -->'
results_file="$(dirname "$0")/../../src/e2e/reports/results.json"

comment_id=$(gh api --paginate "repos/$GITHUB_REPOSITORY/issues/$PULL_REQUEST_NUMBER/comments" \
  --jq ".[] | select(.user.login == \"github-actions[bot]\" and (.body | startswith(\"$marker\"))) | .id" | head -n 1)

if [ "$JOB_STATUS" = success ]; then
  if [ -z "$comment_id" ]; then
    exit 0
  fi
  body="$marker
### :white_check_mark: End-to-end tests on kind pass

The end-to-end tests pass on $ZAC_IMAGE_DESCRIPTION at $HEAD_SHA. [Run]($RUN_URL)"
else
  failed_scenarios=""
  if [ -f "$results_file" ]; then
    failed_scenarios=$(jq -r '
      [.. | objects | select(has("specs")) | .specs[]
        | select(any(.tests[]; .status == "unexpected" or .status == "flaky"))
        | "- `\(.file):\(.line)` \(.title)"]
      | unique | .[]' "$results_file")
  fi
  if [ -z "$failed_scenarios" ]; then
    failed_scenarios="No scenario failed: the run stopped before or after the tests. See the log of the run."
  fi
  body="$marker
### :x: End-to-end tests on kind fail

The end-to-end tests fail on $ZAC_IMAGE_DESCRIPTION at $HEAD_SHA. This check does not block the merge yet.

$failed_scenarios

The run has the Playwright report with the traces, and on a failure the logs of the kind stack, as artifacts. [Run]($RUN_URL)"
fi

if [ -n "$comment_id" ]; then
  gh api --method PATCH "repos/$GITHUB_REPOSITORY/issues/comments/$comment_id" --raw-field body="$body" > /dev/null
else
  gh api --method POST "repos/$GITHUB_REPOSITORY/issues/$PULL_REQUEST_NUMBER/comments" --raw-field body="$body" > /dev/null
fi
