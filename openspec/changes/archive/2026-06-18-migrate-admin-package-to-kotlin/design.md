## Context

`net.atos.zac.admin` holds the last Java service classes in the admin domain: `MailTemplateKoppelingenService` (JPA EntityManager, `@ApplicationScoped @Transactional`) and `ZaaktypeCmmnConfigurationService` (`@ApplicationScoped`, implements `Caching`, wraps two Caffeine caches over `ZaaktypeCmmnConfigurationBeheerService`). Three model classes also remain in Java: `FormulierDefinitie` (enum with `Set<FormulierVeldDefinitie>` field), `FormulierVeldDefinitie` (enum with `ReferenceTable.SystemReferenceTable` field), and `HumanTaskReferentieTabel` (JPA `@Entity`, `@ManyToOne` to `ReferenceTable` and `ZaaktypeCmmnHumantaskParameters`).

The `nl.info.zac.admin` Kotlin package already exists and holds the beheer-layer services and all migrated model types. Tests follow Kotest BehaviorSpec with MockK, constructor-injecting mocks directly (no CDI container).

`ResulttaattypeNotFoundException.java` in the `exception/` subpackage is already deleted in the working tree (unstaged `D` in `git status`) but not yet committed.

## Goals / Non-Goals

**Goals:**
- Establish unit test coverage for both services before touching their implementation
- Migrate all five Java files to idiomatic Kotlin at `nl.info.zac.admin` / `nl.info.zac.admin.model`
- Update every import site so no file in `src/` references `net.atos.zac.admin` after this change
- Commit the already-deleted exception file

**Non-Goals:**
- Changing service behaviour — pure mechanical translation plus Kotlin idioms
- Migrating `MailtemplateKoppelingRESTService.java` or `RestHumanTaskReferenceTableConverter.java` — they are callers in a different package and not in scope
- Adding integration test coverage

## Decisions

### Write tests first, then migrate

The two services have zero unit tests. Writing them in Kotlin against the Java source first confirms behaviour and provides a regression net. Then migrate the source and verify tests still pass.

Alternative considered: migrate first, then add tests. Rejected because any bug introduced during migration would be invisible until integration testing.

### Use nullable types instead of `Optional`

`MailTemplateKoppelingenService.find` returns `Optional<ZaaktypeCmmnMailtemplateParameters>`. In Kotlin, replace with `ZaaktypeCmmnMailtemplateParameters?`. All call sites already null-check or call `ifPresent`; migrate those to `?.let { }` or `?: return`.

### Keep Caffeine cache as-is in ZaaktypeCmmnConfigurationService

The `static final Map<String, Cache<?, ?>> CACHES` in Java becomes a `companion object` in Kotlin. Behaviour and cache names stay identical because `cacheStatistics()` and `estimatedCacheSizes()` already read from that map.

### HumanTaskReferentieTabel as a Kotlin data-like class with JPA annotations

`HumanTaskReferentieTabel` uses custom `equals`/`hashCode` based on `tabel` and `veld`. In Kotlin, implement as a regular class (not `data class`) because JPA entities should not use `data class` (copies break identity equality required by Hibernate). Keep explicit `equals`/`hashCode` based on the same fields.

### FormulierDefinitie and FormulierVeldDefinitie as Kotlin enums

Both are simple Java enums with constructor parameters. Translate directly to Kotlin `enum class`. `getVeldDefinities()` → `val veldDefinities: Set<FormulierVeldDefinitie>` (property), `getDefaultTabel()` → `val defaultTabel: ReferenceTable.SystemReferenceTable`.

## Risks / Trade-offs

- **Caller update breadth** — ~8 Kotlin files and 2 Java files import from `net.atos.zac.admin`. All need import updates; a missed import will fail at compile time, making it safe to detect.
- **Caffeine static map in companion object** — JVM statics from `companion object` behave identically to Java `static final`; no runtime difference.
- **JPA entity Kotlin pitfalls** — Kotlin `val` fields on JPA entities require a no-arg constructor; add `@JvmField` where needed or use `open var` per project conventions for JPA entities. Check existing Kotlin JPA entities (e.g. `ReferenceTable.kt`) for the pattern used.
