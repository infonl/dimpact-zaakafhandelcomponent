## 1. Layer `admin-lists`: Inrichtingscheck and Zaakafhandelparameters lists

- [x] 1.1 Add `RestInrichtingscheckZaaktype` (`uuid`, `omschrijving`, `doel`, `beginGeldigheid`) with a
      `ZaakType.toRestInrichtingscheckZaaktype()` converter, and make `RestZaaktypeInrichtingscheck.zaaktype` that
      type in `HealthCheckRestService.convertToREST`. Verify with a unit test in `HealthCheckRestServiceTest` that
      the zaaktype of a check carries exactly these four values and that every check result is still copied.
- [x] 1.2 Add `RestZaaktypeConfigurationOverzicht` with `RestZaaktypeBeheerOverzicht` and
      `RestProcessDefinitionOverzicht` (design D1), and a converter function that builds it from the `ZaakType` in
      hand and the stored or unsaved `ZaaktypeConfiguration`, taking `naam` from
      `RestProcessDefinitionConverter.toRestProcessDefinition(binding, inclusiefRelaties = false)` (design D4).
      Verify with unit tests: a saved CMMN configuration, a saved BPMN configuration, and an unsaved configuration
      (no process definition, no creatiedatum, `isValide` false).
- [x] 1.3 Make `ZaaktypeConfigurationRestService.listZaaktypeConfigurations` return
      `List<RestZaaktypeConfigurationOverzicht>` through 1.2, keeping the beheren check. Verify with
      `src/test/kotlin/nl/info/zac/app/admin/ZaaktypeConfigurationRestServiceTest.kt` that the list no longer reads
      the zaaktype again nor calls `isZaakspecifiekAutoriseerbaar`, and that the read of one configuration still
      returns the full `RestZaaktypeConfiguration`.
- [x] 1.4 Regenerate the OpenAPI spec (`./gradlew generateOpenApiSpec`) and the frontend types, and check that only
      the two list responses changed.
- [x] 1.5 Update `InrichtingscheckComponent` and `ParametersComponent` (and `ZaakafhandelParametersListParameters`,
      `compareZaaktype`) to the new generated types without changing property paths. Verify with
      `npm test -- --test-path-patterns 'inrichtingscheck|parameters.component|zaakafhandel-parameters.service'` and
      `npx ngc -p tsconfig.app.json --noEmit`; update the mocks in those specs to the new list item types.
- [x] 1.6 Update `HealthCheckRestServiceTest` and the itests that read `GET /zaakafhandelparameters` to expect the
      slim items. Verify with `./gradlew itest` on those test classes.
- [x] 1.7 Run `./gradlew spotlessApply detektApply detekt test` and `npm run lint` in `src/main/app`, and fix what
      they report.

## 2. Layer `zaak-creation`: zaaktype choice and its settings

- [x] 2.1 Add `RestZaaktypeKeuze` (`uuid`, `omschrijving`) and `RestZaaktypeAanmaakInstellingen` (`uuid`,
      `omschrijving`, `vertrouwelijkheidaanduiding`, `defaultGroepId`, `defaultBehandelaarId`,
      `isKvkKoppelenEnabled`, `isBrpKoppelenEnabled`) with converters from `ZaakType` and `ZaaktypeConfiguration`.
      Verify with unit tests of the converters.
- [x] 2.2 Add `ZaaktypeRestService` for `/zaaktypen` (design D6) and move `listZaaktypesForZaakCreation` from
      `ZaakRestService` into it as `GET /zaaktypen` with the `startbaar` filter. Extract the filter into one check
      (design D2), and return `List<RestZaaktypeKeuze>`. Verify with `ZaaktypeRestServiceTest` that `startbaar=true`
      offers the same zaaktypes, that the collection without a filter lists the whole catalogus, that
      `startbaar=false` lists the rest, and that each item has only `uuid` and `omschrijving`.
- [x] 2.3 Add `GET /zaaktypen/{zaaktypeUuid}` returning `RestZaaktypeAanmaakInstellingen`: HTTP
      403 when the user may not start a zaak of the zaaktype, HTTP 404 when it is unknown or not offered. Verify with
      unit tests for an offered zaaktype, a zaaktype the user may not start, and a zaaktype with an invalid
      configuration.
- [x] 2.4 Regenerate the OpenAPI spec and the frontend types; check that `RestZaak.zaaktype` is unchanged.
- [x] 2.5 Add `ZakenService.readZaaktypeForCreationQuery(uuid)` next to `listZaaktypesForCreationQuery`, and change
      `zaaktypenForKoppeling` and `initiatorAllowedByZaaktype` in `zaak-create-koppeling.ts` to the list item and the
      settings type. Verify with their unit specs.
- [x] 2.6 Rework `ZaakCreateComponent` (design D3): the zaaktype control holds a `RestZaaktypeKeuze`; an
      `injectQuery` on the settings of the chosen UUID; apply vertrouwelijkheidaanduiding, groep enable and default
      groep, default behandelaar, initiator enable/clear and the copied initiator when the settings of the still
      chosen zaaktype arrive; disable submit until then; send `zaaktype: { uuid }` on create. Verify with
      `npm test -- --test-path-patterns zaak-create`, covering each scenario of the requirement "The Zaak aanmaken
      form applies the settings of the chosen zaaktype after the choice", including a quick switch between two
      zaaktypes.
- [x] 2.7 Update `ZaakLinkComponent` and its spec to the new list item type; verify the deelzaak filter and the
      "no zaaktype available" message with `npm test -- --test-path-patterns zaak-link`.
- [x] 2.8 Update `ZaakRestServiceTest` in `src/itest` for `GET /zaaktypen?startbaar=true` and add itests for the read endpoint
      (200 with settings, 403, 404). Verify with `./gradlew itest` on that class.
- [x] 2.9 Run the e2e features for zaak aanmaken and gekoppelde zaak aanmaken against the local stack
      (`./start-e2e-with-local-env.sh`), or, when no stack is available, state in the pull request that they were not
      run.
- [x] 2.10 Run `./gradlew spotlessApply detektApply detekt test` and `npm run lint` and `npx ngc -p tsconfig.app.json
      --noEmit` in `src/main/app`, and fix what they report.

## 3. Layer `configuration-cache`: the cache of zaaktype configurations

- [ ] 3.1 Set the limit of the caches in `ZaaktypeConfigurationService` to 2000 entries and let an entry expire one
      hour after it was written (design D7). Verify with a unit test in `ZaaktypeConfigurationServiceTest` that the
      configurations of 500 zaaktypes, each found twice, are each read from the database only once.
- [ ] 3.2 Measure the three lists against a local stack with 200 zaaktypes, before and after the change, and
      record the response times under "Measurements" in design.md.
- [ ] 3.3 Run `./gradlew spotlessApply detektApply detekt test`, and fix what they report.
