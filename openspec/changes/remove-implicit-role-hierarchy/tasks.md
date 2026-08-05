## 1. Flatten OPA policy grants (`src/main/resources/policies/`)

Apply the mechanical rule from design.md Decision 1 to every clause: whichever role is checked today, add an identical clause for every role to its right in `raadpleger < behandelaar < coordinator < recordmanager < beheerder`.

- [ ] 1.1 `zaak-rechten.rego`: `lezen` (raadpleger-only today) gains explicit `behandelaar`/`coordinator`/`recordmanager`/`beheerder` clauses.
- [ ] 1.2 `zaak-rechten.rego`: `wijzigen` — add `coordinator` to the `behandelaar`+`zaak.open` clause; add `beheerder` to the `recordmanager` clause.
- [ ] 1.3 `zaak-rechten.rego`: `toekennen` — same shape as `wijzigen` (add `coordinator` to the `behandelaar` clause, `beheerder` to the `recordmanager` clause).
- [ ] 1.4 `zaak-rechten.rego`: `afbreken` (behandelaar-only) gains `coordinator`/`recordmanager`/`beheerder`; `heropenen` (recordmanager-only) gains `beheerder`.
- [ ] 1.5 `zaak-rechten.rego`: `wijzigen_doorlooptijd`, `verlengen`, `opschorten`, `hervatten`, `creeren_document`, `versturen_email`, `versturen_ontvangstbevestiging`, `starten_taak`, `vastleggen_besluit`, `verlengen_doorlooptijd` (all behandelaar-only) each gain `coordinator`/`recordmanager`/`beheerder` clauses (same conditions as the existing behandelaar clause).
- [ ] 1.6 `zaak-rechten.rego`: `toevoegen_document`, `koppelen`, `toevoegen_initiator_persoon`, `toevoegen_initiator_bedrijf`, `verwijderen_initiator`, `toevoegen_betrokkene_persoon`, `toevoegen_betrokkene_bedrijf`, `verwijderen_betrokkene`, `toevoegen_bag_object` — add `coordinator` to the behandelaar clause, `beheerder` to the recordmanager clause. Leave `bekijken_zaakdata` (beheerder-only) and `wijzigen_locatie` (derived from `wijzigen`) unchanged.
- [ ] 1.7 `taak-rechten.rego`: `lezen` (raadpleger-only) gains `behandelaar`/`coordinator`/`recordmanager`/`beheerder`; `wijzigen`, `toekennen`, `creeren_document`, `toevoegen_document` (all behandelaar-only) each gain `coordinator`/`recordmanager`/`beheerder`.
- [ ] 1.8 `document-rechten.rego`: `lezen` and `downloaden` (raadpleger-only) gain `behandelaar`/`coordinator`/`recordmanager`/`beheerder`.
- [ ] 1.9 `document-rechten.rego`: `wijzigen`, `verwijderen`, `ontgrendelen`, `toevoegen_nieuwe_versie`, `verplaatsen`, `ontkoppelen` — add `coordinator` to the behandelaar clause, `beheerder` to the recordmanager clause.
- [ ] 1.10 `document-rechten.rego`: `vergrendelen`, `ondertekenen`, `converteren` (behandelaar-only, no existing recordmanager clause) each gain `coordinator`/`recordmanager`/`beheerder` clauses.
- [ ] 1.11 `werklijst-rechten.rego`: `inbox` and `zaken_taken_verdelen` (coordinator-only) gain `recordmanager`/`beheerder`; `ontkoppelde_documenten_verwijderen` and `inbox_productaanvragen_verwijderen` (recordmanager-only) gain `beheerder`; `zaken_taken` (raadpleger-only) gains `behandelaar`/`coordinator`/`recordmanager`/`beheerder`. Leave `zaken_taken_exporteren` (beheerder-only) unchanged.
- [ ] 1.12 `notitie-rechten.rego`: `lezen` (raadpleger-only) gains `behandelaar`/`coordinator`/`recordmanager`/`beheerder`; `wijzigen` (behandelaar-only) gains `coordinator`/`recordmanager`/`beheerder`.
- [ ] 1.13 `overige-rechten.rego`: `starten_zaak` (behandelaar-only) gains `coordinator`/`recordmanager`/`beheerder`; `zoeken` (raadpleger-only) gains `behandelaar`/`coordinator`/`recordmanager`/`beheerder`. Leave `beheren` (beheerder-only) unchanged.
- [ ] 1.14 Import the newly-referenced role constants (`data.net.atos.zac.rol.coordinator`, `.recordmanager`, `.beheerder`, `.behandelaar` as needed) at the top of each `.rego` file touched above.

## 2. Extend Rego unit tests (`src/test/resources/policies/`)

- [ ] 2.1 In `zaak-rechten_test.rego`, `taak-rechten_test.rego`, `document-rechten_test.rego`, `werklijst-rechten_test.rego`, `notitie-rechten_test.rego`, `overige-rechten_test.rego`: add a positive test case per newly-added clause from section 1, asserting `true` for a user holding *only* the newly-granted role (no other role in `user.rollen`).
- [ ] 2.2 Add a negative test case confirming `raadpleger`-only users still cannot perform `behandelaar`/`coordinator`/`recordmanager`/`beheerder`-gated actions (e.g. `zaak-rechten.wijzigen`), to guard against over-broadening a rule.
- [ ] 2.3 Run `./gradlew test --tests "*RechtenTest*"` (or the project's OPA test task) and confirm all pass.

## 3. Flatten local PABC test mapping data

- [ ] 3.1 In `scripts/docker-compose/imports/pabc-database/json-mapping/pabc-mapping-data.json`, for every functional test role currently mapped to more than one of `raadpleger`/`behandelaar`/`coordinator`/`recordmanager`/`beheerder`, remove every mapping entry except the one matching that role's own name (e.g. `coordinator_domein_test_1` keeps only its `coordinator` mapping; `beheerder_elk_domein` keeps only `beheerder`). Leave `brp_zoeken` mappings untouched (non-goal, see design.md).
- [ ] 3.2 While flattening, double check the sparse existing mappings for `coordinator_domein_test_2` and `recordmanager_domein_test_2` (currently only mapped to `brp_zoeken`, no own-role mapping at all) — confirm whether this is intentional test fixture data or a pre-existing gap, and fix if it's a gap so every functional role ends up mapped to exactly its own application role.
- [ ] 3.3 Re-import the mapping data into the local PABC database (per `docs/development` / docker-compose setup) and confirm the PABC UI/API reflects the flattened mappings.

## 4. Update permission matrix documentation

- [ ] 4.1 In `docs/solution-architecture/accessControlPolicies.md`, remove the "lower-level roles" narrative bullet list describing role inheritance.
- [ ] 4.2 Update the permission table: add a ✅ in every column for which section 1 added an explicit Rego grant, keeping the existing conditional/italic notes style. Do not add a ✅ under `brp_zoeken` for any row purely due to the removed hierarchy.
- [ ] 4.3 Proofread the full table against the final `.rego` files (grep each permission name in the corresponding policy file) to confirm doc and code agree exactly.

## 5. Update integration tests for behandelaar-groups endpoints

- [ ] 5.1 In `IdentityRestServiceTest.kt`, update the "Getting authorised behandelaar groups for a zaaktype" test: remove `GROUP_BEHEERDERS_ELK_DOMEIN`, `GROUP_COORDINATORS_TEST_1`, `GROUP_RECORDMANAGERS_TEST_1` from the expected response, keeping only `GROUP_BEHANDELAARS_TEST_1` and `GROUP_BEHANDELAARS_LONG_NAME_TEST`.
- [ ] 5.2 In `IdentityRestServiceTest.kt`, update the "Getting authorised behandelaar groups for multiple zaaktypes" test: expect an empty list, since `beheerder_elk_domein` no longer carries the `behandelaar` application role.
- [ ] 5.3 Add a new test scenario asserting that a coordinator-only (or recordmanager-only, or beheerder-only) group never appears in either the single-zaaktype or multi-zaaktype behandelaar-groups response, directly covering PZ-11245's acceptance criterion.
- [ ] 5.4 Review `ZaakRestServiceTest.kt` and `TaskRestServiceTest.kt` for any `COORDINATOR_1`, `RECORDMANAGER_1`, or `BEHEERDER_1` test-user scenario that implicitly relied on inherited raadpleger/behandelaar access; confirm they still pass unmodified after sections 1 and 3, since effective permissions are unchanged.

## 6. Full verification

- [ ] 6.1 Run `./gradlew spotlessApply detektApply` on any touched Kotlin test files.
- [ ] 6.2 Build a fresh ZAC Docker image and run `./gradlew itest --info`, confirming `IdentityRestServiceTest`, `ZaakRestServiceTest`, and `TaskRestServiceTest` all pass.
- [ ] 6.3 Run `openspec validate remove-implicit-role-hierarchy --strict` before archiving.
