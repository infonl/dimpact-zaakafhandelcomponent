#!/usr/bin/env bash
#
# SPDX-FileCopyrightText: 2026 INFO.nl
# SPDX-License-Identifier: EUPL-1.2+
#
# Runs <evidence dir>/walkthrough.spec.ts with the harness of this skill and the Playwright install of src/e2e.
# Leaves screenshots/, steps.jsonl, failed-requests.log, run.json and walkthrough.webm in the evidence dir.
# With --explore it runs explore.spec.ts instead and leaves the evidence of the last walkthrough alone;
# its dumpPage output goes to .explore/.
#
# Usage: bash run-walkthrough.sh <evidence dir> [--headed] [--explore]
# Env:   ZAC_URL (default: the ZAC port of the kind stack, else http://localhost:8080)
#        ZAC_LOGIN_URL (frontend dev server only: the ZAC to sign in on, e.g. http://localhost:8080)
set -uo pipefail

evidence_dir="$(cd "${1:?usage: run-walkthrough.sh <evidence dir> [--headed] [--explore]}" && pwd)"
headed=0
spec=walkthrough.spec.ts
for option in "${@:2}"; do
  case $option in
    --headed) headed=1 ;;
    --explore) spec=explore.spec.ts ;;
    *) echo "Unknown option: $option" >&2; exit 2 ;;
  esac
done

skill_dir="$(cd "$(dirname "$0")/.." && pwd)"
git_common_dir=$(git -C "$skill_dir" rev-parse --path-format=absolute --git-common-dir)
main_checkout=$(dirname "$git_common_dir")
e2e_dir="$main_checkout/src/e2e"

if [ ! -f "$evidence_dir/$spec" ]; then
  echo "No $spec in $evidence_dir" >&2
  exit 2
fi

export NVM_DIR="${NVM_DIR:-$HOME/.nvm}"
# shellcheck disable=SC1091
[ -s "$NVM_DIR/nvm.sh" ] && . "$NVM_DIR/nvm.sh" && nvm use --silent "$(cat "$main_checkout/.nvmrc")" > /dev/null

if [ ! -d "$e2e_dir/node_modules/@playwright/test" ]; then
  (cd "$e2e_dir" && npm ci --ignore-scripts --silent)
fi
(cd "$e2e_dir" && npx --no-install playwright install chromium > /dev/null)

if [ -z "${ZAC_URL:-}" ]; then
  zac_port=$(sed -n 's/^HOST_PORT_ZAC=//p' "$main_checkout"/build/kind/*/ports.env "$main_checkout"/.claude/worktrees/proof-*/build/kind/*/ports.env 2> /dev/null | head -1)
  export ZAC_URL="http://localhost:${zac_port:-8080}"
fi

cp "$skill_dir/harness/playwright.config.ts" "$skill_dir/harness/proof.ts" "$evidence_dir/"
ln -sfn "$e2e_dir/node_modules" "$evidence_dir/node_modules"
mode=$([ $headed = 1 ] && echo headed || echo headless)
echo "Running $spec against $ZAC_URL ($mode) ..."

if [ "$spec" = explore.spec.ts ]; then
  rm -rf "$evidence_dir/.explore"
  (cd "$evidence_dir" && SPEC=$spec HEADED=$headed npx --no-install playwright test --config playwright.config.ts --output .explore/test-results)
  exit_code=$?
  echo
  echo "Snapshots in $evidence_dir/.explore:"
  ls "$evidence_dir/.explore" | grep -v test-results | sed 's/^/  /'
  exit $exit_code
fi

rm -rf "$evidence_dir/screenshots" "$evidence_dir/steps.jsonl" "$evidence_dir/failed-requests.log" \
  "$evidence_dir/test-results" "$evidence_dir/walkthrough.webm"
jq -n --arg zacUrl "$ZAC_URL" --arg loginUrl "${ZAC_LOGIN_URL:-}" --arg mode "$mode" --arg date "$(date '+%Y-%m-%d %H:%M')" \
  '{zacUrl: $zacUrl, loginUrl: (if $loginUrl == "" then null else $loginUrl end), mode: $mode, date: $date}' \
  > "$evidence_dir/run.json"
(cd "$evidence_dir" && SPEC=$spec HEADED=$headed npx --no-install playwright test --config playwright.config.ts)
exit_code=$?

video=$(find "$evidence_dir/test-results" -name '*.webm' -print -quit 2> /dev/null)
[ -n "$video" ] && cp "$video" "$evidence_dir/walkthrough.webm"

echo
echo "Steps:"
if [ -f "$evidence_dir/steps.jsonl" ]; then
  jq -r '"  \(.step). [\(.status)] \(.title)\(if .error then " — " + .error else "" end)\(if (.failedRequests | length) > 0 then "\n     failed requests: " + (.failedRequests | join(", ")) else "" end)"' \
    "$evidence_dir/steps.jsonl"
fi
echo "Video: ${video:+$evidence_dir/walkthrough.webm}"
exit $exit_code
