## Why

When distributing (verdelen) cases or tasks from the work queue (werkvoorraad), all groups are currently shown in the group selector — including groups not authorised for the zaaktype(s) involved. This means a coordinator can accidentally assign work to a group that cannot handle it. Filtering to only authorised groups prevents that mistake and makes the empty-state explicit when no authorised group exists for the full selection.

## What Changes

- **New backend endpoint** `POST /rest/identity/behandelaar-groups` accepts a JSON body with a list of zaaktype descriptions and returns the intersection of groups authorised for all of them. POST is used because the list can be large (up to ~100 descriptions) and would exceed URL length limits as query params.
- Frontend integration is a follow-up change.

## Capabilities

### New Capabilities
- `behandelaar-groups-for-multiple-zaaktypes`: New REST endpoint that returns the intersection of behandelaar-authorised groups for a given set of zaaktype descriptions.

### Modified Capabilities

## Impact

- **Backend**: `IdentityRestService` gets a new POST endpoint; `IdentityService` gets a new helper method (intersection logic over multiple zaaktype descriptions using PABC); integration tests cover the new endpoint.
- **OpenAPI spec**: regenerated to include the new endpoint.
- **No breaking changes** to existing endpoints.
