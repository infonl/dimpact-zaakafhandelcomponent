#!/usr/bin/env bash
#
# SPDX-FileCopyrightText: 2026 INFO.nl
# SPDX-License-Identifier: EUPL-1.2+
#
# Posts the teammate-style message of the agent to Slack, with a "Read more" button to the job summary.
# A run without results still posts a short warning, so a broken run never looks like a quiet day.
#
# Env:
#   SLACK_WEBHOOK_URL   incoming webhook; without it, nothing is posted
#   SLACK_DRY_RUN=true  print the payload instead of posting it
#   JIRA_BASE_URL       to turn ticket keys into links (default: https://dimpact.atlassian.net)
#   RESULTS             results file (default: qa-output/results.json)
#   GITHUB_SERVER_URL, GITHUB_REPOSITORY, GITHUB_RUN_ID (set by Actions)

set -euo pipefail

RESULTS="${RESULTS:-qa-output/results.json}"
RUN_URL="${GITHUB_SERVER_URL:-https://github.com}/${GITHUB_REPOSITORY:-}/actions/runs/${GITHUB_RUN_ID:-}"

if [[ -z "${SLACK_WEBHOOK_URL:-}" && "${SLACK_DRY_RUN:-false}" != "true" ]]; then
  echo "SLACK_WEBHOOK_URL is not set, skipping Slack."
  exit 0
fi

if [[ -f "$RESULTS" ]] && jq -e '.slack_message | type == "string" and length > 0' "$RESULTS" > /dev/null 2>&1; then
  payload=$(jq --arg run "$RUN_URL" --arg jira "${JIRA_BASE_URL:-https://dimpact.atlassian.net}" '
    def escape: gsub("&"; "&amp;") | gsub("<"; "&lt;") | gsub(">"; "&gt;");
    def link_tickets: gsub("(?<key>\\bPZ-[0-9]+\\b)"; "<\($jira)/browse/\(.key)|\(.key)>");
    def count($verdict): [.tickets[]? | select(.verdict == $verdict)] | length;
    def plural($n; $word): "\($n) \($word)\(if $n == 1 then "" else "s" end)";

    (.slack_message | escape) as $message
    | ([.tickets[]? | .prs[]?] | unique | length) as $changes
    | {
        text: $message,
        blocks: [
          { type: "section", text: { type: "mrkdwn", text: ($message | link_tickets) } },
          { type: "context", elements: [{ type: "mrkdwn", text: (
              [ "\(plural($changes; "change")) checked",
                (count("pass") | select(. > 0) | ":white_check_mark: \(.) fine"),
                (count("fail") | select(. > 0) | ":beetle: \(plural(.; "bug"))"),
                (count("needs-human") | select(. > 0) | ":eyes: \(.) to look at")
              ] | join("  ·  ")) }] },
          { type: "actions", elements: [
              { type: "button", text: { type: "plain_text", text: "Read more" }, url: $run }
          ] }
        ]
      }' "$RESULTS")
else
  payload=$(jq -n --arg run "$RUN_URL" '{
    text: "My QA run did not finish today.",
    blocks: [
      { type: "section", text: { type: "mrkdwn",
          text: ":warning: My QA run did not finish today, so I have nothing to report. Something went wrong on my side, the log has the details." } },
      { type: "actions", elements: [
          { type: "button", text: { type: "plain_text", text: "Open the log" }, url: $run }
      ] }
    ]}')
fi

if [[ "${SLACK_DRY_RUN:-false}" == "true" ]]; then
  echo "$payload"
  exit 0
fi

if curl -sS --fail-with-body -X POST -H "Content-Type: application/json" --data "$payload" "$SLACK_WEBHOOK_URL" > /dev/null; then
  echo "Posted to Slack."
else
  echo "warn: posting to Slack failed" >&2
fi
