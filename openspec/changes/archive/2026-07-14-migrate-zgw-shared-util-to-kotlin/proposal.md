## Why

`net.atos.client.zgw.shared.util` is the last Java package in the ZGW shared client layer. Project convention requires all Java code to be converted to Kotlin when touched. Three of its five classes (`DateTimeUtil`, `HistorieUtil`, `JsonbUtil`, `URIJsonbDeserializer`) have no unit tests, so migrating them without first establishing coverage risks silently changing behaviour (date/time formatting, "Ja"/"Nee" historie values, JSON-B visibility rules, and lenient URI parsing are all used across the ZGW client and history-line rendering).

## What Changes

- Add unit tests for `DateTimeUtil`, `HistorieUtil`, `JsonbUtil`, and `URIJsonbDeserializer` (no existing coverage)
- Migrate `DateTimeUtil`, `HistorieUtil`, `JsonbConfiguration`, `JsonbUtil`, and `URIJsonbDeserializer` to Kotlin at `nl.info.client.zgw.util`, using Kotlin idioms (top-level `const val`/object members instead of `private` constructors, `?.let`/`?:` instead of null checks)
- Move the existing `JsonbConfigurationTest` from `net.atos.zac.util` to `nl.info.client.zgw.util` alongside the migrated class
- Update all import sites across `src/main/java` and `src/main/kotlin` to point to the new `nl.info.client.zgw.util` package

## Capabilities

### New Capabilities

- `zgw-shared-utilities`: Documents the testable behaviour contracts of the shared ZGW client utilities (date-time conversion, historie "waarde" formatting, JSON-B configuration/visibility, lenient URI deserialization) that previously had little to no spec or test coverage. No behaviour changes — this formalizes existing behaviour as part of the Kotlin migration.

### Modified Capabilities

_None — this is a mechanical Kotlin migration of internal utility classes with no change to externally observable behaviour._

## Impact

- `DateTimeUtil` — delete Java, add Kotlin equivalent; update 1 caller (`RestTaskHistoryLine.kt`/`HistoryLine.kt` area) — verify exact call sites during implementation
- `HistorieUtil` — delete Java, add Kotlin equivalent; used by `Rol.java`/`ZaakInformatieobject.java` and other history-line rendering code
- `JsonbConfiguration` — delete Java, add Kotlin equivalent; used by `RESTBAGObjectJsonbDeserializer.java` and REST config wiring; move its test
- `JsonbUtil` — delete Java, add Kotlin equivalent; used by `DrcClient.kt`, `ZtcClient.kt`, `BrcClient.kt`, `ZrcClient.kt`, `ZgwApiService.kt`, and jsonb (de)serializers
- `URIJsonbDeserializer` — delete Java, add Kotlin equivalent; referenced by `JsonbConfiguration`
- No API contract change, no DB schema change, no new dependencies
