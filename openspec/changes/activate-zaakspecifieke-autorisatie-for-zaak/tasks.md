## 1. Access control: the current-behandelaar exception

- [ ] 1.1 Add `behandelaarIsLoggedInUser: Boolean` to `ZaakData`, `TaakData` and `DocumentData` in
      `nl.info.zac.policy.input`, and populate it in every `PolicyService.readZaakRechten` /
      `readTaakRechten` / `readDocumentRechten` overload by comparing the logged-in user's id against the
      zaak's behandelaar-medewerker rol (`ZgwApiService.findBehandelaarMedewerkerRoleForZaak`). Short-circuit
      the lookup to zaken that are actually zaakspecifiek geautoriseerd, as decided in design.md. Verify with
      `PolicyServiceTest` cases asserting the field is `true` only for the behandelaar of a flagged zaak and
      that no rollen lookup happens for an unflagged zaak.
- [ ] 1.2 Add `zaak_allowed if { zaak.behandelaarIsLoggedInUser }` (and the taak/document equivalents) to
      `zaak-rechten.rego`, `taak-rechten.rego` and `document-rechten.rego`, keeping the existing comment above
      each guard accurate. Verify with new cases in `zaak-rechten_test.rego`, `taak-rechten_test.rego` and
      `document-rechten_test.rego` covering: behandelaar without the flag is allowed on their own flagged
      zaak, its taak and its document; is denied on another flagged zaak of the same zaaktype; and gets
      nothing when they hold no application role at all.
- [ ] 1.3 Populate `behandelaarIsLoggedInUser` in the three zoekobject-based `PolicyService` overloads
      (`readZaakRechtenForZaakZoekObject`, `readTaakRechten(TaakZoekObject)`,
      `readDocumentRechten(DocumentZoekObject)`) from the new indexed field added in task 2.2, so werklijst
      rechten match detail-view rechten. Verify with `PolicyServiceTest` cases per zoekobject type.

## 2. Search index: the zaak's behandelaar

- [ ] 2.1 Add `zaakBehandelaarGebruikersnaam` field constants to `ZoekObject`, `ZaakZoekObject`,
      `TaakZoekObject` and `DocumentZoekObject` (`zaak_`/`taak_`/`informatieobject_` prefixed per-type field
      plus the shared copyField target), named distinctly from `TaakZoekObject.BEHANDELAAR_ID_FIELD` as
      decided in design.md. Verify the constants compile and the shared name is used by exactly one field per
      type.
- [ ] 2.2 Populate the new field in `ZaakZoekObjectConverter`, `TaakZoekObjectConverter` and
      `DocumentZoekObjectConverter`, resolving the zaak's behandelaar through the same memoized-lookup
      function parameter that `isZaakspecifiekGeautoriseerd` already uses, so indexing all taken of one zaak
      costs one rollen lookup. Verify with converter unit tests asserting the field for a zaak with and
      without a behandelaar.
- [ ] 2.3 Add `SolrSchemaV9` adding the four fields plus three copyFields, mirroring `SolrSchemaV8`, with
      `teHerindexerenZoekObjectTypes = emptySet()` and a comment stating the reindex is manual per
      environment. Verify with a `SolrSchemaV9Test` matching the existing `SolrSchemaV*` test pattern.
- [ ] 2.4 Widen `SearchService.getZaakspecifiekGeautoriseerdFilterQuery()` so each per-zaaktype exclusion
      clause also excludes rows whose `zaakBehandelaarGebruikersnaam` is the logged-in user. Verify with
      `SearchServiceTest` cases asserting the generated filter query string for: a user with no flag anywhere,
      a user holding the flag as an overall role, and the mixed zaaktype-A/zaaktype-B case.

## 3. Activation write path

- [ ] 3.1 Add a `ZrcClientService`-based helper alongside `isZaakspecifiekGeautoriseerd` in
      `zrc/util/ZaakspecifiekGeautoriseerd.kt` that creates the `ZAAK_GEAUTORISEERD` zaakeigenschap with value
      `true`, resolving the eigenschap URL via `ZtcClientService.findEigenschap(zaaktypeUrl,
      ZAAK_GEAUTORISEERD_EIGENSCHAP_NAAM)`, and that is a no-op when the zaak already carries it. Verify with
      unit tests for the create, the idempotent no-op, and the missing-zaaktype-eigenschap case.
- [ ] 3.2 Add `isZaakspecifiekGeautoriseerd: Boolean?` to `RestZaakCreateData` with the tri-state semantics
      from design.md (`null` = leave alone), and regenerate the OpenAPI spec and frontend types
      (`./gradlew generateOpenApiSpec` and the frontend type generation). Verify the generated
      `zac-openapi-types.d.ts` carries the new optional field.
- [ ] 3.3 Add four exception types under `nl.info.zac.app.zaak.exception`, each extending
      `InputValidationFailedException` with its own new `ErrorCode` entry whose `value` is an i18n key
      (`msg.error.zaak....`): zaaktype not zaakspecifiek autoriseerbaar, zaak has no behandelaar, employee may
      not activate, and zaakspecifieke autorisatie cannot be lifted. The backend carries the key only — no
      message text in any language. Verify with unit tests asserting each exception maps to its own code
      through `RestExceptionMapper`.
- [ ] 3.4 Add the two exception types for the behandelaar restrictions (release refused, reassignment
      refused) the same way, with their own `ErrorCode` entries. Verify the same way as 3.3.
- [ ] 3.5 Add the Dutch and English translations for every new error code to
      `src/main/app/src/assets/i18n/nl.json` and `en.json` (kebab-case last key segment), wording the Dutch
      entries to satisfy PZ-10200's "heldere Nederlandstalige tekst". Verify every new key resolves in both
      files and that no key exists in one file but not the other.
- [ ] 3.6 Implement the activation branch in `ZaakRestService.updateZaak`: evaluate all four preconditions
      before any Open Zaak call, then `patchZaak` first and create the zaakeigenschap last, per design.md.
      Verify with `ZaakRestServiceTest` cases for the happy path, the combined assign-and-activate path, each
      of the four rejections leaving the zaak untouched, and the idempotent re-activation.
- [ ] 3.7 Trigger a direct reindex of the zaak, its taken and its documenten on successful activation, rather
      than relying on the `zaakeigenschap` notificatie. Verify with a `ZaakRestServiceTest` case asserting the
      indexing calls.

## 4. Locking the behandelaar of a flagged zaak

- [ ] 4.1 Reject removing or changing the behandelaar of a zaakspecifiek geautoriseerde zaak in the
      synchronous endpoints `ZaakAssignAndReleaseRestService.assignZaak`, `assignZaakToLoggedInUser` and
      `assignZaakToLoggedInUserFromList`, and in `ZaakRestService.updateZaak` when the request carries a
      different `groep`/`behandelaar`, each throwing the appropriate exception from task 3.4. Verify with unit
      tests per endpoint, including the no-op case where the request repeats the current behandelaar.
- [ ] 4.2 Skip zaakspecifiek geautoriseerde zaken in `ZaakService.releaseZaken` and in the batch assignment
      path behind `assignFromList`, emitting `ScreenEventType.ZAAK_ROLLEN.skipped(zaak)` the way the existing
      not-open filter does. Verify with `ZaakServiceTest` cases asserting the flagged zaak keeps its
      behandelaar, the screen event is sent, and unflagged zaken in the same batch are still processed.
- [ ] 4.3 Expose `isZaakspecifiekGeautoriseerd` on the werklijst row so the frontend can name the skip reason:
      add it to the zaak zoekobject REST model and to the frontend `ZaakZoekObject` type, sourced from the
      already-indexed Solr field. Verify with a converter/REST unit test and by regenerating the OpenAPI types.
- [ ] 4.4 Surface the refusal on the zaakdetailpagina: the confirmation/refusal dialog per the Figma design
      when an employee tries to release or reassign a zaakspecifiek geautoriseerde zaak. Verify with component
      specs using Testing Library role-based queries, per the project's spec conventions.
- [ ] 4.5 Partition the selection in `zaken-werkvoorraad.component.ts` before dispatching the batch, splitting
      out zaken that are zaakspecifiek geautoriseerd and zaken that are already afgehandeld, and report the
      two counts as separate translated messages in the verdelen and vrijgeven flows — following the existing
      pre-filter for zaken without a behandelaar rather than adding a reason to the `SKIPPED` websocket
      payload, as decided in design.md. Add the i18n keys to `nl.json` and `en.json` with kebab-case last
      segments. Verify with component specs covering: only-flagged skipped, both reasons skipped, and nothing
      skipped (no message shown).

## 5. Frontend activation control

- [ ] 5.1 Add the activation control to `zaak-details-wijzigen.component`, shown only when the zaak's zaaktype
      is zaakspecifiek autoriseerbaar (`RestZaaktypeConfiguration.zaakspecifiekAutoriseerbaar`) and disabled
      once the zaak is already flagged, sending `isZaakspecifiekGeautoriseerd` on submit. Verify with
      component specs covering: control hidden for a non-autoriseerbaar zaaktype, shown and enabled for an
      unflagged eligible zaak, and shown but not un-settable for an already-flagged zaak.
- [ ] 5.2 Confirm no change is needed to the lock indicator: `zaak-details-card.component.html` already
      renders it from `isZaakspecifiekGeautoriseerd`. Verify the existing `zaak-details-card.component.spec.ts`
      still passes and note the PZ-10201 acceptance criterion as already met.

## 6. Configuration, documentation and integration tests

- [ ] 6.1 Map the recordmanager and beheerder functional roles to the `zaakspecifiek_geautoriseerd`
      application role in `scripts/docker-compose/imports/pabc-database/json-mapping/pabc-mapping-data.json`
      and the Keycloak realm, per the design decision to handle PZ-10203 by configuration. Verify by starting
      the Docker Compose stack and confirming the roles appear in the `UserPrincipalFilter` log line for a
      recordmanager test user.
- [ ] 6.2 Update `docs/solution-architecture/accessControlPolicies.md` with the current-behandelaar exception
      and the note that recordmanager/beheerder access is arranged through PABC rather than a policy branch.
      Verify by reading the section against the spec's documentation requirement.
- [ ] 6.3 Add an integration test alongside the existing `*ZaakspecifiekAutorisatie*Test.kt` family covering
      activation end to end: a behandelaar activates their zaak, still reads it and finds it in a werklijst
      without holding the flag, while a colleague with the same application role can neither open it nor find
      it. Verify with `./gradlew itest`.
- [ ] 6.4 Extend the integration tests to cover the refusals: activation without a behandelaar, activation on
      a non-autoriseerbaar zaaktype, activation by an unauthorised employee, attempted deactivation, attempted
      release and attempted reassignment (single and batch), and a batch in which the flagged zaken are
      skipped while the unflagged ones are still processed. Verify with `./gradlew itest`.
- [ ] 6.5 Add integration tests for the recordmanager/beheerder access path, which this change moves entirely
      into PABC configuration and which is therefore invisible to the unit tests. Cover: (a) a recordmanager
      and a beheerder with the seeded mapping can open a flagged zaak, its taken and its documenten, and find
      them in werklijsten and zoekresultaten; (b) a recordmanager whose zaaktype lacks the mapping is refused
      with the generic insufficient-rights response, proving the mapping — not a policy branch — is what
      grants the access. Verify with `./gradlew itest`.
- [ ] 6.6 Add an integration test for the recovery path of a flagged zaak whose behandelaar was removed
      out-of-band: strip the behandelaar rol directly in Open Zaak, then assert that a recordmanager with the
      mapping can still open the zaak and assign a new behandelaar, and that the new behandelaar then reaches
      the zaak through the behandelaar exception without holding the flag. Verify with `./gradlew itest`.
- [ ] 6.7 Add the PABC mapping to the deployment notes as a prerequisite, and record the manual reindex step
      with the exact calls: `GET /rest/internal/indexeren/herindexeren/ZAAK`, then `/TAAK`, then `/DOCUMENT`
      on `IndexingAdminRestService` (API-key authenticated, `202 Accepted`, `409 Conflict` while one of that
      type is already running). Verify the notes name the endpoint and the per-type order.
- [ ] 6.8 Run `./gradlew spotlessApply detektApply build` and `./scripts/lint-changed-files.sh`, and verify
      the full unit test suite and the frontend `npm test` pass.
