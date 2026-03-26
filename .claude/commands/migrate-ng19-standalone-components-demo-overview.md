# ZAC - Angular 19 Standalone Migration — Demo Overview

<p align="center"><img src="ng-demo.png" width="600" alt="NgModules → Standalone Components"></p>

**Why are we doing this?** New Angular versions require standalone components — `NgModule` is legacy as of Angular 17+ and unsupported for new APIs (`@defer`, signal inputs, etc.) in Angular 19. Newest Angular versions require standalone components. We need to move forward.

---

## What about ZAC project

**Project**: ZAC — Dutch municipal case management system (Angular 19 frontend + Kotlin/WildFly backend)

**Migration task**: Convert remaining `standalone: false` components to Angular 19 standalone. Already 20+ done, around 130 remaining (as of 2026-03-23).

---

## What the demo will show

The migration plan (`.claude/commands/migrate-ng19-standalone-components.md`) is a living runbook with 3 phases:

| Phase                    | What happens                                                                                                                                 |
| ------------------------ | -------------------------------------------------------------------------------------------------------------------------------------------- |
| **A — Analyse & branch** | Check Collaboration/Claims branch, Check open PRs, pick the right next component, create a branch                                            |
| **B — TDD loop**         | Read component → analyse template → write spec → baseline green → ask permission → migrate → clean module → tests pass → lint → commit claim |
| **C — Ship**             | Update plan MD, browser verify, propose PR (title + body for approval), push                                                                 |

**Key guardrails I strictly follow:**

- Never auto-commit or auto-push
- Always wait for explicit user approval before `gh pr create`
- No `any` anywhere — zero exceptions
- Use protected/private functions where possible
- No `NO_ERRORS_SCHEMA` in specs
- Skip `shared/material-form-builder/` (ATOS form builder, being phased out)
- TDD: spec baseline must be green _before_ migration starts

**Next target**: TBD — `/admin` lazy-load is complete (the intermediate goal), so we'll pick the next module together.
