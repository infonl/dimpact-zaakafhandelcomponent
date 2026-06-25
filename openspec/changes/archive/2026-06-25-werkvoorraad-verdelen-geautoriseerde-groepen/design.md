## Context

Coordinators distribute (verdelen) cases and tasks from the work queue using `ZakenVerdelenDialogComponent` and `TakenVerdelenDialogComponent`. Both dialogs currently populate the group selector with `identityService.listGroups()` — all active groups, unfiltered. The PABC integration already provides `GET /rest/identity/zaaktype/{zaaktypeDescription}/behandelaar-groups` for single-zaaktype filtering (used in the zaakdetail page and taak-edit view). The verdelen dialogs must respect the same authorisation but for a set of zaaktypes at once, returning only the intersection of authorised groups.

## Goals / Non-Goals

**Goals:**
- New backend endpoint returning the intersection of behandelaar-authorised groups for one or more zaaktype descriptions.
- Integration tests covering the new endpoint.

**Non-Goals:**
- Frontend verdelen dialog changes — follow-up change.
- Changes to the single-zaaktype endpoint `GET /rest/identity/zaaktype/{zaaktypeDescription}/behandelaar-groups`.
- Any change to downstream authorisation or PABC configuration.

## Decisions

### New endpoint vs. extending the existing single-zaaktype endpoint

**Decision**: Add a new endpoint `POST /rest/identity/behandelaar-groups` with a JSON body `{ "zaaktypeDescriptions": [...] }`.

**Rationale**: The list of zaaktype descriptions can be large (up to ~100 entries). Query params would exceed URL length limits. POST with a JSON body has no such constraint and is idiomatic for this pattern. The existing single-zaaktype GET endpoint is left unchanged for all current callers.

**Alternative considered**: GET with query params. Rejected: URL length limit (~2 KB practical cap) is easily exceeded with 100 descriptions; encoding issues with special characters in descriptions add further risk.

### Intersection logic in IdentityService

**Decision**: Fetch authorised groups per zaaktype description sequentially and compute the intersection in Kotlin using `Set.intersect`.

**Rationale**: PABC is an internal integration service; N small sequential calls are acceptable for the typical selection size (handful of zaaktypes). No batch API exists on the PABC side. Caching is not added now — PABC already has its own caching; adding a second layer is premature.

## Risks / Trade-offs

- **PABC unavailable** → the new endpoint returns HTTP 500 (same behaviour as the existing single-zaaktype endpoint). Mitigation: no change needed; PABC availability is an operational concern already handled.
- **Large number of distinct zaaktypes** → N sequential PABC calls per request. Mitigation: the list is bounded by the number of distinct zaaktypes configured in the system, which is small in practice; no batching needed now.

## Migration Plan

1. Deploy backend with new endpoint (additive, no schema removal).
2. No database migration required.
3. Rollback: remove the new endpoint; no downstream callers exist yet.
