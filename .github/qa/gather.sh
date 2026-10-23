#!/usr/bin/env bash
#
# SPDX-FileCopyrightText: 2026 INFO.nl
# SPDX-License-Identifier: EUPL-1.2+
#
# Collects the merged PRs of a window, their trimmed diffs and Jira tickets into qa-context/.
# Runs before the agent, without AI, so the agent never needs GitHub or Jira credentials.
#
# Env:
#   SCOPE              yesterday | <N>d | since:YYYY-MM-DD   (default: yesterday)
#                      "yesterday" is the previous working day, so on Monday it covers Friday to Sunday.
#   GH_TOKEN           token for the gh CLI
#   JIRA_BASE_URL      optional, e.g. https://dimpact.atlassian.net; without it no tickets are fetched
#   JIRA_EMAIL         optional, Jira Cloud user
#   JIRA_API_TOKEN     optional, Jira Cloud API token
#   JIRA_PROJECT_KEYS  comma separated (default: PZ)
#   MAX_PRS            maximum number of PRs handed to the agent (default: 15)
#   MAX_DIFF_LINES     maximum lines per trimmed diff (default: 1500)
#   OUT_DIR            output directory (default: qa-context)

set -euo pipefail

SCOPE="${SCOPE:-yesterday}"
OUT="${OUT_DIR:-qa-context}"
MAX_PRS="${MAX_PRS:-15}"
MAX_DIFF_LINES="${MAX_DIFF_LINES:-1500}"
JIRA_PROJECT_KEYS="${JIRA_PROJECT_KEYS:-PZ}"

rm -rf "$OUT"
mkdir -p "$OUT/diffs" "$OUT/jira"

# GNU date on the runner, gdate on macOS for local runs.
DATE="date"
command -v gdate > /dev/null && DATE="gdate"

case "$SCOPE" in
  yesterday)
    UNTIL=$($DATE -u -d yesterday +%F)
    if [[ $($DATE -u +%u) == 1 ]]; then SINCE=$($DATE -u -d '3 days ago' +%F); else SINCE=$UNTIL; fi
    ;;
  since:*) SINCE="${SCOPE#since:}"; UNTIL=$($DATE -u +%F) ;;
  *d) SINCE=$($DATE -u -d "${SCOPE%d} days ago" +%F); UNTIL=$($DATE -u +%F) ;;
  *) echo "Unknown SCOPE '$SCOPE' (use yesterday | <N>d | since:YYYY-MM-DD)" >&2; exit 1 ;;
esac
echo "Collecting PRs merged into main from $SINCE to $UNTIL (scope: $SCOPE)"

gh pr list --state merged --base main --search "merged:$SINCE..$UNTIL" --limit 300 \
  --json number,title,body,headRefName,mergedAt,mergeCommit,author,url,files \
  > "$OUT/all-prs.json"

# A PR is worth testing in the browser when it changes application code and is not a dependency bump.
jq '
  def app_code: test("^src/main/") and (test("^src/main/resources/api-specs/") | not);
  def skip_reason:
    if .author.login == "app/renovate" or (.title | test("^(chore|fix)\\(deps\\)")) then "dependency update"
    elif ([.files[].path | select(app_code)] | length) == 0 then "no application code changed"
    else null end;
  map(. + {skip_reason: skip_reason})
  | sort_by(.mergedAt) | reverse
' "$OUT/all-prs.json" > "$OUT/classified.json"

jq --argjson max "$MAX_PRS" '[.[] | select(.skip_reason == null)][:$max] | map(del(.skip_reason))' \
  "$OUT/classified.json" > "$OUT/prs.json"
jq --argjson max "$MAX_PRS" '
  ([.[] | select(.skip_reason == null)][$max:] | map(.skip_reason = "over the MAX_PRS limit"))
  + [.[] | select(.skip_reason != null)]
  | map({number, title, url, skip_reason})
' "$OUT/classified.json" > "$OUT/skipped.json"
rm "$OUT/all-prs.json" "$OUT/classified.json"

PR_COUNT=$(jq length "$OUT/prs.json")
SKIPPED_COUNT=$(jq length "$OUT/skipped.json")
echo "Testable PRs: $PR_COUNT, skipped: $SKIPPED_COUNT"
if [[ -n "${GITHUB_OUTPUT:-}" ]]; then
  {
    echo "pr_count=$PR_COUNT"
    echo "since=$SINCE"
    echo "until=$UNTIL"
  } >> "$GITHUB_OUTPUT"
fi

KEY_RE="($(tr -d ' ' <<< "$JIRA_PROJECT_KEYS" | tr ',' '|'))-[0-9]+"
: > "$OUT/pr-keys.tsv"
for number in $(jq -r '.[].number' "$OUT/prs.json"); do
  text=$(jq -r --argjson n "$number" '.[] | select(.number == $n) | "\(.title)\n\(.headRefName)\n\(.body // "")"' "$OUT/prs.json")
  keys=$(grep -oE "$KEY_RE" <<< "$text" | sort -u | paste -sd, - || true)
  printf '%s\t%s\n' "$number" "$keys" >> "$OUT/pr-keys.tsv"

  # The local diff has no size limit, unlike the GitHub API. Lock files, generated code and
  # snapshots cost tokens and tell the agent nothing about behaviour.
  merge_commit=$(jq -r --argjson n "$number" '.[] | select(.number == $n) | .mergeCommit.oid' "$OUT/prs.json")
  if git cat-file -e "$merge_commit" 2> /dev/null; then
    # Without pipefail, because head closing the pipe early on a long diff is not an error.
    (
      set +o pipefail
      git show --format= --no-color "$merge_commit" -- . \
        ':(exclude)**/package-lock.json' ':(exclude)**/generated/**' ':(exclude)**/*.snap' ':(exclude)**/*.svg' \
        | head -n "$MAX_DIFF_LINES" > "$OUT/diffs/$number.diff"
    )
  else
    echo "warn: merge commit of #$number not found, is the checkout shallow?" >&2
  fi
done

jira_issue() {
  curl -sS --fail-with-body -u "$JIRA_EMAIL:$JIRA_API_TOKEN" -H "Accept: application/json" \
    "$JIRA_BASE_URL/rest/api/3/issue/$1?expand=renderedFields&fields=summary,status,issuetype,description,comment,parent"
}

# Rendered HTML reduced to plain text: the agent needs the words, not the markup.
JQ_TEXT='def text: (. // "") | gsub("<br ?/?>|</p>|</li>|</h[1-6]>"; "\n") | gsub("<li>"; "- ") | gsub("<[^>]+>"; "")
  | gsub("&nbsp;"; " ") | gsub("&amp;"; "&") | gsub("&lt;"; "<") | gsub("&gt;"; ">") | gsub("&quot;"; "\"")
  | gsub("&#39;"; "'"'"'") | gsub("\n{3,}"; "\n\n");'

if [[ -n "${JIRA_BASE_URL:-}" && -n "${JIRA_EMAIL:-}" && -n "${JIRA_API_TOKEN:-}" ]]; then
  for key in $(cut -f2 "$OUT/pr-keys.tsv" | tr ',' '\n' | grep -v '^$' | sort -u); do
    if ! raw=$(jira_issue "$key"); then
      echo "warn: could not fetch Jira issue $key" >&2
      continue
    fi
    # Our sub-tasks ("Oplossen", "Testen") are usually empty; the intent lives on the parent story.
    parent_description=""
    if [[ $(jq -r '.fields.issuetype.subtask' <<< "$raw") == "true" ]]; then
      parent_key=$(jq -r '.fields.parent.key' <<< "$raw")
      parent_description=$(jira_issue "$parent_key" | jq -r "$JQ_TEXT .renderedFields.description | text" || true)
    fi
    jq --arg base "$JIRA_BASE_URL" --arg parent_description "$parent_description" "$JQ_TEXT"'
      {
        key,
        url: "\($base)/browse/\(.key)",
        summary: .fields.summary,
        type: .fields.issuetype.name,
        status: .fields.status.name,
        parent: (.fields.parent | if . then "\(.key): \(.fields.summary)" else null end),
        parent_description: (if $parent_description != "" then $parent_description else null end),
        description: (.renderedFields.description | text),
        last_comments: [.renderedFields.comment.comments[-5:][]? | .body | text]
      }' <<< "$raw" > "$OUT/jira/$key.json"
  done
else
  echo "No Jira credentials, testing from the PR descriptions and diffs only."
fi

{
  echo "# QA context"
  echo
  echo "- Scope: \`$SCOPE\`, PRs merged into \`main\` from $SINCE to $UNTIL"
  echo "- PRs to test: $PR_COUNT (the $SKIPPED_COUNT others are listed in skipped.json and need no testing)"
  echo "- Jira tickets fetched: $(find "$OUT/jira" -name '*.json' | wc -l | tr -d ' ')"
  echo
  echo "| PR | Title | Jira | Diff |"
  echo "|----|-------|------|------|"
  while IFS=$'\t' read -r number keys; do
    title=$(jq -r --argjson n "$number" '.[] | select(.number == $n) | .title | gsub("\\|"; "/")' "$OUT/prs.json")
    echo "| #$number | $title | ${keys:-none} | diffs/$number.diff |"
  done < "$OUT/pr-keys.tsv"
} > "$OUT/manifest.md"

echo "Context written to $OUT/"
