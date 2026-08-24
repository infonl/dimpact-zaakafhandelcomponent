## 1. Shared "zaakspecifiek geautoriseerd" lookup helper

- [ ] 1.1 Extract the `ZAAK_GEAUTORISEERD` zaakeigenschap check duplicated in `RestZaakConverter`
      (`ZAAKEIGENSCHAP_NAAM_GEAUTORISEERD` / `ZAAKEIGENSCHAP_WAARDE_GEAUTORISEERD`,
      `src/main/kotlin/nl/info/zac/app/zaak/converter/RestZaakConverter.kt:71-73,114-116`) into a shared,
      reusable function so both `RestZaakConverter` and `PolicyService` call the same logic.
- [ ] 1.2 Update `RestZaakConverter` to use the shared helper.
- [ ] 1.3 Add/adjust unit tests for the shared helper and `RestZaakConverterTest`
      (`src/test/kotlin/nl/info/zac/app/zaak/converter/RestZaakConverterTest.kt:640-682`) to confirm no
      behaviour change.

## 2. Policy input model

- [ ] 2.1 Add `zaakspecifiekGeautoriseerd: Boolean` (default `false`) to `ZaakData`
      (`src/main/kotlin/nl/info/zac/policy/input/ZaakData.kt`).
- [ ] 2.2 Add `zaakspecifiekGeautoriseerd: Boolean` (default `false`) to `TaakData`
      (`src/main/kotlin/nl/info/zac/policy/input/TaakData.kt`).
- [ ] 2.3 Add `zaakspecifiekGeautoriseerd: Boolean` (default `false`) to `DocumentData`
      (`src/main/kotlin/nl/info/zac/policy/input/DocumentData.kt`).

## 3. Populate the new field in PolicyService

- [ ] 3.1 Populate `ZaakData.zaakspecifiekGeautoriseerd` in `PolicyService.readZaakRechten(zaak, zaaktype, loggedInUser)`
      (`src/main/kotlin/nl/info/zac/policy/PolicyService.kt:87-110`) using the shared helper from task 1.1.
- [ ] 3.2 Populate `TaakData.zaakspecifiekGeautoriseerd` in `PolicyService.readTaakRechten(taskInfo, zaaktypeOmschrijving)`
      (`PolicyService.kt:190-206`), resolving the zaak UUID via
      `TaakVariabelenService.readZaakUUID(taskInfo)` (`src/main/java/net/atos/zac/flowable/task/TaakVariabelenService.java:110`)
      and applying the shared helper.
- [ ] 3.3 Populate `DocumentData.zaakspecifiekGeautoriseerd` in
      `PolicyService.readDocumentRechten(enkelvoudigInformatieobject, lock, zaak)` (`PolicyService.kt:143-164`)
      using the already-passed `zaak: Zaak?` parameter and the shared helper (`false` when `zaak` is `null`).
- [ ] 3.4 Leave `readZaakRechtenForZaakZoekObject`, `readTaakRechten(taakZoekObject)`, and
      `readDocumentRechten(enkelvoudigInformatieobject: DocumentZoekObject)` unchanged (field defaults to
      `false`), per the werklijsten/zoekresultaten scope boundary.
- [ ] 3.5 Add/adjust `PolicyServiceTest` unit tests covering the new `zaakspecifiekGeautoriseerd` population for all three
      direct-read call sites, including the "no zaak" / "not zaakspecifiekGeautoriseerd" default cases.

## 4. OPA policy: zaak-rechten.rego

- [ ] 4.1 Import `zaakspecifiekAutorisatieBehandelaar` from `data.net.atos.zac.rol.zaakspecifiekAutorisatieBehandelaar`
      (`src/main/resources/policies/zaak-rechten.rego`; constant already defined in
      `src/main/resources/policies/rollen.rego:34-36`, currently unused).
- [ ] 4.2 Add a `default zaak_allowed := false` rule set: true when `not zaak.zaakspecifiekGeautoriseerd`, or
      when the user holds `zaakspecifiekAutorisatieBehandelaar`. Do not reference `recordmanager`/`beheerder`
      in this rule — their access to a zaakspecifiek geautoriseerde zaak is out of scope for this change.
- [ ] 4.3 Add `zaak_allowed` as an extra condition only to the rule bodies that grant
      `raadpleger`, `behandelaar`, and/or `coordinator` a permission (`lezen`, the `{behandelaar, coordinator}`
      body of `wijzigen` and `toekennen`, `behandelen`, `afbreken`, `wijzigen_doorlooptijd`, `verlengen`,
      `opschorten`, `hervatten`, `creeren_document`, the `{behandelaar, coordinator}` body of
      `toevoegen_document` and `koppelen`, `versturen_email`, `versturen_ontvangstbevestiging`, the
      `{behandelaar, coordinator}` body of `toevoegen_initiator_persoon`, `toevoegen_initiator_bedrijf`,
      `verwijderen_initiator`, `toevoegen_betrokkene_persoon`, `toevoegen_betrokkene_bedrijf`,
      `verwijderen_betrokkene`, `toevoegen_bag_object`, `starten_taak`, `vastleggen_besluit`,
      `verlengen_doorlooptijd`). Split any rule that currently combines `recordmanager`/`beheerder` into the
      same body as `raadpleger`/`behandelaar`/`coordinator` (e.g. `lezen`, `behandelen`, `afbreken`,
      `wijzigen_doorlooptijd`, `verlengen`, `opschorten`, `hervatten`, `creeren_document`, `versturen_email`,
      `versturen_ontvangstbevestiging`, `starten_taak`, `vastleggen_besluit`, `verlengen_doorlooptijd`) into
      two bodies, so the gate applies only to the non-privileged half. Leave the `recordmanager`/`beheerder`
      bodies (and the `recordmanager`/`beheerder`-only rules `heropenen`, `bekijken_zaakdata`,
      `brondatum_zetten`) completely untouched. `wijzigen_locatie` is derived from `wijzigen` and needs no
      separate change.
- [ ] 4.4 Add `zaakspecifiekAutorisatieBehandelaar` to every `raadpleger`/`behandelaar`/`coordinator` role set
      that currently contains `behandelaar` in this file (not to the untouched `recordmanager`/`beheerder`
      bodies), so the new role's grants match `behandelaar`'s exactly.

## 5. OPA policy: taak-rechten.rego and document-rechten.rego

- [ ] 5.1 Add `zaakspecifiekGeautoriseerd` to the `input.taak` usage in `taak-rechten.rego` (import the role, add
      `zaak_allowed`, gate/split only the `raadpleger`/`behandelaar`/`coordinator` rule bodies,
      add `zaakspecifiekAutorisatieBehandelaar` next to every `behandelaar` occurrence in those bodies, leave
      `recordmanager`/`beheerder` bodies untouched) mirroring task group 4.
- [ ] 5.2 Do the same for `document-rechten.rego` (`input.document`).

## 6. Rego unit tests

- [ ] 6.1 Add scenarios to `src/test/resources/policies/zaak-rechten_test.rego` covering: a geautoriseerde
      zaak denies `behandelaar`/`raadpleger`/`coordinator`, allows `zaakspecifiekAutorisatieBehandelaar`; a
      non-geautoriseerde zaak is unaffected; the new role alone gets behandelaar-equivalent rights and nothing
      beyond. Do not add assertions about `recordmanager`/`beheerder` behaviour for the zaakspecifiek
      geautoriseerde case — out of scope for this change.
- [ ] 6.2 Add equivalent test coverage for `taak-rechten.rego` and `document-rechten.rego` (new test files if
      none exist yet, following the `zaak-rechten_test.rego` structure).
- [ ] 6.3 Run `opa test` locally (or via the `opa-tests` docker-compose service,
      `docker-compose.yaml:386-393`) to confirm all Rego tests pass.

## 7. Documentation

- [ ] 7.1 Add `zaakspecifiek_autorisatie_behandelaar` to the application roles table in
      `docs/solution-architecture/accessControlPolicies.md` with its description.
- [ ] 7.2 Add a `zaakspecifiek_autorisatie_behandelaar` column to the permission matrix table, checked for
      every permission the role now has (mirroring `behandelaar`'s checkmarks).
- [ ] 7.3 Add a note to `accessControlPolicies.md` documenting that `behandelaar`/`raadpleger`/`coordinator`
      checkmarks do not apply to a zaakspecifiek geautoriseerde zaak (or its taken/documenten) unless that
      user also holds `zaakspecifiek_autorisatie_behandelaar` for the zaaktype. Do not claim that
      `recordmanager`/`beheerder` access to a zaakspecifiek geautoriseerde zaak is specified or restricted by
      this change — leave that to a follow-up story.

## 8. Integration tests

- [ ] 8.1 Add scenarios to `src/itest/kotlin/nl/info/zac/itest/ZaakRestServiceTest.kt` that create (or reuse)
      a zaak marked zaakspecifiek geautoriseerd via its `ZAAK_GEAUTORISEERD` zaakeigenschap, then verify
      `readZaak`/`readZaakById` returns `HTTP_FORBIDDEN` for `BEHANDELAAR_1` and `HTTP_OK` for
      `ZAAKSPECIFIEK_AUTORISATIE_BEHANDELAAR_1` (existing fixture:
      `src/itest/kotlin/nl/info/zac/itest/config/TestUsers.kt:131-139`), following the existing
      `HTTP_FORBIDDEN` pattern at `ZaakRestServiceTest.kt:1104-1163`.
- [ ] 8.2 Add an equivalent taak-access itest (behandelaar denied, zaakspecifiek_autorisatie_behandelaar
      allowed, for a taak of a zaakspecifiek geautoriseerde zaak).
- [ ] 8.3 Add an equivalent document-access itest (behandelaar denied, zaakspecifiek_autorisatie_behandelaar
      allowed, for a document linked to a zaakspecifiek geautoriseerde zaak). Do not add itest coverage for
      `recordmanager`/`beheerder` access to a zaakspecifiek geautoriseerde zaak — out of scope, left to a
      follow-up story.

## 9. Verification

- [ ] 9.1 Run `./gradlew spotlessApply detektApply` and `./gradlew test`.
- [ ] 9.2 Run `./gradlew itest --info` (after rebuilding the ZAC Docker image) to confirm the new integration
      tests pass end to end.
