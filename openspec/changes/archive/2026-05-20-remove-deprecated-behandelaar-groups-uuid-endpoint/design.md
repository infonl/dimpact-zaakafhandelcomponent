## Context

Two deprecated identifiers have been marked for removal since the PABC migration:

- **Backend**: `IdentityRestService.listBehandelaarGroupsForZaaktypeUuid` (`GET /rest/identity/groups/behandelaar/zaaktype/{zaaktypeUuid}`) and `IdentityService.listActiveGroupsForBehandelaarRoleAndZaaktypeUuid`. Both are annotated `@Deprecated` with explicit instructions to delete them.
- **Frontend**: `IdentityService.listBehandelaarGroupsForZaaktype(zaaktypeUuid)` currently calls the deprecated endpoint. All six call sites pass a UUID, but the zaaktype description (`omschrijving`) is available at every call site.
- The replacement endpoint (`GET /rest/identity/zaaktype/{zaaktypeDescription}/behandelaar-groups`) already exists and is PABC-native.

The `MedewerkerGroepFormField` and its builder carry a `zaaktypeUuid` field that is the last UUID-carrying artifact in the chain.

## Goals / Non-Goals

**Goals:**
- Delete the deprecated backend endpoint and service method
- Update the frontend `IdentityService` wrapper to call the new endpoint with `zaaktypeDescription`
- Rename `zaaktypeUuid` → `zaaktypeOmschrijving` in `MedewerkerGroepFormField` and its builder
- Update all component callers to pass the description instead of the UUID
- Remove/update all affected tests

**Non-Goals:**
- Changing the business logic of group authorisation
- Altering the PABC feature flag behaviour
- Updating the OpenAPI spec file (the deprecated endpoint simply disappears)

## Decisions

### D1: Rename field in `MedewerkerGroepFormField` from `zaaktypeUuid` to `zaaktypeOmschrijving`

**Why**: The field drives the API call; keeping a field named `zaaktypeUuid` that actually holds an `omschrijving` would be confusing.

**Alternative considered**: Keep the field name and just change the value passed. Rejected — misleading naming is a maintenance hazard.

**Affected files**:
- `medewerker-groep-form-field.ts` — rename property
- `medewerker-groep-field-builder.ts` — rename `setZaaktypeUuid` → `setZaaktypeOmschrijving`, update setter
- `medewerker-groep.component.ts` — update field reference in `setGroups()`

### D2: Update all callers of `setZaaktypeUuid` to pass `omschrijving`

All call sites already have the description available:

| Caller | Current | Replacement |
|--------|---------|-------------|
| `taak-formulier-builder.ts:43` | `zaak.zaaktype.uuid` | `zaak.zaaktype.omschrijving!` |
| `taak-view.component.ts:349` | `this.taak.zaaktypeUUID!` | `this.taak.zaaktypeOmschrijving!` |
| `taak-edit.component.ts` | `this.task().zaaktypeUUID!` | `this.task().zaaktypeOmschrijving!` |
| `zaak-create.component.ts` | `caseType.uuid` | `caseType.omschrijving!` |
| `zaak-details-wijzigen.component.ts` | `this.zaak.zaaktype.uuid` | `this.zaak.zaaktype.omschrijving!` |
| `human-task-do.component.ts` | `this.zaak.zaaktype.uuid` | `this.zaak.zaaktype.omschrijving!` |

### D3: Update `IdentityService` (frontend) method signature

Rename parameter from `zaaktypeUuid: string` → `zaaktypeDescription: string` and switch the HTTP call from the deprecated path to `/rest/identity/zaaktype/{zaaktypeDescription}/behandelaar-groups`.

### D4: Handle direct HTTP call in `formio-setup-service.ts`

The formio setup service calls the deprecated endpoint directly (not via the service wrapper). It also has `this.taak!.zaaktypeOmschrijving` available. Switch to use `IdentityService.listBehandelaarGroupsForZaaktype` with the omschrijving.

## Risks / Trade-offs

- **Risk**: A consumer outside this repo still calls the deprecated endpoint → Mitigation: The `@Deprecated` annotation has been in place; no internal callers will remain after this change. External consumers were warned.
- **Risk**: `zaaktypeOmschrijving` is nullable (`string | null`) at some call sites → Mitigation: Use non-null assertion (`!`) matching existing patterns; these fields are always set in valid task/zaak objects.

## Migration Plan

1. Delete deprecated backend method and endpoint (no DB migration needed).
2. Update frontend `IdentityService` and all callers in a single commit — the old endpoint is gone, so a partial migration would break the build.
3. Run full test suite to confirm no regressions.
