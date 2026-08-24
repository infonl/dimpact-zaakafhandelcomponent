## 1. Shared "zaakspecifiek geautoriseerd" lookup helper

- [x] 1.1 Extract the `ZAAK_GEAUTORISEERD` zaakeigenschap check duplicated in `RestZaakConverter`
      (`ZAAKEIGENSCHAP_NAAM_GEAUTORISEERD` / `ZAAKEIGENSCHAP_WAARDE_GEAUTORISEERD`,
      `src/main/kotlin/nl/info/zac/app/zaak/converter/RestZaakConverter.kt:71-73,114-116`) into a shared,
      reusable function (`ZrcClientService.isZaakspecifiekGeautoriseerd`,
      `src/main/kotlin/nl/info/client/zgw/zrc/util/ZaakEigenschapUtil.kt`) so both `RestZaakConverter` and
      `PolicyService` call the same logic.
- [x] 1.2 Update `RestZaakConverter` to use the shared helper.
- [x] 1.3 Confirmed no behaviour change via existing `RestZaakConverterTest`
      (`src/test/kotlin/nl/info/zac/app/zaak/converter/RestZaakConverterTest.kt:640-682`).

## 2. Policy input model

- [x] 2.1 Add `zaakspecifiekGeautoriseerd: Boolean` to `ZaakData`
      (`src/main/kotlin/nl/info/zac/policy/input/ZaakData.kt`).
- [x] 2.2 Add `zaakspecifiekGeautoriseerd: Boolean` (default `false`) to `TaakData`
      (`src/main/kotlin/nl/info/zac/policy/input/TaakData.kt`).
- [x] 2.3 Add `zaakspecifiekGeautoriseerd: Boolean` (default `false`) to `DocumentData`
      (`src/main/kotlin/nl/info/zac/policy/input/DocumentData.kt`).

## 3. Populate the new field in PolicyService

- [x] 3.1 Populate `ZaakData.zaakspecifiekGeautoriseerd` in `PolicyService.readZaakRechten(zaak, zaaktype, loggedInUser)`
      (`src/main/kotlin/nl/info/zac/policy/PolicyService.kt:87-110`) using the shared helper from task 1.1.
- [x] 3.2 Populate `TaakData.zaakspecifiekGeautoriseerd` in `PolicyService.readTaakRechten(taskInfo, zaaktypeOmschrijving)`
      (`PolicyService.kt:190-206`), resolving the zaak UUID via
      `TaakVariabelenService.readZaakUUID(taskInfo)` (`src/main/java/net/atos/zac/flowable/task/TaakVariabelenService.java:110`)
      and applying the shared helper.
- [x] 3.3 Populate `DocumentData.zaakspecifiekGeautoriseerd` in
      `PolicyService.readDocumentRechten(enkelvoudigInformatieobject, lock, zaak)` (`PolicyService.kt:143-164`)
      using the already-passed `zaak: Zaak?` parameter and the shared helper (`false` when `zaak` is `null`).
- [x] 3.4 Leave `readZaakRechtenForZaakZoekObject`, `readTaakRechten(taakZoekObject)`, and
      `readDocumentRechten(enkelvoudigInformatieobject: DocumentZoekObject)` unchanged (field defaults to
      `false`), per the werklijsten/zoekresultaten scope boundary.
- [x] 3.5 `PolicyServiceTest` unit tests cover the new `zaakspecifiekGeautoriseerd` population for all three
      direct-read call sites, including the "no zaak" / "not zaakspecifiekGeautoriseerd" default cases.
      Unaffected by the role-semantics correction in task groups 4-7 below (the field is about the zaak, not
      the role).

## 4. Rename the role: zaakspecifiek_autorisatie_behandelaar → zaakspecifiek_geautoriseerd

- [x] 4.1 Rename the Rego role constant in `src/main/resources/policies/rollen.rego` from
      `zaakspecifiekAutorisatieBehandelaar` (role string `zaakspecifiek_autorisatie_behandelaar`) to
      `zaakspecifiekGeautoriseerd` (role string `zaakspecifiek_geautoriseerd`).
- [x] 4.2 Rename the corresponding PABC application role in
      `scripts/docker-compose/imports/pabc-database/json-mapping/pabc-mapping-data.json` (the
      `applicationRoles[]` entry's `name` field, id `1a2b3c4d-5e6f-4708-9a0b-c1d2e3f4a5b6`) from
      `zaakspecifiek_autorisatie_behandelaar` to `zaakspecifiek_geautoriseerd`, so the role string PABC
      hands out and the role string OPA checks for stay identical. Do not rename Keycloak's functional
      role/group names (e.g. `zaakspecifiek_autorisatie_behandelaar_test_1`) — they are independent,
      free-text labels unaffected by this change.
- [x] 4.3 Extend the "zaakspecifiek autorisatie behandelaars test 1" functional role's PABC mapping
      (functionalRoleId `2b3c4d5e-6f78-4901-ab0c-d1e2f3a4b5c6`, domain `domein_test_1`) so it grants
      **both** `behandelaar` and `zaakspecifiek_geautoriseerd` (added mapping id
      `a1b2c3d4-e5f6-7890-abcd-ef1234567812`, `applicationRoleId` = behandelaar's
      `a43e878d-a08f-4102-9d6c-9aa58299581b`), so this one functional role is self-sufficient per the "flag
      needs a normal role" model, instead of relying on the test user's separate, independent membership of
      the `/behandelaars-test-1` Keycloak group for its `behandelaar` grant.
- [x] 4.4 Remove the `zaakspecifiekautorisatiebehandelaar1` test user's membership of the
      `/behandelaars-test-1` Keycloak group (`scripts/docker-compose/imports/keycloak/realms/zaakafhandelcomponent-realm.json`),
      leaving them only in `/zaakspecifiek_autorisatie_behandelaars_test_1`, so the itest suite genuinely
      exercises the new PABC mapping from task 4.3 for this user's `behandelaar` grant, rather than getting
      it "for free" from an unrelated group membership.

## 5. OPA policy: zaak-rechten.rego — gate access, do not grant the flag its own rights

- [x] 5.1 Import `zaakspecifiekGeautoriseerd` from `data.net.atos.zac.rol.zaakspecifiekGeautoriseerd` in
      `src/main/resources/policies/zaak-rechten.rego`.
- [x] 5.2 Add a `default zaak_allowed := false` rule set: true when `not zaak.zaakspecifiekGeautoriseerd`, or
      when the user holds `zaakspecifiekGeautoriseerd`. Do not reference `recordmanager`/`beheerder` in this
      rule — their access to a zaakspecifiek geautoriseerde zaak is out of scope for this change.
- [x] 5.3 Add `zaak_allowed` as an extra condition only to the rule bodies that grant `raadpleger`,
      `behandelaar`, and/or `coordinator` a permission (`lezen`, the `{behandelaar, coordinator}` body of
      `wijzigen` and `toekennen`, `behandelen`, `afbreken`, `wijzigen_doorlooptijd`, `verlengen`,
      `opschorten`, `hervatten`, `creeren_document`, the `{behandelaar, coordinator}` body of
      `toevoegen_document` and `koppelen`, `versturen_email`, `versturen_ontvangstbevestiging`, the
      `{behandelaar, coordinator}` body of `toevoegen_initiator_persoon`, `toevoegen_initiator_bedrijf`,
      `verwijderen_initiator`, `toevoegen_betrokkene_persoon`, `toevoegen_betrokkene_bedrijf`,
      `verwijderen_betrokkene`, `toevoegen_bag_object`, `starten_taak`, `vastleggen_besluit`,
      `verlengen_doorlooptijd`). Split any rule that currently combines `recordmanager`/`beheerder` into the
      same body as `raadpleger`/`behandelaar`/`coordinator` into two bodies, so the gate applies only to the
      non-privileged half. Leave the `recordmanager`/`beheerder` bodies (and the `recordmanager`/`beheerder`-
      only rules `heropenen`, `bekijken_zaakdata`, `brondatum_zetten`) completely untouched. `wijzigen_locatie`
      is derived from `wijzigen` and needs no separate change.
- [x] 5.4 Do **not** add `zaakspecifiekGeautoriseerd` to any `some role in {...}` set. The flag only appears
      in the `zaak_allowed` gate (task 5.2) — a medewerker's rights on a zaakspecifiek geautoriseerde zaak
      come entirely from whichever normal role they separately hold; the flag never grants rights by itself.

## 6. OPA policy: taak-rechten.rego and document-rechten.rego

- [x] 6.1 Mirror task group 5 in `taak-rechten.rego` (`input.taak`): import the renamed role, add
      `zaak_allowed`, gate/split only the `raadpleger`/`behandelaar`/`coordinator` rule bodies, do not add
      the role to any role set, leave `recordmanager`/`beheerder` bodies untouched.
- [x] 6.2 Mirror task group 5 in `document-rechten.rego` (`input.document`).

## 7. Rego unit tests

- [x] 7.1 In `src/test/resources/policies/zaak-rechten_test.rego`, `taak-rechten_test.rego`, and
      `document-rechten_test.rego`, cover: the flag alone (no normal role) grants nothing, on both a
      geautoriseerde and a non-geautoriseerde zaak/taak/document; a normal role alone (without the flag) is
      denied on a geautoriseerde zaak/taak/document; a normal role combined with the flag is allowed,
      identical to that role's rights on a non-geautoriseerde zaak/taak/document; `recordmanager`/
      `beheerder` are unaffected. Do not add assertions about `recordmanager`/`beheerder` behaviour for the
      zaakspecifiek geautoriseerde case — out of scope for this change.
- [x] 7.2 Run `opa test` locally (or via the `opa-tests` docker-compose service,
      `docker-compose.yaml:386-393`) to confirm all Rego tests pass — 470/470 passing.

## 8. Documentation

- [x] 8.1 Add `zaakspecifiek_geautoriseerd` to the application roles table in
      `docs/solution-architecture/accessControlPolicies.md`, describing it as a flag that extends a normal
      application role's rights to zaakspecifiek geautoriseerde zaken, and which grants no rights on its
      own.
- [x] 8.2 Do **not** add a `zaakspecifiek_geautoriseerd` column to the permission matrix table — it grants
      no permission on its own, so it has no checkmarks of its own to show. The matrix table itself is
      otherwise left exactly as it was before this change.
- [x] 8.3 Add a note to `accessControlPolicies.md` explaining the flag mechanism: a `behandelaar`/
      `raadpleger`/`coordinator` checkmark does not apply to a zaakspecifiek geautoriseerde zaak (or its
      taken/documenten) unless that user also holds `zaakspecifiek_geautoriseerd` for the zaaktype, in which
      case that role's own checkmarks also apply to such zaken. Do not claim that `recordmanager`/
      `beheerder` access to a zaakspecifiek geautoriseerde zaak is specified or restricted by this change —
      leave that to a follow-up story.

## 9. Integration tests

- [x] 9.1 `src/itest/kotlin/nl/info/zac/itest/ZaakRestServiceTest.kt` creates a zaak marked zaakspecifiek
      geautoriseerd via its `ZAAK_GEAUTORISEERD` zaakeigenschap (new `OpenZaakClient.createZaakeigenschap`
      helper; Open Zaak's `ZAAK_GEAUTORISEERD` eigenschap already exists on "Test zaaktype 2" /
      `ZAAKTYPE_CMMN_TEST_2_UUID` in the docker-compose seed data), then verifies `readZaak` returns
      `HTTP_FORBIDDEN` for `BEHANDELAAR_1` (behandelaar, no flag) and `HTTP_OK` for
      `ZAAKSPECIFIEK_AUTORISATIE_BEHANDELAAR_1`. No itest code changes were needed for the role-semantics
      correction: this fixture user gets both a normal `behandelaar` role and the flag for "Test zaaktype 2"
      from the single PABC mapping added in task 4.3 (confirmed via the ZAC login log emitted during the
      itest run), after task 4.4 removed their unrelated `/behandelaars-test-1` group membership so the
      test genuinely exercises that mapping rather than an incidental second path to `behandelaar`.
- [x] 9.2 `TaskRestServiceZaakspecifiekAutorisatieTest.kt` — same pattern for `GET taken/{taskId}`
      (`assertPolicy(...lezen)`, `HTTP_FORBIDDEN`/`HTTP_OK`). No itest code changes needed, for the same
      reason as 9.1.
- [x] 9.3 `EnkelvoudigInformatieObjectRestServiceZaakspecifiekAutorisatieTest.kt` — same pattern for
      `GET informatieobjecten/informatieobject/{uuid}`, asserting on the `rechten.lezen` field (this
      endpoint returns HTTP 200 with reduced content rather than 403 on denial — pre-existing behaviour, not
      introduced by this change). No itest code changes needed, for the same reason as 9.1.
- [x] 9.4 Update two pre-existing `IdentityRestServiceTest.kt` assertions that were exact-match/membership
      fixtures affected by tasks 4.3/4.4: "Getting authorised behandelaar groups for a zaaktype" now also
      expects `GROUP_ZAAKSPECIFIEK_AUTORISATIE_BEHANDELAARS_TEST_1` in the result (that group's functional
      role is now also mapped to `behandelaar` for domein_test_1, per task 4.3), and "Getting users in a
      group" no longer expects `ZAAKSPECIFIEK_AUTORISATIE_BEHANDELAAR_1` among `GROUP_BEHANDELAARS_TEST_1`'s
      members (that user is no longer in that group, per task 4.4). Both are pre-existing tests unrelated to
      this change's own new specs; they needed updating because tasks 4.3/4.4 changed the PABC/Keycloak
      seed data these tests assert against.
- [x] 9.5 Re-run `./gradlew itest --info` after the role rename (task group 4) and rego changes (task groups
      5-6) to confirm the three new specs and the full suite pass end to end against the real
      PABC/Keycloak/OPA stack with the renamed role and corrected PABC mapping.

## 10. Verification

- [x] 10.1 Ran `./gradlew spotlessApply detektApply` and `./gradlew test` after the role-semantics
      correction — clean, no findings, all unit tests pass.
- [x] 10.2 Ran `./gradlew itest --info` (ZAC Docker image rebuilt) to confirm the full integration test
      suite passes end to end with the renamed role and corrected flag semantics — `BUILD SUCCESSFUL`,
      348/348 tests passing, 0 failures/errors.
