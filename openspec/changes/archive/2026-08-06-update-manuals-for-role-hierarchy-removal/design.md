## Context

`remove-implicit-role-hierarchy` (PZ-11711, archived) removed the implicit role hierarchy from the OPA Rego policies and updated `docs/solution-architecture/accessControlPolicies.md` accordingly, but the change's scope was the OPA policies and that one architecture document. It did not audit the end-user manuals under `docs/manuals/`. PZ-11245 asks that this audit be done and any stale hierarchy narrative be corrected.

A full-text search across `docs/**/*.md`, `README.md`, and `CONTRIBUTING.md` for role- and hierarchy-related keywords (Dutch and English) found exactly one section with hierarchy-implying prose: the "Rollen en rechten" section (lines 168-178) of `docs/manuals/ZAC-gebruikershandleiding/ZAC-gebruikershandleiding.md`. `docs/manuals/inrichting-zaakafhandelcomponent/inrichting-zaakafhandelcomponent.md` already lists all 6 application roles as a flat, non-hierarchical bullet list and needs no change.

## Goals / Non-Goals

**Goals:**
- Bring the "Rollen en rechten" section of the user manual in line with the explicit, non-hierarchical role model already documented in `accessControlPolicies.md`.
- Ensure every application role the OPA policies grant permissions to (`raadpleger`, `behandelaar`, `coordinator`, `recordmanager`, `beheerder`) is described in the manual, each with its own directly-granted rights.

**Non-Goals:**
- No changes to OPA policies, backend, or frontend code — this is a documentation-only follow-up.
- No changes to the historical, version-pinned PDF manuals (`ZAC-V*.*-gebruikershandleiding.pdf`, `ZAC-V*.*-inrichting-zaakafhandelcomponent.pdf`) — those are frozen snapshots of past releases and are not regenerated as part of routine doc fixes.
- Not adding `brp_zoeken` to this section: it's a search-only capability unrelated to the zaak/taak/document rechten narrative this section covers, and isn't part of the `raadpleger < ... < beheerder` hierarchy that was removed.
- No change to `docs/manuals/inrichting-zaakafhandelcomponent/inrichting-zaakafhandelcomponent.md` — confirmed to contain no hierarchy narrative.

## Decisions

- **Scope the fix to one file, one section.** The keyword/narrative search covered the full `docs/` tree; only `ZAC-gebruikershandleiding.md`'s "Rollen en rechten" section contained inheritance-style language ("aanvullende rechten") or an incomplete/missing role list. No other file needs edits, so this change touches only that section rather than re-auditing unrelated pages.
- **Extend the existing `application-role-permission-matrix` capability instead of creating a new one.** The capability's second requirement already asserts "no inheritance narrative" for `accessControlPolicies.md`; broadening its scope to also cover the user manual keeps one place as the source of truth for "our docs must not describe role inheritance," rather than duplicating a near-identical requirement under a new capability name.
- **Describe each role's rights independently, cross-checked against the `.rego` policies**, rather than translating the accessControlPolicies.md permission table verbatim into prose. The manual is a narrative, task-oriented document for end users, not a technical reference table; keeping it prose-based but making every sentence state a role's own direct grant (never "also has X's rights") satisfies the requirement without turning the manual into a duplicate of the technical matrix.

## Risks / Trade-offs

- **Prose can drift from the policy matrix again over time** → the `application-role-permission-matrix` capability's "Permission table matches policy grants" scenario already covers `accessControlPolicies.md`; this change adds an equivalent expectation for the manual so future OPA policy edits are more likely to prompt a manual check too (reinforced by a note in "Updating OPA policies" guidance, if not already present).
- **Manual is meant for end users, not developers** → keep the rewritten section short and role-by-role, matching the existing style of the surrounding text, rather than pasting in Rego-level detail (zaaktype checks, lock state, etc.) that belongs only in the technical `accessControlPolicies.md`.
