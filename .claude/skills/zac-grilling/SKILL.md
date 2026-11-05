---
name: zac-grilling
description: Grill the user relentlessly about a plan, decision, or idea for ZAC (zaakafhandelcomponent), with the ZAC-specific branches (REST API, policy, migrations, Solr, Flowable, Java-to-Kotlin conversion, PR split, ADRs, tests) on the design tree. Use in this repository when the user wants to stress-test their thinking, or uses any 'grill' trigger phrases.
---

Interview the user relentlessly until you reach a shared understanding. Map this as a **design tree**: every decision branches into the decisions that hang off it.

Work the tree in **rounds**. The **frontier** is every decision whose prerequisites are already settled: the questions you can ask _now_ without guessing at answers you haven't heard yet. Ask the whole frontier in one round: number each question and give your recommended answer. Then wait for the user's answers before the next round.

Each question should be formatted like so:

```
❓ **Q1** - **<question title>**: <question body, might be multiple paragraphs, including multiple choices>

➡️ <your recommended answer>
```

Each round the user answers reshapes the tree: settled decisions push the frontier outward and unblock questions that depended on them. Recompute the frontier and ask the next round. A question whose answer depends on another question still open in this round belongs to a _later_ round, not this one.

Finding _facts_ is your job, never the user's. When a frontier question needs a fact from the environment (filesystem, tools, etc.), dispatch a sub-agent to find it; don't ask the user for anything you could look up yourself. Don't block on it: a running exploration is an unsettled prerequisite, so only the questions downstream of it wait for the sub-agent to report; ask the rest of the frontier now. The _decisions_ are the user's: put each to them and wait.

In ZAC, look facts up in these sources before you ask anything:

- **Jira**: the tickets live on dimpact.atlassian.net, so use `acli` (`acli jira workitem view PZ-XXX`), not the Atlassian MCP. Read the story or bug, its subtasks, and linked issues: the acceptance criteria are the scope.
- **OpenSpec**: open changes in `openspec/changes/`, archived ones in `openspec/changes/archive/`, and the current behaviour per capability in `openspec/specs/<capability>/spec.md`.
- **ADRs**: `docs/solution-architecture/decisions/README.md` and the ADRs it lists. An `accepted` ADR is binding: a plan that conflicts with one is a question for the user, not a detail.
- **Conventions**: `CLAUDE.md`, `CONTRIBUTING.md`, `.claude/rules/`, and the guides in `docs/development/`.
- **The code itself**: callers, REST models, policies, migrations, and the existing tests.

## ZAC branches of the design tree

Make sure the tree visits every branch below that the plan can touch. A branch that does not apply is settled by a fact ("the plan changes no REST endpoint"), not skipped silently. Do not ask about a branch the facts already settle; say what you found instead.

- **Scope**: which acceptance criteria of which ticket does this cover, and what is explicitly not part of it?
- **REST API**: does a path, payload or validation change? Then the generated OpenAPI spec and frontend types change too, and the scripts in `scripts/test-data/` must be updated in the same PR.
- **Authorisation**: does the plan need a new or changed policy rule (`src/main/resources/policies/*.rego`, `nl/info/zac/policy/`) or a new right in the frontend?
- **Database**: is a Flyway migration needed (`src/main/resources/schemas/`), and does it run on a populated database with existing data?
- **Solr**: does a zoekobject or index field change, and do existing documents need a reindex?
- **Flowable**: do case or process variables, the CMMN model, or BPMN processes change, and what happens to instances that are already running?
- **Events and signaleringen**: which screen events, signaleringen or notifications must fire, and which must not change?
- **Frontend**: TanStack Query for data fetching, Form.io forms, and i18n keys in `nl.json` and `en.json`.
- **Configuration**: new environment variables in `.env.example`, the Helm chart in `charts/`, and the kind stack in `scripts/kind/`.
- **Java code**: does the plan change a file under `src/main/java/`? Then that code is first converted to Kotlin in a separate PR with the `migrate-java-to-kotlin` skill, without functional changes, and the feature PR builds on it. The conversion never goes into the feature PR itself, because it adds complexity to that PR: the reviewer can no longer tell the translated lines from the changed behaviour. Put the order of the PRs to the user as a decision.
- **PR split**: how many PRs, in which order, and which one is based on which?
- **Architecture**: does the plan add or replace a framework, library or external service, or change a layer, pattern, integration or convention? Then it needs a new ADR in the same PR.
- **Tests**: which behaviour is proven by unit tests, which by integration tests (`src/itest/`), and which by e2e tests?

The session is done when the frontier is empty: every branch of the design tree visited, nothing left silently assumed. Do not act on it until the user confirms you have reached a shared understanding.
