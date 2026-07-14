## Context

`net.atos.client.zgw.shared.util` holds five Java classes: `DateTimeUtil` (date conversion constants/helper), `HistorieUtil` (formats values for ZGW audit-trail "waarde" strings), `JsonbConfiguration` (a `ContextResolver<Jsonb>` wiring three custom (de)serializers), `JsonbUtil` (a shared `Jsonb` instance with a field-only visibility strategy), and `URIJsonbDeserializer` (lenient `URI` deserialization that swallows a known JSON-B parser quirk).

The sibling Kotlin package `nl.info.client.zgw.util` already exists and holds `AuditWijzigingJsonbDeserializer`, which itself imports `net.atos.client.zgw.shared.util.JsonbUtil.JSONB` — so this migration also removes a Kotlin-to-Java dependency in the same package.

Only `JsonbConfigurationTest` (currently misplaced at `net.atos.zac.util`) has coverage today. `DateTimeUtil`, `HistorieUtil`, `JsonbUtil`, and `URIJsonbDeserializer` have none.

## Goals / Non-Goals

**Goals:**
- Establish unit test coverage for `DateTimeUtil`, `HistorieUtil`, `JsonbUtil`, and `URIJsonbDeserializer` before migrating them
- Migrate all five classes to idiomatic Kotlin at `nl.info.client.zgw.util`
- Move `JsonbConfigurationTest` into the same package as the class it tests
- Update every import site so no file in `src/` references `net.atos.client.zgw.shared.util` after this change

**Non-Goals:**
- Changing behaviour of date/time formatting, historie "waarde" strings, JSON-B visibility rules, or URI parsing leniency — pure mechanical translation plus Kotlin idioms
- Migrating callers (`Rol.java`, `ZaakInformatieobject.java`, `RESTBAGObjectJsonbDeserializer.java`, etc.) beyond updating their import statements
- Adding integration test coverage

## Decisions

### Write tests first, then migrate

`DateTimeUtil`, `HistorieUtil`, `JsonbUtil`, and `URIJsonbDeserializer` have zero unit tests. Writing them in Kotlin against the current Java source first confirms behaviour and provides a regression net, matching the approach used in the prior `net.atos.zac.admin` migration.

### `DateTimeUtil` and `HistorieUtil` as Kotlin objects with top-level functions

Both classes are stateless holders of `private static final` constants and static methods with a private constructor to prevent instantiation. In Kotlin this becomes an `object` (or top-level `const val`s + functions) — no constructor-hiding boilerplate needed. `HistorieUtil.toWaarde` is overloaded per parameter type in Java; Kotlin keeps the same overloads (`fun toWaarde(date: LocalDate?): String?`, etc.) since call sites dispatch on static type, not a sealed hierarchy.

### `JsonbUtil` as a Kotlin object

`JsonbUtil.JSONB` and `PROPERTY_VISIBILITY_STRATEGY` become `object JsonbUtil` members. The anonymous `PropertyVisibilityStrategy` becomes a Kotlin `object : PropertyVisibilityStrategy { ... }` expression assigned to the property.

### `URIJsonbDeserializer` — keep the same `IllegalStateException` swallow

The existing `catch (IllegalStateException)` is a documented workaround for a JSON-B parser quirk (`getString()` on `VALUE_NULL` state), not a design flaw to fix here. Preserve the same catch block and comment in Kotlin, translating `StringUtils.isNotEmpty` to `?.takeIf { it.isNotEmpty() }` or keep `org.apache.commons.lang3.StringUtils` for parity — same behaviour either way.

### `JsonbConfiguration` as a Kotlin class implementing `ContextResolver<Jsonb>`

Direct translation: constructor builds the `Jsonb` instance via `JsonbConfig().withDeserializers(...).withSerializers(...)`, stored in a property, returned from `getContext`.

### Move `JsonbConfigurationTest`

The existing test at `src/test/kotlin/net/atos/zac/util/JsonbConfigurationTest.kt` tests `net.atos.client.zgw.shared.util.JsonbConfiguration` (imported implicitly via same-package resolution — actually it's in a different package, so it must have an explicit import). Move it to `src/test/kotlin/nl/info/client/zgw/util/JsonbConfigurationTest.kt` and update its package declaration and import to match the migrated class.

## Risks / Trade-offs

- **Caller update breadth** — `HistorieUtil` and `JsonbUtil` are used across several Kotlin client files (`DrcClient.kt`, `ZtcClient.kt`, `BrcClient.kt`, `ZrcClient.kt`, `ZgwApiService.kt`, `RolJsonbDeserializer.kt`, `ZaakObjectJsonbDeserializer.kt`) and remaining Java model classes (`Rol.java`, `ZaakInformatieobject.java`). A missed import fails at compile time, making it safe to detect.
- **`AuditWijzigingJsonbDeserializer.kt` self-reference** — it imports `net.atos.client.zgw.shared.util.JsonbUtil.JSONB`; this import must be updated to `nl.info.client.zgw.util.JsonbUtil.JSONB` as part of this migration, not left for a follow-up.
- **Historie "waarde" string exactness** — `HistorieUtil` formats are user-facing (Dutch date formats, "Ja"/"Nee"). Unit tests must pin exact expected strings to catch any accidental format drift during translation.
