# docs/superpowers

AI-assisted development artifacts generated with [Claude Code](https://claude.com/claude-code) and the [Superpowers](https://github.com/obra/superpowers) skill set.

## Directory structure

### `specs/`

Design documents produced before implementation begins. Each spec describes the scope, test cases, fixtures, and mock strategy for a unit of work. Specs are written to be precise enough to generate an implementation plan from, but are not executable themselves.

Naming convention: `YYYY-MM-DD-<slug>.md`

### `plans/`

Step-by-step implementation plans derived from a spec. Each plan is structured as a series of tasks with concrete, copy-pasteable code snippets and verification commands. Plans are intended to be executed task-by-task by an agentic worker using the `superpowers:executing-plans` or `superpowers:subagent-driven-development` skill.

Naming convention: `YYYY-MM-DD-<slug>.md` (matches the corresponding spec)
