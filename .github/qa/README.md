# Claude QA (pilot)

Every weekday morning, Claude looks at the PRs merged into `main` the previous working day, clicks
through the changes on the dev environment with Playwright, and posts a short message to Slack, with
a "Read more" button that opens the full report.

```
gather.sh ──► Claude Code (Haiku) ──► post-summary.sh ──► job summary ("Read more")
GitHub, Jira  Playwright on dev env   post-slack.sh   ──► Slack message
```

| File | What |
|---|---|
| `.github/workflows/claude-qa.yml` | The workflow: weekdays at 05:30 UTC, or by hand with a scope. |
| `.github/qa/gather.sh` | Collects the merged PRs, their diffs and Jira tickets into `qa-context/`. Skips Renovate updates and PRs without application code, so they cost nothing. |
| `.github/qa/QA_INSTRUCTIONS.md` | What the agent does and the rules it follows on the shared dev environment. |
| `.github/qa/post-summary.sh` | Writes the report, the cost of the run and the skipped PRs to the job summary. |
| `.github/qa/post-slack.sh` | Posts the message of the agent to Slack. A crashed run posts a warning instead. |
| `src/e2e/claude-qa/` | Playwright config, `login` and `createZaak` helpers. Tests the agent writes go to `tests/` and end up in the run artifact, not in git. |

## Setup

The workflow runs in the existing `azure-dimpact-dta` environment, so it reuses what the nightly e2e
run already has: `ZAC_URL`, the Azure credentials for the firewall rule, and the test users
`ZAC_E2E_TEST_USER_1_*` (Bob) and `ZAC_E2E_TEST_USER_2_*` (Oscar).

Add these secrets to the `azure-dimpact-dta` environment, or as repository secrets:

| Secret | Required | How to get it |
|---|---|---|
| `ANTHROPIC_API_KEY` | yes | platform.claude.com, Settings, API keys. Create a separate workspace "zac-claude-qa" with a monthly spend limit (e.g. $20) and create the key in that workspace. |
| `CLAUDE_QA_SLACK_WEBHOOK_URL` | yes | api.slack.com/apps, Create New App, From scratch, name it e.g. "QA buddy", Incoming Webhooks on, Add New Webhook, pick the channel. Without it, nothing is posted. |
| `CLAUDE_QA_JIRA_EMAIL` | no | The Atlassian account the token belongs to. |
| `CLAUDE_QA_JIRA_API_TOKEN` | no | id.atlassian.com/manage-profile/security/api-tokens. A read-only token is enough; without it, the agent tests from the PR descriptions only. |

Optional repository variables:

| Variable | Default | What |
|---|---|---|
| `CLAUDE_QA_SCOPE` | `yesterday` | Scope of the scheduled run. |
| `CLAUDE_QA_MODEL` | `claude-haiku-5-5` | Model of the scheduled run. |
| `CLAUDE_QA_MAX_BUDGET_USD` | `2` | Hard cost limit per run; Claude Code stops when it is reached. |
| `CLAUDE_QA_LANGUAGE` | `English` | Language of the Slack message, e.g. `Dutch`. |

## Running it

- **Scheduled**: weekdays at 05:30 UTC, scope `yesterday` (on Monday: Friday to Sunday). Days without
  testable PRs post nothing.
- **By hand**: Actions, Claude QA, Run workflow. Pick a scope (`yesterday`, `7d`, `since:2026-10-01`),
  a model, a budget, and whether to post to Slack. The first run is best done with Slack off.
- **Weekly instead of daily**: change the cron to e.g. `"30 5 * * 1"` and set `CLAUDE_QA_SCOPE` to `7d`.
  `gather.sh` hands the agent at most 15 PRs per run (`MAX_PRS`), newest first.

`workflow_dispatch` only shows up once the workflow is on `main`.

## Cost

Haiku 5.5 costs $0.10 per million input tokens and $0.50 per million output tokens. A run on one busy
day (15 PRs) costs well under the default $2 limit; the job summary shows the exact cost of every run.
Pick `claude-sonnet-5-5` ($2 / $10) by hand when Haiku's tests are not good enough.

## Safety

- The agent only gets read access to the repo, write access to `src/e2e/claude-qa/tests/` and
  `qa-output/`, and one shell command: running its Playwright tests. Everything else is refused.
- Jira and GitHub credentials never reach the agent; `gather.sh` fetches everything up front.
- The test users are normal employees. The instructions forbid saving on admin pages, real e-mail
  addresses and BSNs other than the test person's, and changing zaken the agent did not create.
- The tests the agent writes run with the test user passwords in their environment; Claude Code
  scrubs the Anthropic key from it on a best-effort basis (`CLAUDE_CODE_SUBPROCESS_ENV_SCRUB`). PR descriptions and Jira tickets reach the agent as input, so this pilot trusts
  the people who write them.
