## Why

Current unit test instruction coverage is 67.55% (71,069 of 105,210 instructions). Eighteen backend classes have 0% or near-0% coverage despite containing real business logic. Adding comprehensive tests for these classes will push coverage above 69.55% (+2%), improving confidence in future refactoring and reducing regression risk.

## What Changes

**New test classes** (13 additional, on top of the original 5):

| Class | Instructions | Language |
|---|---|---|
| `RESTHumanTaskParametersConverter` | 189 (0%) | Java |
| `EnkelvoudigInformatieObjectLockService` | 156 (0%) | Kotlin |
| `PersonenQueryResponseJsonbDeserializer` | 132 (0%) | Kotlin |
| `BetrokkeneIdentificatieValidator` | 114 (0%) | Kotlin |
| `SignaleringMailHelper` | 103 (0%) | Kotlin |
| `AuditEnkelvoudigInformatieobjectConverter` | 282 missed (2.1%) | Kotlin |
| `GebruikersvoorkeurenRESTService` | 168 (0%) | Java |
| `FlowableHelper` | 108 (0%) | Kotlin |
| `AbstractVerblijfplaatsJsonbDeserializer` | 96 (0%) | Kotlin |
| `AbstractDatumJsonbDeserializer` | 94 (0%) | Kotlin |
| `AuditWijzigingAttributeHelpers` | 86 (0%) | Kotlin |
| `RestHumanTaskReferenceTableConverter` | 80 (0%) | Java |
| `MailRestService` | 101 (0%) | Java |
| `RESTCaseDefinitionConverter` | 71 (0%) | Java |
| `RestOpenbareRuimteConverter` | 138 missed (12.1%) | Java |
| `OpenZaakReadinessHealthCheck` | 65 (0%) | Kotlin |
| `MailtemplateKoppelingRestService` | 81 (0%) | Java |
| `UserTaskCompletionListener` | 57 (0%) | Kotlin |

Combined these 18 classes account for 2,121 instructions — above the 2,105 needed for +2% coverage.

No production code changes. All new files are Kotlin test classes (`XxxTest.kt`) added alongside their existing Java or Kotlin source files.

## Capabilities

### New Capabilities

- `enkelvoudig-informatieobject-lock-service-tests`: Unit tests for `EnkelvoudigInformatieObjectLockService` covering lock, unlock, and forced-unlock scenarios
- `betrokkene-identificatie-validator-tests`: Unit tests for `BetrokkeneIdentificatieValidator` covering all validation branches for BSN, VN, RSIN, and null
- `personen-query-response-deserializer-tests`: Unit tests for `PersonenQueryResponseJsonbDeserializer` covering all 6 supported response types and the unknown-type error
- `signalering-mail-helper-tests`: Unit tests for `SignaleringMailHelper` covering mail-address resolution and mail-template mapping
- `rest-human-task-parameters-converter-tests`: Unit tests for `RESTHumanTaskParametersConverter` covering both conversion directions
- `audit-enkelvoudig-informatieobject-converter-tests`: Unit tests for `AuditEnkelvoudigInformatieobjectConverter` covering field-change detection and URI lookup
- `gebruikersvoorkeuren-rest-service-tests`: Unit tests for `GebruikersvoorkeurenRESTService` covering all REST endpoints
- `mail-rest-service-tests`: Unit tests for `MailRestService` covering send-mail and acknowledgement-mail endpoints
- `flowable-helper-tests`: Unit tests for `FlowableHelper` verifying constructor injection and property accessibility
- `brp-jsonb-deserializer-tests`: Unit tests for `AbstractVerblijfplaatsJsonbDeserializer` and `AbstractDatumJsonbDeserializer` covering all supported types and the unknown-type error
- `audit-wijziging-attribute-helpers-tests`: Unit tests for the `AuditWijzigingAttributeHelpers` extension functions covering change-detection across all overloaded types
- `rest-human-task-reference-table-converter-tests`: Unit tests for `RestHumanTaskReferenceTableConverter` covering domain→REST and REST→domain conversion
- `rest-case-definition-converter-tests`: Unit tests for `RESTCaseDefinitionConverter` covering conversion with and without relations
- `rest-openbare-ruimte-converter-tests`: Unit tests for `RestOpenbareRuimteConverter` covering all static conversion methods and null-input guards
- `openZaak-readiness-health-check-tests`: Unit tests for `OpenZaakReadinessHealthCheck` covering the UP and DOWN health-check scenarios
- `mailtemplateKoppeling-rest-service-tests`: Unit tests for `MailtemplateKoppelingRestService` covering CRUD endpoints with policy enforcement
- `user-task-completion-listener-tests`: Unit tests for `UserTaskCompletionListener` covering event handling and lifecycle method return values

### Modified Capabilities

## Impact

- Test files only — no production code changes
- Java source files remain as Java; no Kotlin migrations in this change
- Affected packages (test side only): `nl/info/zac/enkelvoudiginformatieobject`, `nl/info/zac/app/zaak/model`, `nl/info/client/brp/util`, `nl/info/zac/signalering`, `net/atos/zac/app/admin/converter`, `nl/info/zac/history/converter`, `net/atos/zac/app/gebruikersvoorkeuren`, `net/atos/zac/app/mail`, `net/atos/zac/flowable`, `nl/info/zac/health`, `net/atos/zac/flowable/bpmn`, `net/atos/zac/app/bag/converter`, `net/atos/zac/app/admin`
