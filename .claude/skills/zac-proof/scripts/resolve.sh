#!/usr/bin/env bash
#
# SPDX-FileCopyrightText: 2026 INFO.nl
# SPDX-License-Identifier: EUPL-1.2+
#
# Resolves a PR number, PR URL, branch name or PZ ticket to the ZAC pull requests to run,
# including the stack each PR is part of. Prints one JSON document on stdout.
#
# Usage: bash resolve.sh <1234 | https://github.com/.../pull/1234 | feature/PZ-1234-x | PZ-1234>
set -euo pipefail

input="${1:?usage: resolve.sh <PR number | PR URL | branch | PZ-XXXX>}"
repo="infonl/dimpact-zaakafhandelcomponent"
pr_fields="number,title,state,url,headRefName,baseRefName,mergeCommit,mergedAt,isDraft"

jira_key=""
jira_json="null"
if [[ "$input" =~ ^[Pp][Zz]-[0-9]+$ ]]; then
  jira_key=$(echo "$input" | tr '[:lower:]' '[:upper:]')
fi

list_prs() {
  gh pr list --repo "$repo" --state all --limit 30 --json "$pr_fields" "$@"
}

view_jira() {
  acli jira workitem view "$1" --fields summary,status,description,subtasks,parent --json 2> /dev/null || echo null
}

# PRs that belong to a ticket: the key in the title or branch, or a "Solves <key>" line in the body.
# Release and hotfix PRs list many keys in their body, so they never match on the body alone.
prs_for_key() {
  local key="$1"
  gh pr list --repo "$repo" --state all --limit 30 --search "$key" --json "$pr_fields,body" |
    jq --arg key "$key" '
      ("\\b" + $key + "(?![0-9])") as $pattern
      | [.[] | select(
          ((.title + " " + .headRefName) | test($pattern; "i"))
          or (((.body // "") | test("(?i)solves[^\\n]*" + $pattern)) and (.headRefName | test("^(hotfix|release)/") | not))
        ) | del(.body)]'
}

if [[ "$input" =~ ^[0-9]+$ ]]; then
  candidates=$(gh pr view "$input" --repo "$repo" --json "$pr_fields" | jq -s .)
elif [[ "$input" =~ /pull/([0-9]+) ]]; then
  candidates=$(gh pr view "${BASH_REMATCH[1]}" --repo "$repo" --json "$pr_fields" | jq -s .)
elif [ -n "$jira_key" ]; then
  jira_json=$(view_jira "$jira_key")
  keys=$(echo "$jira_json" | jq -r --arg key "$jira_key" '[$key] + [(.fields.subtasks // [])[].key] | .[]' 2> /dev/null || echo "$jira_key")
  candidates=$(for key in $keys; do prs_for_key "$key"; done | jq -s 'add // [] | unique_by(.number)')
else
  candidates=$(list_prs --head "$input")
fi

if ! [[ "$input" =~ ^[0-9]+$ || "$input" =~ /pull/ ]]; then
  candidates=$(echo "$candidates" | jq '[.[] | select(.state != "CLOSED")]')
fi

if [ -z "$jira_key" ]; then
  jira_key=$(echo "$candidates" | jq -r '[.[] | (.title + " " + .headRefName) | capture("(?<key>PZ-[0-9]+)"; "i").key][0] // "" | ascii_upcase')
  if [ -n "$jira_key" ]; then
    jira_json=$(view_jira "$jira_key")
  fi
fi

# The stack around a PR: down via its base branch, up via open PRs based on its head branch.
# Prints {stack, ambiguousChildren}. When more than one open PR is based on a head branch, it stops going
# up there and lists those PRs in ambiguousChildren instead of picking one.
stack_for() {
  local number="$1" pr base head stack below above children ambiguous="[]"
  pr=$(gh pr view "$number" --repo "$repo" --json "$pr_fields")
  stack="[$pr]"
  base=$(echo "$pr" | jq -r .baseRefName)
  while [ "$base" != "main" ]; do
    below=$(list_prs --head "$base" | jq '.[0] // empty')
    [ -z "$below" ] && break
    stack=$(jq -n --argjson pr "$below" --argjson stack "$stack" '[$pr] + $stack')
    base=$(echo "$below" | jq -r .baseRefName)
  done
  head=$(echo "$pr" | jq -r .headRefName)
  while :; do
    children=$(list_prs --base "$head" | jq '[.[] | select(.state != "CLOSED")]')
    if [ "$(echo "$children" | jq length)" -gt 1 ]; then
      ambiguous=$(echo "$children" | jq '[.[] | {number, title, state, url, headRefName}]')
      break
    fi
    above=$(echo "$children" | jq '.[0] // empty')
    [ -z "$above" ] && break
    stack=$(jq -n --argjson pr "$above" --argjson stack "$stack" '$stack + [$pr]')
    head=$(echo "$above" | jq -r .headRefName)
  done
  jq -n --argjson stack "$stack" --argjson ambiguous "$ambiguous" '{stack: $stack, ambiguousChildren: $ambiguous}'
}

files_for() {
  gh pr view "$1" --repo "$repo" --json files --jq '[.files[].path]'
}

result_candidates="[]"
for number in $(echo "$candidates" | jq -r '.[].number'); do
  stack_result=$(stack_for "$number")
  stack=$(echo "$stack_result" | jq '.stack')
  ambiguous_children=$(echo "$stack_result" | jq '.ambiguousChildren')
  top_merge_commit=$(echo "$stack" | jq -r 'last | select(.state == "MERGED") | .mergeCommit.oid')
  on_main=null
  if [ -n "$top_merge_commit" ]; then
    status=$(gh api "repos/$repo/compare/main...$top_merge_commit" --jq .status 2> /dev/null || echo unknown)
    if [ "$status" = behind ] || [ "$status" = identical ]; then on_main=true; else on_main=false; fi
  fi
  files=$(for stacked in $(echo "$stack" | jq -r '.[].number'); do files_for "$stacked"; done | jq -s 'add | unique')
  result_candidates=$(jq -n --argjson all "$result_candidates" --argjson stack "$stack" --argjson ambiguous "$ambiguous_children" --argjson files "$files" --argjson onMain "$on_main" '
    ($stack | last) as $top
    | $all + [{
        stack: [$stack[] | {number, title, state, isDraft, url, headRefName, baseRefName, mergedAt}],
        runPr: $top.number,
        runRef: (if $top.state == "MERGED" then $top.mergeCommit.oid else "refs/pull/\($top.number)/head" end),
        runRefDescription: (if $top.state == "MERGED" then "merge commit of #\($top.number)" else "head of #\($top.number) (\($top.headRefName))" end),
        ambiguousChildren: $ambiguous,
        mergeCommitOnMain: $onMain,
        changedFiles: ($files | length),
        frontendOnly: ($files | all(test("^(src/main/app/|docs/|openspec/|\\.claude/|\\.github/|README)"))),
        backendFiles: ($files | map(select(test("^src/main/(kotlin|java|resources)/"))) | length),
        migrations: ($files | map(select(test("^src/main/resources/schemas/")))),
        integrationHints: ($files | map(select(test("^src/main/(kotlin|resources)/"))) as $backend | {
          openObjecten: ($backend | any(test("/client/or/|/productaanvraag/"))),
          openNotificaties: ($backend | any(test("/zac/notification/|/productaanvraag/"))),
          openFormulieren: ($backend | any(test("(?i)open-?formulieren")))
        }),
        files: $files
      }]')
done

jq -n --arg input "$input" --arg jiraKey "$jira_key" --argjson jira "$jira_json" --argjson candidates "$result_candidates" '
  {input: $input,
   jiraKey: (if $jiraKey == "" then null else $jiraKey end),
   jira: (if $jira == null then null else {
     key: $jira.key,
     summary: $jira.fields.summary,
     status: $jira.fields.status.name,
     parent: ($jira.fields.parent.key // null),
     description: ([$jira.fields.description | .. | objects | select(.type == "text") | .text] | join(" ")),
     subtasks: [($jira.fields.subtasks // [])[] | {key, summary: .fields.summary, status: .fields.status.name}]
   } end),
   candidates: ($candidates | unique_by(.stack | map(.number)))}'
