## 1. Add unit tests for RESTHumanTaskParametersConverter

- [ ] 1.1 Create `RESTHumanTaskParametersConverterTest.kt` in `src/test/kotlin/net/atos/zac/app/admin/converter/`
- [ ] 1.2 Add test: `convertHumanTaskParametersCollection` with matching `ZaaktypeCmmnHumantaskParameters` returns fully populated `RESTHumanTaskParameters`
- [ ] 1.3 Add test: `convertHumanTaskParametersCollection` with no match returns default `RESTHumanTaskParameters` (actief=false)
- [ ] 1.4 Add test: `convertRESTHumanTaskParameters` converts single entry with all fields mapped correctly
- [ ] 1.5 Add test: `convertRESTHumanTaskParameters` converts multiple entries to same-size list

## 2. Add unit tests for RestHumanTaskReferenceTableConverter

- [ ] 2.1 Create `RestHumanTaskReferenceTableConverterTest.kt` in `src/test/kotlin/net/atos/zac/app/admin/converter/`
- [ ] 2.2 Use reflection to inject `ReferenceTableService` mock into private field
- [ ] 2.3 Add test: `convertDefault` calls `ReferenceTableService.readReferenceTable` and returns correct `RestHumanTaskReferenceTable`
- [ ] 2.4 Add test: `convert(Collection)` converts multiple `HumanTaskReferentieTabel` entries to REST
- [ ] 2.5 Add test: `convert(List<RestHumanTaskReferenceTable>)` converts back to domain objects, fetching tabel from service

## 3. Add unit tests for BetrokkeneIdentificatieValidator

- [ ] 3.1 Create `BetrokkeneIdentificatieValidatorTest.kt` in `src/test/kotlin/nl/info/zac/app/zaak/model/`
- [ ] 3.2 Add tests for BSN: valid (temporaryPersonId set, kvkNummer/vestigingsnummer blank), invalid (temporaryPersonId null), invalid (kvkNummer present)
- [ ] 3.3 Add tests for VN: valid, invalid (kvkNummer blank), invalid (temporaryPersonId set)
- [ ] 3.4 Add tests for RSIN: valid, invalid (vestigingsnummer present)
- [ ] 3.5 Add test: null input returns false

## 4. Add unit tests for PersonenQueryResponseJsonbDeserializer

- [ ] 4.1 Create `PersonenQueryResponseJsonbDeserializerTest.kt` in `src/test/kotlin/nl/info/client/brp/util/`
- [ ] 4.2 Add tests for all six supported `type` values using real `JsonParser` from JSON strings
- [ ] 4.3 Add test: unknown `type` throws `InputValidationFailedException`

## 5. Add unit tests for EnkelvoudigInformatieObjectLockService

- [ ] 5.1 Create `EnkelvoudigInformatieObjectLockServiceTest.kt` in `src/test/kotlin/nl/info/zac/enkelvoudiginformatieobject/`
- [ ] 5.2 Mock `EntityManager` (criteria builder chain) and `DrcClientService` using MockK
- [ ] 5.3 Add test: `createLock` calls DRC client, persists entity, flushes, returns lock
- [ ] 5.4 Add test: `findLock` returns lock when one exists
- [ ] 5.5 Add test: `findLock` returns null when none exists
- [ ] 5.6 Add test: `readLock` returns lock when it exists
- [ ] 5.7 Add test: `readLock` throws `EnkelvoudigInformatieObjectLockNotFoundException` when none exists
- [ ] 5.8 Add test: `deleteLock` unlocks via DRC client and removes entity when lock exists
- [ ] 5.9 Add test: `deleteLock` does nothing when no lock exists

## 6. Add unit tests for SignaleringMailHelper

- [ ] 6.1 Create `SignaleringMailHelperTest.kt` in `src/test/kotlin/nl/info/zac/signalering/`
- [ ] 6.2 Add test: `getTargetMail` GROUP with email → returns `SignaleringTarget.Mail`
- [ ] 6.3 Add test: `getTargetMail` GROUP without email → returns null
- [ ] 6.4 Add test: `getTargetMail` USER with email → returns `SignaleringTarget.Mail` with full name
- [ ] 6.5 Add test: `getTargetMail` USER without email → returns null
- [ ] 6.6 Add test: `getTargetMail` unknown target type → returns null
- [ ] 6.7 Add tests: `getMailTemplate` maps all five `SignaleringType.Type` values including both ZAAK_VERLOPEND detail variants

## 7. Add unit tests for AuditEnkelvoudigInformatieobjectConverter

- [ ] 7.1 Create `AuditEnkelvoudigInformatieobjectConverterTest.kt` in `src/test/kotlin/nl/info/zac/history/converter/documenten/`
- [ ] 7.2 Mock `ZtcClientService` using MockK
- [ ] 7.3 Add test: two identical objects → empty list
- [ ] 7.4 Add test: only `titel` changed → one `HistoryLine` with label `"titel"`
- [ ] 7.5 Add test: `informatieobjecttype` URI changed → `ZtcClientService` called for both URIs, one `HistoryLine` with label `"documentType"`
- [ ] 7.6 Add test: multiple fields changed → correct number of `HistoryLine` entries
- [ ] 7.7 Add test: `oud` is null → single `HistoryLine` with label `"informatieobject"`
- [ ] 7.8 Add test: `nieuw` is null → single `HistoryLine` with label `"informatieobject"`

## 8. Add unit tests for AuditWijzigingAttributeHelpers

- [ ] 8.1 Create `AuditWijzigingAttributeHelpersTest.kt` in `src/test/kotlin/nl/info/zac/history/converter/`
- [ ] 8.2 Add tests for String overload: values differ → line added; values equal → no change
- [ ] 8.3 Add tests for Boolean overload
- [ ] 8.4 Add tests for nullable LocalDate overload: different dates, one null, both null
- [ ] 8.5 Add test for ZonedDateTime overload
- [ ] 8.6 Add test for StatusEnum overload
- [ ] 8.7 Add test for VertrouwelijkheidaanduidingEnum overload

## 9. Add unit tests for AbstractVerblijfplaatsJsonbDeserializer and AbstractDatumJsonbDeserializer

- [ ] 9.1 Create `AbstractVerblijfplaatsJsonbDeserializerTest.kt` in `src/test/kotlin/nl/info/client/brp/util/`
- [ ] 9.2 Add tests for all four verblijfplaats type values and the unknown-type error case
- [ ] 9.3 Create `AbstractDatumJsonbDeserializerTest.kt` in `src/test/kotlin/nl/info/client/brp/util/`
- [ ] 9.4 Add tests for all four datum type values and the unknown-type error case

## 10. Add unit tests for GebruikersvoorkeurenRESTService

- [ ] 10.1 Create `GebruikersvoorkeurenRESTServiceTest.kt` in `src/test/kotlin/net/atos/zac/app/gebruikersvoorkeuren/`
- [ ] 10.2 Use reflection to inject all three mocks (`GebruikersvoorkeurenService`, `Instance<LoggedInUser>`, `PolicyService`) into the private fields
- [ ] 10.3 Add test: `listZoekopdrachten` delegates to service with correct parameters and returns converted list
- [ ] 10.4 Add test: `deleteZoekopdracht` delegates to service
- [ ] 10.5 Add test: `createOrUpdateZoekopdracht` converts, delegates, returns converted result
- [ ] 10.6 Add test: `readTabelGegevens` reads tabel instellingen and werklijst rechten
- [ ] 10.7 Add test: `updateAantalItemsPerPagina` with value in bounds → service called
- [ ] 10.8 Add test: `updateAantalItemsPerPagina` with value out of bounds → service NOT called
- [ ] 10.9 Add test: `listDashboardCards` delegates and returns converted list

## 11. Add unit tests for MailRestService

- [ ] 11.1 Create `MailRestServiceTest.kt` in `src/test/kotlin/net/atos/zac/app/mail/`
- [ ] 11.2 Add test: `sendMail` reads zaak, asserts policy, sends mail
- [ ] 11.3 Add test: `sendAcknowledgmentReceiptMail` sends mail and marks zaak when permitted and not yet sent
- [ ] 11.4 Add test: `sendAcknowledgmentReceiptMail` policy denied → mail not sent

## 12. Add unit tests for FlowableHelper

- [ ] 12.1 Create `FlowableHelperTest.kt` in `src/test/kotlin/net/atos/zac/flowable/`
- [ ] 12.2 Instantiate `FlowableHelper` with 16 mocked dependencies
- [ ] 12.3 Add test: verify all 16 properties hold the expected mock references

## 13. Add unit tests for RESTCaseDefinitionConverter

- [ ] 13.1 Create `RESTCaseDefinitionConverterTest.kt` in `src/test/kotlin/net/atos/zac/app/admin/converter/`
- [ ] 13.2 Add test: `convertToRESTCaseDefinition(CaseDefinition, false)` returns name and key, no relations
- [ ] 13.3 Add test: `convertToRESTCaseDefinition(CaseDefinition, true)` calls `CMMNService` and populates relations
- [ ] 13.4 Add test: `convertToRESTCaseDefinition(String, Boolean)` delegates to `CMMNService.readCaseDefinition`

## 14. Add unit tests for RestOpenbareRuimteConverter

- [ ] 14.1 Create `RestOpenbareRuimteConverterTest.kt` in `src/test/kotlin/net/atos/zac/app/bag/converter/`
- [ ] 14.2 Add test: `convertToREST(null, adres)` returns null
- [ ] 14.3 Add test: `convertToREST(openbareRuimteIO, adres)` uses adres woonplaatsNaam
- [ ] 14.4 Add test: `convertToREST(openbareRuimteIO, null)` falls back to ligtIn
- [ ] 14.5 Add test: `convertToREST(ZaakobjectOpenbareRuimte)` returns null when objectIdentificatie is null
- [ ] 14.6 Add test: `convertToREST(ZaakobjectOpenbareRuimte)` maps all fields correctly
- [ ] 14.7 Add test: `convertToZaakobject` builds correct `ZaakobjectOpenbareRuimte`

## 15. Add unit tests for OpenZaakReadinessHealthCheck

- [ ] 15.1 Create `OpenZaakReadinessHealthCheckTest.kt` in `src/test/kotlin/nl/info/zac/health/`
- [ ] 15.2 Add test: `call()` returns UP when `ZtcClientService.listCatalogus` succeeds
- [ ] 15.3 Add test: `call()` returns DOWN with error data when `ZtcClientService.listCatalogus` throws `RuntimeException`

## 16. Add unit tests for MailtemplateKoppelingRestService

- [ ] 16.1 Create `MailtemplateKoppelingRestServiceTest.kt` in `src/test/kotlin/net/atos/zac/app/admin/`
- [ ] 16.2 Add test: policy denied → service not called
- [ ] 16.3 Add test: `readMailtemplateKoppeling` delegates and returns converted result
- [ ] 16.4 Add test: `deleteMailtemplateKoppeling` delegates to service
- [ ] 16.5 Add test: `storeMailtemplateKoppeling` converts, delegates, returns result

## 17. Add unit tests for UserTaskCompletionListener

- [ ] 17.1 Create `UserTaskCompletionListenerTest.kt` in `src/test/kotlin/net/atos/zac/flowable/bpmn/`
- [ ] 17.2 Mock `FlowableHelper.getInstance()` via `mockkStatic(FlowableHelper.FlowableHelperProvider::class)`
- [ ] 17.3 Add test: `onEvent` with `TASK_COMPLETED` → `IndexingService.removeTaak` called with task ID
- [ ] 17.4 Add test: `onEvent` with non-matching event type → `IndexingService.removeTaak` not called
- [ ] 17.5 Add test: `isFailOnException` returns true
- [ ] 17.6 Add test: `isFireOnTransactionLifecycleEvent` returns true
- [ ] 17.7 Add test: `getOnTransaction` returns `ON_TRANSACTION_COMMITTED`

## 18. Verify coverage improvement

- [ ] 18.1 Run `./gradlew test jacocoTestReport` and confirm overall instruction coverage exceeds 69.55%
