## 1. Add unit tests for RESTHumanTaskParametersConverter

- [x] 1.1 Create `RESTHumanTaskParametersConverterTest.kt` in `src/test/kotlin/net/atos/zac/app/admin/converter/`
- [x] 1.2 Add test: `convertHumanTaskParametersCollection` with matching `ZaaktypeCmmnHumantaskParameters` returns fully populated `RESTHumanTaskParameters`
- [x] 1.3 Add test: `convertHumanTaskParametersCollection` with no match returns default `RESTHumanTaskParameters` (actief=false)
- [x] 1.4 Add test: `convertRESTHumanTaskParameters` converts single entry with all fields mapped correctly
- [x] 1.5 Add test: `convertRESTHumanTaskParameters` converts multiple entries to same-size list

## 2. Add unit tests for RestHumanTaskReferenceTableConverter

- [x] 2.1 Create `RestHumanTaskReferenceTableConverterTest.kt` in `src/test/kotlin/net/atos/zac/app/admin/converter/`
- [x] 2.2 Use reflection to inject `ReferenceTableService` mock into private field
- [x] 2.3 Add test: `convertDefault` calls `ReferenceTableService.readReferenceTable` and returns correct `RestHumanTaskReferenceTable`
- [x] 2.4 Add test: `convert(Collection)` converts multiple `HumanTaskReferentieTabel` entries to REST
- [x] 2.5 Add test: `convert(List<RestHumanTaskReferenceTable>)` converts back to domain objects, fetching tabel from service

## 3. Add unit tests for BetrokkeneIdentificatieValidator

- [x] 3.1 Create `BetrokkeneIdentificatieValidatorTest.kt` in `src/test/kotlin/nl/info/zac/app/zaak/model/`
- [x] 3.2 Add tests for BSN: valid (temporaryPersonId set, kvkNummer/vestigingsnummer blank), invalid (temporaryPersonId null), invalid (kvkNummer present)
- [x] 3.3 Add tests for VN: valid, invalid (kvkNummer blank), invalid (temporaryPersonId set)
- [x] 3.4 Add tests for RSIN: valid, invalid (vestigingsnummer present)
- [x] 3.5 Add test: null input returns false

## 4. Add unit tests for PersonenQueryResponseJsonbDeserializer

- [x] 4.1 Create `PersonenQueryResponseJsonbDeserializerTest.kt` in `src/test/kotlin/nl/info/client/brp/util/`
- [x] 4.2 Add tests for all six supported `type` values using real `JsonParser` from JSON strings
- [x] 4.3 Add test: unknown `type` throws `InputValidationFailedException`

## 5. Add unit tests for EnkelvoudigInformatieObjectLockService

- [x] 5.1 Create `EnkelvoudigInformatieObjectLockServiceTest.kt` in `src/test/kotlin/nl/info/zac/enkelvoudiginformatieobject/`
- [x] 5.2 Mock `EntityManager` (criteria builder chain) and `DrcClientService` using MockK
- [x] 5.3 Add test: `createLock` calls DRC client, persists entity, flushes, returns lock
- [x] 5.4 Add test: `findLock` returns lock when one exists
- [x] 5.5 Add test: `findLock` returns null when none exists
- [x] 5.6 Add test: `readLock` returns lock when it exists
- [x] 5.7 Add test: `readLock` throws `EnkelvoudigInformatieObjectLockNotFoundException` when none exists
- [x] 5.8 Add test: `deleteLock` unlocks via DRC client and removes entity when lock exists
- [x] 5.9 Add test: `deleteLock` does nothing when no lock exists

## 6. Add unit tests for SignaleringMailHelper

- [x] 6.1 Create `SignaleringMailHelperTest.kt` in `src/test/kotlin/nl/info/zac/signalering/`
- [x] 6.2 Add test: `getTargetMail` GROUP with email → returns `SignaleringTarget.Mail`
- [x] 6.3 Add test: `getTargetMail` GROUP without email → returns null
- [x] 6.4 Add test: `getTargetMail` USER with email → returns `SignaleringTarget.Mail` with full name
- [x] 6.5 Add test: `getTargetMail` USER without email → returns null
- [x] 6.6 Add test: `getTargetMail` unknown target type → returns null
- [x] 6.7 Add tests: `getMailTemplate` maps all five `SignaleringType.Type` values including both ZAAK_VERLOPEND detail variants

## 7. Add unit tests for AuditEnkelvoudigInformatieobjectConverter

- [x] 7.1 Create `AuditEnkelvoudigInformatieobjectConverterTest.kt` in `src/test/kotlin/nl/info/zac/history/converter/documenten/`
- [x] 7.2 Mock `ZtcClientService` using MockK
- [x] 7.3 Add test: two identical objects → empty list
- [x] 7.4 Add test: only `titel` changed → one `HistoryLine` with label `"titel"`
- [x] 7.5 Add test: `informatieobjecttype` URI changed → `ZtcClientService` called for both URIs, one `HistoryLine` with label `"documentType"`
- [x] 7.6 Add test: multiple fields changed → correct number of `HistoryLine` entries
- [x] 7.7 Add test: `oud` is null → single `HistoryLine` with label `"informatieobject"`
- [x] 7.8 Add test: `nieuw` is null → single `HistoryLine` with label `"informatieobject"`

## 8. Add unit tests for AuditWijzigingAttributeHelpers

- [x] 8.1 Create `AuditWijzigingAttributeHelpersTest.kt` in `src/test/kotlin/nl/info/zac/history/converter/`
- [x] 8.2 Add tests for String overload: values differ → line added; values equal → no change
- [x] 8.3 Add tests for Boolean overload
- [x] 8.4 Add tests for nullable LocalDate overload: different dates, one null, both null
- [x] 8.5 Add test for ZonedDateTime overload
- [x] 8.6 Add test for StatusEnum overload
- [x] 8.7 Add test for VertrouwelijkheidaanduidingEnum overload

## 9. Add unit tests for AbstractVerblijfplaatsJsonbDeserializer and AbstractDatumJsonbDeserializer

- [x] 9.1 Create `AbstractVerblijfplaatsJsonbDeserializerTest.kt` in `src/test/kotlin/nl/info/client/brp/util/`
- [x] 9.2 Add tests for all four verblijfplaats type values and the unknown-type error case
- [x] 9.3 Create `AbstractDatumJsonbDeserializerTest.kt` in `src/test/kotlin/nl/info/client/brp/util/`
- [x] 9.4 Add tests for all four datum type values and the unknown-type error case

## 10. Add unit tests for GebruikersvoorkeurenRESTService

- [x] 10.1 Create `GebruikersvoorkeurenRESTServiceTest.kt` in `src/test/kotlin/net/atos/zac/app/gebruikersvoorkeuren/`
- [x] 10.2 Use reflection to inject all three mocks (`GebruikersvoorkeurenService`, `Instance<LoggedInUser>`, `PolicyService`) into the private fields
- [x] 10.3 Add test: `listZoekopdrachten` delegates to service with correct parameters and returns converted list
- [x] 10.4 Add test: `deleteZoekopdracht` delegates to service
- [x] 10.5 Add test: `createOrUpdateZoekopdracht` converts, delegates, returns converted result
- [x] 10.6 Add test: `readTabelGegevens` reads tabel instellingen and werklijst rechten
- [x] 10.7 Add test: `updateAantalItemsPerPagina` with value in bounds → service called
- [x] 10.8 Add test: `updateAantalItemsPerPagina` with value out of bounds → service NOT called
- [x] 10.9 Add test: `listDashboardCards` delegates and returns converted list

## 11. Add unit tests for MailRestService

- [x] 11.1 Create `MailRestServiceTest.kt` in `src/test/kotlin/net/atos/zac/app/mail/`
- [x] 11.2 Add test: `sendMail` reads zaak, asserts policy, sends mail
- [x] 11.3 Add test: `sendAcknowledgmentReceiptMail` sends mail and marks zaak when permitted and not yet sent
- [x] 11.4 Add test: `sendAcknowledgmentReceiptMail` policy denied → mail not sent

## 12. Add unit tests for FlowableHelper

- [x] 12.1 Create `FlowableHelperTest.kt` in `src/test/kotlin/net/atos/zac/flowable/`
- [x] 12.2 Instantiate `FlowableHelper` with 16 mocked dependencies
- [x] 12.3 Add test: verify all 16 properties hold the expected mock references

## 13. Add unit tests for RESTCaseDefinitionConverter

- [x] 13.1 Create `RESTCaseDefinitionConverterTest.kt` in `src/test/kotlin/net/atos/zac/app/admin/converter/`
- [x] 13.2 Add test: `convertToRESTCaseDefinition(CaseDefinition, false)` returns name and key, no relations
- [x] 13.3 Add test: `convertToRESTCaseDefinition(CaseDefinition, true)` calls `CMMNService` and populates relations
- [x] 13.4 Add test: `convertToRESTCaseDefinition(String, Boolean)` delegates to `CMMNService.readCaseDefinition`

## 14. Add unit tests for RestOpenbareRuimteConverter

- [x] 14.1 Create `RestOpenbareRuimteConverterTest.kt` in `src/test/kotlin/net/atos/zac/app/bag/converter/`
- [x] 14.2 Add test: `convertToREST(null, adres)` returns null
- [x] 14.3 Add test: `convertToREST(openbareRuimteIO, adres)` uses adres woonplaatsNaam
- [x] 14.4 Add test: `convertToREST(openbareRuimteIO, null)` falls back to ligtIn
- [x] 14.5 Add test: `convertToREST(ZaakobjectOpenbareRuimte)` returns null when objectIdentificatie is null
- [x] 14.6 Add test: `convertToREST(ZaakobjectOpenbareRuimte)` maps all fields correctly
- [x] 14.7 Add test: `convertToZaakobject` builds correct `ZaakobjectOpenbareRuimte`

## 15. Add unit tests for OpenZaakReadinessHealthCheck

- [x] 15.1 Create `OpenZaakReadinessHealthCheckTest.kt` in `src/test/kotlin/nl/info/zac/health/`
- [x] 15.2 Add test: `call()` returns UP when `ZtcClientService.listCatalogus` succeeds
- [x] 15.3 Add test: `call()` returns DOWN with error data when `ZtcClientService.listCatalogus` throws `RuntimeException`

## 16. Add unit tests for MailtemplateKoppelingRestService

- [x] 16.1 Create `MailtemplateKoppelingRestServiceTest.kt` in `src/test/kotlin/net/atos/zac/app/admin/`
- [x] 16.2 Add test: policy denied → service not called
- [x] 16.3 Add test: `readMailtemplateKoppeling` delegates and returns converted result
- [x] 16.4 Add test: `deleteMailtemplateKoppeling` delegates to service
- [x] 16.5 Add test: `storeMailtemplateKoppeling` converts, delegates, returns result

## 17. Add unit tests for UserTaskCompletionListener

- [x] 17.1 Create `UserTaskCompletionListenerTest.kt` in `src/test/kotlin/net/atos/zac/flowable/bpmn/`
- [x] 17.2 Mock `FlowableHelper.getInstance()` via `mockkStatic(FlowableHelper.FlowableHelperProvider::class)`
- [x] 17.3 Add test: `onEvent` with `TASK_COMPLETED` → `IndexingService.removeTaak` called with task ID
- [x] 17.4 Add test: `onEvent` with non-matching event type → `IndexingService.removeTaak` not called
- [x] 17.5 Add test: `isFailOnException` returns true
- [x] 17.6 Add test: `isFireOnTransactionLifecycleEvent` returns true
- [x] 17.7 Add test: `getOnTransaction` returns `ON_TRANSACTION_COMMITTED`

## 18. Verify coverage improvement

- [ ] 18.1 Run `./gradlew test jacocoTestReport` and confirm overall instruction coverage exceeds 69.55%
