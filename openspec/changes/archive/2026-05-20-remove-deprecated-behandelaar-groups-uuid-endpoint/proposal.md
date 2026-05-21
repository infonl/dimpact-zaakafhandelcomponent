## Why

The `listBehandelaarGroupsForZaaktypeUuid` REST endpoint and its backing service method `listActiveGroupsForBehandelaarRoleAndZaaktypeUuid` are already marked `@Deprecated` — they exist only as a migration bridge from the old IAM architecture (UUID-based zaaktype identification) to the current PABC model (description-based). The replacement endpoint `listBehandelaarGroupsForZaaktype` is already in production use. Removing this dead code eliminates an unnecessary Keycloak + ZTC lookup and clarifies the API surface.

## What Changes

- Remove `GET /identity/groups/behandelaar/zaaktype/{zaaktypeUuid}` REST endpoint (`listBehandelaarGroupsForZaaktypeUuid`) from `IdentityRestService`
- Remove `listActiveGroupsForBehandelaarRoleAndZaaktypeUuid(zaaktypeUuid: UUID)` from `IdentityService`
- Update any frontend code that calls the UUID-based endpoint to call `GET /identity/zaaktype/{zaaktypeDescription}/behandelaar-groups` instead, passing the zaaktype `omschrijving` (description) rather than the UUID
- Remove or update all unit and integration tests that cover the deleted code

## Capabilities

### New Capabilities
<!-- none — this is a removal -->

### Modified Capabilities
<!-- none — observable behavior is unchanged; only the deprecated code path is removed -->

## Impact

- **Backend**: `IdentityRestService.kt`, `IdentityService.kt` — two functions deleted
- **Frontend**: wherever `listBehandelaarGroupsForZaaktypeUuid` is called — callers must switch to `listBehandelaarGroupsForZaaktype` with the zaaktype description
- **Tests**: unit tests for `IdentityService` and `IdentityRestService`; any integration tests exercising the deprecated endpoint
- **API**: the deprecated endpoint is removed from the public API surface (**BREAKING** for any consumer still using it, though none are expected)
