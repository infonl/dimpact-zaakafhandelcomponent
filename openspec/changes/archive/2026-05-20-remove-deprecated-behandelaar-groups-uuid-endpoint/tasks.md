## 1. Backend Removal

- [x] 1.1 Delete `listBehandelaarGroupsForZaaktypeUuid` endpoint method from `IdentityRestService.kt`
- [x] 1.2 Delete `listActiveGroupsForBehandelaarRoleAndZaaktypeUuid` method from `IdentityService.kt` (including the `ZtcClientService` delegation)
- [x] 1.3 Remove unused `import java.util.UUID` from `IdentityRestService.kt` if no longer needed
- [x] 1.4 Remove unused `ZtcClientService` injection from `IdentityService.kt` if no longer needed after deletion

## 2. Frontend Service Update

- [x] 2.1 In `identity.service.ts`: rename parameter `zaaktypeUuid` → `zaaktypeDescription`, switch HTTP call from `/rest/identity/groups/behandelaar/zaaktype/{zaaktypeUuid}` to `/rest/identity/zaaktype/{zaaktypeDescription}/behandelaar-groups`

## 3. Frontend MedewerkerGroep Field Update

- [x] 3.1 In `medewerker-groep-form-field.ts`: rename field `zaaktypeUuid: string` → `zaaktypeOmschrijving: string`
- [x] 3.2 In `medewerker-groep-field-builder.ts`: rename `setZaaktypeUuid` → `setZaaktypeOmschrijving`, update setter to assign `zaaktypeOmschrijving`
- [x] 3.3 In `medewerker-groep.component.ts`: update `setGroups()` to use `this.data.zaaktypeOmschrijving`

## 4. Frontend Caller Updates

- [x] 4.1 In `taak-formulier-builder.ts:43`: change `.setZaaktypeUuid(zaak.zaaktype.uuid)` → `.setZaaktypeOmschrijving(zaak.zaaktype.omschrijving!)`
- [x] 4.2 In `taak-view.component.ts:349`: change `.setZaaktypeUuid(this.taak.zaaktypeUUID!)` → `.setZaaktypeOmschrijving(this.taak.zaaktypeOmschrijving!)`
- [x] 4.3 In `formio-setup-service.ts`: switch direct deprecated endpoint call to use the frontend `IdentityService.listBehandelaarGroupsForZaaktype` method with `this.taak!.zaaktypeOmschrijving!`
- [x] 4.4 In `taak-edit.component.ts`: change `.listBehandelaarGroupsForZaaktype(this.task().zaaktypeUUID!)` → `(this.task().zaaktypeOmschrijving!)`
- [x] 4.5 In `zaak-create.component.ts`: change `.listBehandelaarGroupsForZaaktype(caseType.uuid)` → `(caseType.omschrijving!)`
- [x] 4.6 In `zaak-details-wijzigen.component.ts`: change `.listBehandelaarGroupsForZaaktype(this.zaak.zaaktype.uuid)` → `(this.zaak.zaaktype.omschrijving!)`
- [x] 4.7 In `human-task-do.component.ts`: change `.listBehandelaarGroupsForZaaktype(this.zaak.zaaktype.uuid)` → `(this.zaak.zaaktype.omschrijving!)`

## 5. Test Updates

- [x] 5.1 Update/remove unit tests for `IdentityService` (Kotlin) covering `listActiveGroupsForBehandelaarRoleAndZaaktypeUuid`
- [x] 5.2 Update/remove unit tests for `IdentityRestService` (Kotlin) covering `listBehandelaarGroupsForZaaktypeUuid`
- [x] 5.3 Update frontend unit tests for `identity.service.ts` to reflect new endpoint and parameter
- [x] 5.4 Update frontend unit tests for `medewerker-groep.component.ts` to use `zaaktypeOmschrijving`
- [x] 5.5 Update frontend unit tests for `medewerker-groep-field-builder.ts` to use `setZaaktypeOmschrijving`
- [x] 5.6 Update any integration tests that exercise `GET /rest/identity/groups/behandelaar/zaaktype/{zaaktypeUuid}`
- [x] 5.7 Update `taak-edit.component.spec.ts` assertion: `toHaveBeenCalledWith("zaaktype-uuid-1")` → `toHaveBeenCalledWith("Test Zaaktype")`

## 6. Verification

- [x] 6.1 Run backend unit tests (`./gradlew test`) — all pass
- [x] 6.2 Run frontend unit tests (`npm test`) — all pass (2105/2105; 1 pre-existing SIGSEGV crash in unrelated cmmn spec)
- [ ] 6.3 Run integration tests — all pass
