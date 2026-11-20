---
name: zac-proof
description: Use when the user wants to run, try out, test or demo a ZAC pull request, PR stack, branch or PZ ticket locally in the kind stack, wants a test guide for it (a localhost link, which user, which zaak, what to check), or wants proof (screenshots, video, a Playwright walkthrough) that a change works, e.g. "/zac-proof 7977", "/zac-proof PZ-1234 --guide", "run PZ-1234 locally", "how do I test this PR", "test this PR of a teammate", "make proof for my agent's PR".
---

# zac-proof

Runs a change in the local kind stack and writes a test guide: the localhost link, the user to sign in with,
the zaak to use and what to check. Then it runs a throwaway Playwright walkthrough and leaves screenshots, a
video and a paste-ready PR description section in a local folder. The user pastes the evidence into the PR
themselves: this skill never uploads files and never edits a PR, an issue or a comment.

Arguments: `<PR number | PR URL | branch | PZ-XXXX> [--guide] [--headed]`.
- `--guide`: stop after the test guide (step 4); the user tests by hand. Offer the walkthrough afterwards.
- `--headed`: show the browser during the walkthrough. Headless is the default.

`<skill dir>` below is the directory of this file. Call every script as its own plain command,
`bash <skill dir>/scripts/<name>.sh ...`, without `cd` or `&&` around it. Scratch files go in
`$CLAUDE_JOB_DIR/tmp` when that is set, otherwise in `/tmp`.

## 1. Find the change

```bash
bash <skill dir>/scripts/resolve.sh <input> > <scratch>/resolve.json
```

A PR number takes seconds; a ticket search covers the story and all its subtasks and takes about a minute.

The JSON:
- `jira`: `key`, `summary`, `status`, `parent`, `description` (plain text), `subtasks` (`key`, `summary`, `status`).
  When `parent` is set, the acceptance criteria are usually on the parent story:
  `acli jira workitem view <parent> --fields summary,description --json`.
- `candidates`: one per stack (a single PR is a stack of one), each with
  - `stack`: the PRs bottom to top, with `number`, `title`, `state`, `url`, `headRefName`, `mergedAt`
  - `runPr`, `runRef` (open: `refs/pull/<top>/head`, merged: the top merge commit) and `runRefDescription`
  - `mergeCommitOnMain` (merged stacks only)
  - `frontendOnly`, `backendFiles`, `migrations` (Flyway files), `integrationHints`, `files`

Pick the candidate:
- One candidate: use it.
- Several (e.g. one PR per subtask): ask which one, listing the PR titles. When all are merged and
  `mergeCommitOnMain` is true, offer the most recent merge commit: it contains all of them.
- None: say so and stop.
- `mergeCommitOnMain: false`: the merged stack never reached `main`; tell the user before running it.

The run name is `pr-<runPr>` of the chosen candidate. It names the worktree and the evidence folder
`${ZAC_PROOF_DIR:-~/zac-proof}/<run name>/`.

## 2. Check out the code

```bash
bash <skill dir>/scripts/checkout.sh <runRef> <run name>   # prints the worktree path
```

A detached worktree under `<main checkout>/.claude/worktrees/proof-<run name>`; the user's checkout stays untouched.

## 3. Run it

First see what runs now: `kubectl --context kind-zac -n zac get deploy zac -o jsonpath='{.spec.template.spec.containers[0].image}'`
and `curl -s -o /dev/null -w '%{http_code}' http://localhost:8080/` (302 = up).

**`frontendOnly: true`**: no image swap. From the worktree, in the background:

```bash
./gradlew generateOpenApiSpec
cd src/main/app && npm ci --ignore-scripts && npm run dev   # serves localhost:4200, proxies /rest to 8080
```

Use Node from `.nvmrc` (`nvm use`). It needs a running ZAC on 8080; when there is none, start the stack with
`./start-kind.sh -z` from the main checkout first.

**Otherwise**: ask the user first, naming the image that runs now. The swap replaces their running ZAC
and runs this branch's Flyway migrations on their local database; list `migrations` when there are any,
and suggest `-d` (fresh seed data) when the migrations would leave the database ahead of their own branch.
Then, from the worktree, in the background:

```bash
./start-kind.sh -b [-o] [-n] [-f]   # true integrationHints: openObjecten -o, openNotificaties -n, openFormulieren -f
kubectl --context kind-zac -n zac rollout status deploy/zac --timeout=20m
```

The build takes several minutes. `start-kind.sh` reads secrets with the 1Password CLI. When `op` is not
signed in (background jobs usually are not), stop and give the user the command to run themselves:
`! cd <worktree> && ./start-kind.sh -b ...`.
When 8080 returns WildFly's plain 404 after the rollout, ZAC failed to deploy: read
`kubectl --context kind-zac -n zac logs deploy/zac -c zaakafhandelcomponent | grep -E 'WFLYCTL0186|ERROR' | head`.

## 4. Write the test guide

From the acceptance criteria, the OpenSpec scenarios, the PR description and the diff, write a guide a person
can follow without reading the code. Save it as `manual-test-steps.md` in the evidence folder and show it in chat.

**Where.** The exact URL to open: `http://localhost:8080` for an image built from the worktree,
`http://localhost:4200` for the frontend dev server. Link straight to the page under test, e.g.
`http://localhost:8080/zaken/ZAAK-2026-0000000012`, not to the dashboard.

**Who.** Per step, the seed user to sign in with (password = username): `beheerder1`, `behandelaar1`,
`coordinator1`, `recordmanager1`, `raadpleger1`. Pick the user the ticket is about, and one without the right
when the change is about rights. Use a private window per user, or sign out in between.

**Which data.** A concrete zaak, task or document, never "open a zaak":
- Look for one that fits in the stack first (the werkvoorraad, the search) and name its identificatie.
- None fits: create one, from the worktree:
  `python3 scripts/test-data/create-zaak.py --zaaktype "Test zaaktype 1" --doc-count 0` (BPMN flows:
  `"BPMN test zaaktype 1"`; `--doc-count N` adds documents). It prints the links to the new zaak. Make sure the
  user of the guide can see it (group, zaakspecifieke autorisatie); assign it in the guide's setup step if needed.
- State needed beyond that (a task in a certain status, a besluit, a linked zaak): a setup step that creates it
  through the UI, with the user and the clicks.

**What to check.** Numbered steps: the action, then what the person should see, in the Dutch UI words (look
them up in `src/main/app/src/assets/i18n/nl.json`). One step per acceptance criterion at least, plus the unhappy
path that matters most (the wrong user, an empty field). End each step with a clear pass condition.

```markdown
Open http://localhost:8080/zaken/ZAAK-2026-0000000012 as `behandelaar1` (password `behandelaar1`).

1. Click "Zaak koppelen" in the side menu. → The dialog "Zaak koppelen" opens. (AC1)
2. ...

Regression check: the zaak page, its tasks and documents still load without an error message.
```

When the change has no visible effect (a refactor, a different HTTP client, caching), the steps check that the
screens it touches still work: the page loads, the data shows, no error dialog appears and no `/rest` request
fails. Say in the guide that this is a regression check, not proof of new behaviour.

With `--guide`, stop here: give the guide, the cleanup commands of step 7, and offer the walkthrough.

## 5. Write and run the walkthrough

Environment for every run in this step:
- kind (ZAC built from the worktree): nothing to set.
- `frontendOnly`: `ZAC_URL=http://localhost:4200 ZAC_LOGIN_URL=http://localhost:8080`. Without them the run
  tests the frontend that is in the image on 8080, not the PR.

**Explore first. Selectors come from the page, never from memory.** Write `explore.spec.ts` in the evidence folder:
sign in and visit each page the change touches, with `dumpPage(page, "<name>")` on each. Run it:

```bash
ZAC_URL=... bash <skill dir>/scripts/run-walkthrough.sh <evidence folder> --explore
```

Read `.explore/<name>.yaml` (the accessibility tree) and `.explore/<name>.failed-requests`. Use `getByRole` with
the exact accessible name; the UI labels are Dutch. Explore again whenever a page is unknown.

**Then the walkthrough.** Write `walkthrough.spec.ts` in the evidence folder: one `proofStep` per manual test step.

```ts
import { expect, test } from "@playwright/test";
import { proofStep, signIn, USERS } from "./proof";

test("PZ-1234 <short title>", async ({ page }) => {
  await proofStep(page, "Sign in as behandelaar", async () => {
    await signIn(page, USERS.behandelaar);
  });
  await proofStep(page, "Open the zaken werkvoorraad", async () => {
    await page.getByRole("button", { name: "Menu voor zaken" }).click();
    await page.getByRole("menuitem", { name: "Zaken-werkvoorraad" }).click();
    await expect(page).toHaveURL(/zaken\/werkvoorraad/);
  });
});
```

```bash
ZAC_URL=... bash <skill dir>/scripts/run-walkthrough.sh <evidence folder> [--headed]
```

The run leaves `screenshots/NN-<step>.png`, `steps.jsonl` (pass or fail per step, with the `/rest` requests that
failed during the step), `failed-requests.log`, `run.json` (where and how it ran) and `walkthrough.webm`.
`proofStep` waits for ZAC to settle before its screenshot; call `waitForZac(page)` yourself only before an
assertion on data that loads late.

- **Every step asserts what the change promises** (`expect`), so a pass means something. A step that only
  navigates is a setup step: keep it, but don't count it as proof.
- **Data:** look in the seed data first (explore the werkvoorraad, the inbox, the search). When the walkthrough
  needs data the stack does not have, create it through the UI in setup steps: it is the user's local stack.
- **A failing step is the script, the environment or the change.**
  - Script (wrong selector, timing): fix it and run again, at most three times.
  - Environment (401/403/5xx on requests that have nothing to do with the diff, a service that is down, missing
    seed data): stop, report the failed requests from `steps.jsonl`, and say what would fix the stack.
    Don't present the run as proof.
  - Change (the app does not do what the ticket says): that is the finding. Keep the failing step, its
    screenshot and the video, and report it. Never change application code to make the walkthrough pass.

## 6. Hand over the evidence

Write `pr-description.md` in the evidence folder, in English. `<where>` comes from `run.json` and step 3:
"the kind stack (ZAC image built from <short sha>)" or "the frontend dev server against the kind stack".

```markdown
## Proof

Ran <runRefDescription from resolve.json> locally in <where> on <run.json date>: <passed>/<total> steps passed.

| # | Step | Result |
|---|------|--------|
| 1 | Sign in as behandelaar | ✅ |
| 2 | ... | ❌ <error> |
| 3 | ... | ⏭️ not run |

<!-- drag screenshots/02-<step>.png here -->

<details><summary>Walkthrough video</summary>

<!-- drag walkthrough.webm here -->

</details>
```

Only the screenshots that show the change get a placeholder, not every step. Then `open <evidence folder>` and
tell the user:
- the result per step, and any finding or environment problem
- the evidence folder path
- that GitHub takes the `.webm` as is (up to 10 MB on free plans)
- for a teammate's PR: it is theirs to update, the user decides whether to share it

Offer once: turn the walkthrough into a real e2e scenario in `src/e2e/bdd` (only when the user asks).

## 7. Clean up

Tell the user how to get back to their own state; don't run it unasked:
- `bash <skill dir>/scripts/cleanup.sh <run name>` removes the worktree (`--all` for every proof worktree); the
  evidence folder stays.
- Their own ZAC again: `./start-kind.sh -b` (or `-l`, `-z`) from their own checkout, with the flags they normally use.
- A frontend dev server started in step 3: stop the background task.
