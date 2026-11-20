## 1. Baseline

- [ ] 1.1 Generate the OpenAPI specification on the base commit (`./gradlew generateOpenApiSpec`) and keep a copy of `build/generated/openapi/META-INF/openapi/openapi.json` for the comparison in 6.1
- [ ] 1.2 Record the numbers of `ZaakRestService.kt` from the churn vs complexity report on the base commit (`uv run .github/scripts/churn_complexity.py --frames none`): commits, total and worst-function complexity, functions, lines of code

## 2. Close the gaps in the safety net (against the current ZaakRestService)

- [ ] 2.1 Add unit tests for `addBetrokkene` to `ZaakRestServiceTest`: a BSN betrokkene with `canToevoegenBetrokkenePersoon`, a vestiging with `canToevoegenBetrokkeneBedrijf`, a policy refusal, and the default explanation when `roltoelichting` is empty
- [ ] 2.2 Add an integration test that deletes a belanghebbende betrokkene of a zaak with a reden through `DELETE /zaken/betrokkene/{uuid}` and then finds it no longer in `GET /zaken/zaak/{uuid}/betrokkene`
- [ ] 2.3 Add an integration test that deletes the initiator of a zaak with a reden through `DELETE /zaken/{uuid}/initiator` and then reads the zaak without an initiator
- [ ] 2.4 Run `./gradlew test` and the affected integration tests; all pass against the current code

## 3. Move the domain constants out of the REST layer

- [ ] 3.1 Declare `VESTIGING_IDENTIFICATIE_DELIMITER` in `nl.info.zac.zaak` with the same value, and import it from there in `ZaakService` and `ZaakRestService`
- [ ] 3.2 Declare `AANVULLENDE_INFORMATIE_TASK_NAME` next to `SuspensionZaakHelper` with the same value, and import it from there in `SuspensionZaakHelper` and `ZaakRestService`
- [ ] 3.3 Check that no file outside `nl.info.zac.app` imports `nl.info.zac.app.zaak.ZaakRestService`

## 4. Share the initiator policy check

- [ ] 4.1 Add `ZaakRechten.assertCanAddInitiator(IdentificatieType)` in `nl.info.zac.app.zaak`, with the policy check per identification type that the private `updateInitiator` does today
- [ ] 4.2 Make `createVestigingIdentificationString` a top-level function next to `VESTIGING_IDENTIFICATIE_DELIMITER`
- [ ] 4.3 Let the private `updateInitiator` in `ZaakRestService` use `assertCanAddInitiator`; run the "Creating a zaak" and "Updating an initiator" unit tests, which pass unchanged

## 5. Create ZaakBetrokkeneRestService

- [ ] 5.1 Create `src/main/kotlin/nl/info/zac/app/zaak/ZaakBetrokkeneRestService.kt` with the same class annotations as `ZaakBesluitRestService`, `@Path("zaken")`, and constructor injection of only the dependencies the five endpoints use
- [ ] 5.2 Move `addBetrokkene`, `deleteBetrokkene`, `deleteInitiator`, `listBetrokkenenVoorZaak` and `updateInitiator` verbatim, with their annotations, method names and signatures
- [ ] 5.3 Move the private helpers that only these endpoints use (`addBetrokkeneToZaak`, `composeBetrokkeneIdentification`, `removeBetrokkene`, `removeInitiator`) and the constants `ROL_VERWIJDER_REDEN` and `ROLTYPE_OMSCHRIJVINGEN_THAT_CANNOT_BE_DELETED_AS_BETROKKENE`; declare `ROL_TOEVOEGEN_REDEN`, which both services use, once next to `assertCanAddInitiator`
- [ ] 5.4 Remove the moved code, the imports and the injected dependencies that `ZaakRestService` no longer uses
- [ ] 5.5 Move the unit tests "Deleting a betrokkene", "Deleting an initiator", "Listing betrokkenen for a zaak", "Updating an initiator" and the `addBetrokkene` tests of 2.1 to `src/test/kotlin/nl/info/zac/app/zaak/ZaakBetrokkeneRestServiceTest.kt`, with only the mocks the new class needs and `checkUnnecessaryStub()`; their descriptions and assertions do not change

## 6. Verify

- [ ] 6.1 Generate the OpenAPI specification again and compare it byte for byte with the copy of 1.1; it is identical
- [ ] 6.2 Run `./gradlew spotlessApply detektApply`, then `./gradlew detekt` and `./gradlew test` (including `ArchitectureTest`); all pass
- [ ] 6.3 Run the integration tests (`./gradlew itest`); all pass, with no existing integration test changed
- [ ] 6.4 Check that the pull request changes nothing under `scripts/test-data/` and no file under `src/main/app/`
- [ ] 6.5 Record the numbers of `ZaakRestService.kt` and `ZaakBetrokkeneRestService.kt` from the report after the change in the pull request description, next to those of 1.2

## 7. Next phase

- [ ] 7.1 Once phase 1 is merged, rerun the report and open the change for phase 2 (zaak lifecycle), or reorder the remaining phases if the report shows a different priority
