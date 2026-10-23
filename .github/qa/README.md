# QA Agent (pilot)

Every weekday morning, an AI agent looks at the PRs merged into `main` the previous working day,
clicks through the changes on the dev environment with Playwright, and posts a short message to Slack,
with a "Read more" button that opens the full report. It runs on GitHub Copilot CLI with Claude Haiku,
billed to our GitHub organization.

```
gather.sh ──► Copilot CLI (Claude Haiku) ──► post-summary.sh ──► job summary ("Read more")
GitHub, Jira  Playwright on dev env          post-slack.sh   ──► Slack message
```

| File | What |
|---|---|
| `.github/workflows/qa-agent.yml` | The workflow: weekdays at 05:30 UTC, or by hand with a scope. |
| `.github/qa/gather.sh` | Collects the merged PRs, their diffs and Jira tickets into `qa-context/`. Skips Renovate updates and PRs without application code, so they cost nothing. |
| `.github/qa/QA_INSTRUCTIONS.md` | What the agent does and the rules it follows on the shared dev environment. |
| `.github/qa/post-summary.sh` | Writes the report, the usage of the run and the skipped PRs to the job summary. |
| `.github/qa/post-slack.sh` | Posts the message of the agent to Slack. A crashed run posts a warning instead. |
| `src/e2e/qa-agent/` | Playwright config, `login` and `createZaak` helpers. Tests the agent writes go to `tests/` and end up in the run artifact, not in git. |

## Setup

The workflow runs in the existing `azure-dimpact-dta` environment, so it reuses what the nightly e2e
run already has: `ZAC_URL`, the Azure credentials for the firewall rule, and the test users
`ZAC_E2E_TEST_USER_1_*` (Bob) and `ZAC_E2E_TEST_USER_2_*` (Oscar).

1. **Copilot billing**: an organization owner checks, in the organization settings under Copilot,
   Policies, that "Copilot CLI" is enabled and "Allow use of Copilot CLI billed to the organization" is
   selected. The workflow then uses its own token; no API key is needed.
2. **Slack**: api.slack.com/apps, Create New App, From scratch, name it e.g. "QA Agent", turn on
   Incoming Webhooks, Add New Webhook, pick the channel. Save the URL as the secret
   `QA_AGENT_SLACK_WEBHOOK_URL`. Without it, nothing is posted.
3. **Jira (optional)**: create a token at id.atlassian.com/manage-profile/security/api-tokens and save
   it as `QA_AGENT_JIRA_API_TOKEN`, with the account's e-mail address as `QA_AGENT_JIRA_EMAIL`. Without
   them, the agent tests from the PR descriptions only.

Put the secrets on the `azure-dimpact-dta` environment, or make them repository secrets.

Optional repository variables:

| Variable | Default | What |
|---|---|---|
| `QA_AGENT_SCOPE` | `yesterday` | Scope of the scheduled run. |
| `QA_AGENT_MODEL` | `claude-haiku-5.5` | Copilot model of the scheduled run. |
| `QA_AGENT_MAX_AI_CREDITS` | `500` | Safety limit per run in AI credits (100 credits = 1 USD); a normal run uses a few. |
| `QA_AGENT_LANGUAGE` | `English` | Language of the Slack message, e.g. `Dutch`. |

## Running it

- **Scheduled**: weekdays at 05:30 UTC, scope `yesterday` (on Monday: Friday to Sunday). Days without
  testable PRs post nothing.
- **By hand**: Actions, QA Agent, Run workflow. Pick a scope (`yesterday`, `7d`, `since:2026-10-01`),
  a model, a credit limit, and whether to post to Slack. The first run is best done with Slack off.
- **Weekly instead of daily**: change the cron to e.g. `"30 5 * * 1"` and set `QA_AGENT_SCOPE` to `7d`.
  `gather.sh` hands the agent at most 15 PRs per run (`MAX_PRS`), newest first.

`workflow_dispatch` only shows up once the workflow is on `main`.

## Cost

Through Copilot, Claude Haiku 5.5 costs $0.10 per million input tokens and $0.50 per million output
tokens, the same as at Anthropic. A trial on one busy day (15 PRs) used about $0.01 of model time,
without browser tests. The run stops at the credit limit; the limit is a soft cap, so the last model
call can go slightly over it. The job summary shows the usage the CLI reports. Pick `claude-sonnet-5.5`
($2 / $10) by hand when Haiku's tests are not good enough.

## Safety

- The agent can read the repo, create and edit files, and run one kind of shell command: `npm run`,
  for its Playwright tests. Other commands, URLs and the built-in GitHub MCP server are off.
  Copilot CLI cannot limit file writes to a folder, so the Slack webhook is only used in a separate
  report job, with a fresh checkout that reads nothing but the report data. The agent job cannot push.
- Jira credentials never reach the agent; `gather.sh` fetches everything up front. The workflow token
  is read-only and redacted from the output by Copilot CLI.
- The test users are normal employees. The instructions forbid saving on admin pages, real e-mail
  addresses and BSNs other than the test person's, and changing zaken the agent did not create.
- The tests the agent writes run with the test user passwords in their environment. PR descriptions and
  Jira tickets reach the agent as input, so this pilot trusts the people who write them.
