#!/usr/bin/env bash
#
# SPDX-FileCopyrightText: 2026 INFO.nl
# SPDX-License-Identifier: EUPL-1.2+
#
# Writes the "Read more" page: the report of the agent plus the cost of the run and the skipped PRs,
# to the GitHub job summary (or to stdout when run locally).
#
# Env: SCOPE, MODEL, GITHUB_STEP_SUMMARY (set by Actions)

set -euo pipefail

REPORT=qa-output/report.md
USAGE=qa-output/copilot-usage.json
SKIPPED=qa-context/skipped.json
SUMMARY="${GITHUB_STEP_SUMMARY:-/dev/stdout}"

{
  if [[ -f "$REPORT" ]]; then
    cat "$REPORT"
  else
    echo "## No QA report"
    echo
    echo "The agent did not write a report. It may have run out of turns or budget; see the run log."
  fi
  echo
  echo "---"
  echo
  echo "### Run details"
  echo
  echo "- Scope: \`${SCOPE:-yesterday}\`"
  echo "- Model: \`${MODEL:-unknown}\`"
  # The format of the Copilot CLI usage file is not documented, so show its top-level values as they are.
  if [[ -f "$USAGE" ]]; then
    jq -r 'to_entries[] | select(.value | type != "object" and type != "array") | "- \(.key): \(.value)"' "$USAGE" \
      || true
  fi
  if [[ -f "$SKIPPED" ]] && [[ $(jq length "$SKIPPED") -gt 0 ]]; then
    echo
    echo "<details><summary>$(jq length "$SKIPPED") PRs not tested</summary>"
    echo
    jq -r '.[] | "- [#\(.number)](\(.url)) \(.title): \(.skip_reason)"' "$SKIPPED"
    echo
    echo "</details>"
  fi
} >> "$SUMMARY"
