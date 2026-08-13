## Why

The `remove-implicit-role-hierarchy` change (PZ-11711) removed the implicit application-role hierarchy (`raadpleger < behandelaar < coordinator < recordmanager < beheerder`) from the OPA policies and updated `docs/solution-architecture/accessControlPolicies.md` to match, but did not touch the ZAC user manual (`ZAC-gebruikershandleiding.md`). That manual's "Rollen en rechten" section still describes roles in an ascending, additive style ("aanvullende rechten") left over from the old inherited model, is missing the `beheerder` role entirely, and gives an incomplete rights summary for `coordinator` that only "worked" because of the now-removed inheritance. PZ-11245 asks that all end-user-facing documentation be checked and brought in line with the explicit, non-hierarchical role model.

## What Changes

- Rewrite the "Rollen en rechten" section of `docs/manuals/ZAC-gebruikershandleiding/ZAC-gebruikershandleiding.md` so each of the 5 application roles (`raadpleger`, `behandelaar`, `coordinator`, `recordmanager`, `beheerder`) is described with its own, independently-granted rights, and drop the "aanvullende rechten" (additional rights) framing that implied `recordmanager` builds on top of `raadpleger`.
- Add the missing `beheerder` role description (currently absent from the manual entirely).
- Complete the `coordinator` description to mention its document/notitie rights (read/download documents, read/edit notities), which were previously implied only through inheritance from `raadpleger`.
- Correct the role count statement ("vier rollen") to reflect the actual number of roles described.
- No other documentation file needs updating: `docs/manuals/inrichting-zaakafhandelcomponent/inrichting-zaakafhandelcomponent.md`, `docs/solution-architecture/iamArchitecture.md`, `README.md`, and `CONTRIBUTING.md` were checked and contain no role-hierarchy narrative (confirmed by a full-text search for role/hierarchy keywords across `docs/`).

## Capabilities

### New Capabilities

(none)

### Modified Capabilities

- `application-role-permission-matrix`: extend the existing "documentation lists every explicit grant with no inheritance narrative" requirement to also cover `docs/manuals/ZAC-gebruikershandleiding/ZAC-gebruikershandleiding.md`, not just `accessControlPolicies.md`.

## Impact

- Affected file: `docs/manuals/ZAC-gebruikershandleiding/ZAC-gebruikershandleiding.md` (documentation only, no code or policy changes).
- No backend, frontend, or OPA policy changes required — this change is purely a documentation correction following PZ-11711.
