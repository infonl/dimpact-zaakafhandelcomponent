## 1. Backend Removal

- [ ] 1.1 Delete `listBehandelaarGroupsForZaaktypeUuid` endpoint method from `IdentityRestService.kt`
- [ ] 1.2 Delete `listActiveGroupsForBehandelaarRoleAndZaaktypeUuid` method from `IdentityService.kt` (including the `ZtcClientService` delegation)
- [ ] 1.3 Remove unused `import java.util.UUID` from `IdentityRestService.kt` if no longer needed
- [ ] 1.4 Remove unused `ZtcClientService` injection from `IdentityService.kt` if no longer needed after deletion

## 2. Frontend Service Update

- [ ] 2.1 In `identity.service.ts`: rename parameter `zaaktypeUuid` → `zaaktypeDescription`, switch HTTP call from `/rest/identity/groups/behandelaar/zaaktype/{zaaktypeUuid}` to `/rest/identity/zaaktype/{zaaktypeDescription}/behandelaar-groups`

## 3. Frontend MedewerkerGroep Field Update

- [ ] 3.1 In `medewerker-groep-form-field.ts`: rename field `zaaktypeUuid: string` → `zaaktypeOmschrijving: string`
- [ ] 3.2 In `medewerker-groep-field-builder.ts`: rename `setZaaktypeUuid` → `setZaaktypeOmschrijving`, update setter to assign `zaaktypeOmschrijving`
- [ ] 3.3 In `medewerker-groep.component.ts`: update `setGroups()` to use `this.data.zaaktypeOmschrijving`

## 4. Frontend Caller Updates

- [ ] 4.1 In `taak-formulier-builder.ts:43`: change `.setZaaktypeUuid(zaak.zaaktype.uuid)` → `.setZaaktypeOmschrijving(zaak.zaaktype.omschrijving!)`
- [ ] 4.2 In `taak-view.component.ts:349`: change `.setZaaktypeUuid(this.taak.zaaktypeUUID!)` → `.setZaaktypeOmschrijving(this.taak.zaaktypeOmschrijving!)`
- [ ] 4.3 In `formio-setup-service.ts`: switch direct deprecated endpoint call to use the frontend `IdentityService.listBehandelaarGroupsForZaaktype` method with `this.taak!.zaaktypeOmschrijving!`

## 5. Test Updates

- [ ] 5.1 Update/remove unit tests for `IdentityService` (Kotlin) covering `listActiveGroupsForBehandelaarRoleAndZaaktypeUuid`
- [ ] 5.2 Update/remove unit tests for `IdentityRestService` (Kotlin) covering `listBehandelaarGroupsForZaaktypeUuid`
- [ ] 5.3 Update frontend unit tests for `identity.service.ts` to reflect new endpoint and parameter
- [ ] 5.4 Update frontend unit tests for `medewerker-groep.component.ts` to use `zaaktypeOmschrijving`
- [ ] 5.5 Update frontend unit tests for `medewerker-groep-field-builder.ts` to use `setZaaktypeOmschrijving`
- [ ] 5.6 Update any integration tests that exercise `GET /rest/identity/groups/behandelaar/zaaktype/{zaaktypeUuid}`

## 6. Verification

- [ ] 6.1 Run backend unit tests (`./gradlew test`) — all pass
- [ ] 6.2 Run frontend unit tests (`npm test`) — all pass
- [ ] 6.3 Run integration tests — all pass
