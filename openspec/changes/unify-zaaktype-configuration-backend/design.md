## Context

See proposal.md for the motivation. The RFC attached to PZ-12637 (`rfc-unify-bpmn-cmmn-zaaktype-configuration.md`)
is the source design. This document records the decisions for its backend chunks A1–A3 and B1–B4, and the
places where it deviates from the RFC after reading the code at `9e5c88f570`.

Current state:

- **Entities.** `ZaaktypeConfiguration` is an abstract JPA entity with `JOINED` inheritance. Its discriminator
  `configuration_type` is a nullable PostgreSQL ENUM. `ZaaktypeCmmnConfiguration` adds five columns and five
  child associations. `ZaaktypeBpmnConfiguration` adds only `bpmn_process_definition_key`.
- **Schema.** `zaaktype_bpmn_configuration` has no primary key, foreign key, or index; V84 dropped them.
  Neither subclass table has a foreign key to the base.
- **Services.** Six services. Each engine has its own:
  - find semantics:
    - CMMN by UUID has no ordering.
    - BPMN by UUID runs in `REQUIRES_NEW` and orders by creatiedatum.
    - CMMN "active" uses a max-creatiedatum subquery.
    - BPMN "active" uses `LIMIT 1`.
  - null contracts: `fetch` returns an empty new instance, `read` is nullable.
  - store path: CMMN validates, BPMN converts a stale id into an insert.
  - productaanvraagtype check. There are three checks with three exclusion keys.
- **Shared code.** `ZaaktypeHelperService` is the only code that both engines share. It copies the shared
  fields into a target entity and remaps resultaattypen by omschrijving.
- **Callers.** About 30 call sites in `src/main` are typed to `ZaaktypeCmmnConfiguration`. Five callers branch
  on the discriminator. Five more decide the engine by which table has a row:
  - `RestZaaktypeConverter` and `HealthCheckService` give BPMN precedence.
  - `ProductaanvraagService` gives CMMN precedence.
- **REST contract.** It must not change; see the `zaaktype-configuration` spec.

## Goals / Non-Goals

**Goals:**

- One read service and one beheer service for every zaaktype configuration, whatever engine it is bound to.
  Both REST resources and all callers use them.
- No code outside the process binding adapters and the CMMN extension names an engine.
- Every chunk is a pull request that compiles, passes `./gradlew build` and `./gradlew itest`, and can be
  merged on its own, in stack order.
- Each chunk reduces the number of files in `src/main` that name `ZaaktypeCmmnConfiguration` or
  `ZaaktypeBpmnConfiguration`. The count is 19 today (RFC section 11) and must reach zero after A3.

**Non-Goals:**

- Any change to the REST resources, their payloads, or the OpenAPI spec (PZ-12754).
- A shared task form configuration (RFC section 7).
- Using the zaaktype `identificatie` in place of the omschrijving as zaaktype identity (RFC section 10).
- Dropping the resultaattype UUID columns. This is the contract step of B2 in a later release.
- Admin input for the moved settings on a BPMN zaaktype. The BPMN payload does not carry them, so a beheerder
  sets them after PZ-12754. Until then a BPMN configuration gets them only from a predecessor version or from
  data.

## Decisions

### D1. Stacked pull requests, in RFC order

Each chunk is a branch based on the branch of the previous chunk:

| # | Branch | Based on | Flyway |
|---|---|---|---|
| A1 | `feature/PZ-12669-unify-zaaktype-configuration-backend` | `main` | V100 |
| A2-java | `feature/PZ-12669-kotlin-migration-mailtemplate-koppeling` | A1 | — |
| A2 | `feature/PZ-12669-a2-zaak-settings-to-base` | A2-java | V101 |
| A3 | `feature/PZ-12669-a3-split-configuration` | A2 | V102 |
| B1 | `feature/PZ-12669-b1-process-binding` | A3 | — |
| B2 | `feature/PZ-12669-b2-resultaattype-omschrijving` | B1 | V103 |
| B3 | `feature/PZ-12669-b3-configuration-versioning` | B2 | — |
| B4 | `feature/PZ-12669-b4-confirmation-email-fallback` | B3 | — |

A1 carries the openspec change directory. When a chunk has to change Java code, the conversion of that code to
Kotlin is a PR of its own, stacked directly below the chunk (A2-java below A2). That PR changes no behaviour, so
a reviewer can read it as a pure conversion. Every PR title follows Conventional Commits, for example
`refactor(admin): ...`, and every PR body ends with `Solves PZ-12669`. When a lower PR merges, the next PR
is rebased onto `main` and retargeted. The flyway versions are fixed per chunk, so a rebase never renumbers a
migration. If another PR takes V100–V103 on `main` first, the stack renumbers its migrations once, from A1 up.

Alternative: one large PR. Rejected, because the story asks for stacked PRs, and A3 alone touches about 30
call sites.

### D2. A1: repair the schema, but do not add the two unique constraints of the RFC

V100 does the following:

- adds a primary key on `zaaktype_bpmn_configuration(id)`
- adds foreign keys from `zaaktype_cmmn_configuration(id)` and `zaaktype_bpmn_configuration(id)` to
  `zaaktype_configuration(id)` with `ON DELETE CASCADE`
- sets `configuration_type` NOT NULL
- adds UNIQUE constraints on the foreign key column of the three one-to-one children: betrokkene, BRP, and
  CMMN email

Before it adds a constraint, V100 first sets a null `configuration_type` from the subclass table that holds
the id. It then moves every row that would still violate a constraint into a quarantine table (D2a).
Those rows are:

- subclass rows without a base row, with their CMMN child rows
- base rows without a `configuration_type` whose id is in neither subclass table, or in both, with their child
  rows. The child rows go first, because ON DELETE CASCADE would otherwise remove them without a copy.
- duplicate ids in `zaaktype_bpmn_configuration`; the physically last row stays
- duplicate one-to-one children; the row with the highest id stays

Hibernate cannot load any of these rows today:
- An EAGER `@OneToOne` with two rows throws.
- A subclass row without a base row is never joined.
- A base row without a discriminator matches no subclass.

Moving them out changes nothing that ZAC can do with the data. A base row whose subclass row is missing does
not block any new constraint, so it stays.

### D2a. Quarantine in place of failing or deleting

The migrations never delete a row that blocks a new constraint, and they never fail on one. V100 creates:

```sql
CREATE TABLE ${schema}.zaaktype_configuration_migration_quarantine (
    id             BIGSERIAL PRIMARY KEY,
    migration      VARCHAR NOT NULL,      -- 'V100', ...
    source_table   VARCHAR NOT NULL,
    reason         VARCHAR NOT NULL,      -- 'orphaned subclass row', 'duplicate one-to-one child', ...
    row_data       JSONB   NOT NULL,      -- to_jsonb(<source row>)
    quarantined_at TIMESTAMPTZ NOT NULL DEFAULT now()
);
```

For each rule, the migration runs `INSERT INTO ... SELECT to_jsonb(t) ...` and then `DELETE` in the same
transaction. It reports the count per rule with `RAISE WARNING` inside a `DO` block, and Flyway writes that
to the ZAC startup log. `row_data` holds the complete row, so ops can restore it with
`jsonb_populate_record`.

The choice follows from how `FlywayIntegrator` runs:

- **Failing is rejected.** A failed migration aborts ZAC startup, and the new pod never becomes ready. The
  upgrade on that environment then blocks until someone fixes the data by hand. The migration also cannot be
  corrected afterwards, because it has already succeeded elsewhere and Flyway would report a checksum mismatch.
- **Deleting is rejected.** It loses data without a trace on environments that nobody can inspect beforehand.

A later migration uses the same table, with its own `migration` value, for any row that blocks one of its
constraints. V101 has no such row: it only re-points foreign keys from the CMMN table to the base table, and
since V100 every CMMN id is a base id. Its migration test asserts that nothing is quarantined.

ZAC never reads the quarantine table. A follow-up ticket covers checking it on every environment and
dropping it by hand (Migration Plan). Every migration that writes to the table creates it first with
`CREATE TABLE IF NOT EXISTS`, so a manual drop between releases never breaks a later migration. Each
migration defines its quarantine helper as a `pg_temp` function, so the helper never outlives the migration.

Deviations from RFC section 3.5:

- **No UNIQUE on `productaanvraagtype`.** Versions of one zaaktype share the omschrijving and legitimately share
  the productaanvraagtype. A plain unique index would reject every second version. "Unique among the current
  versions" cannot be a static constraint without a stored "current" flag that ZAC would have to maintain on
  every notification. One application check enforces the rule (D5).
- **No UNIQUE on `bpmn_process_definition_key`.** All versions of one zaaktype share the key.
  `BpmnService.findUniqueBpmnProcessDefinitionKeysFromConfigurations` already de-duplicates it, which shows
  that sharing is expected.

### D3. A2: move the zaak settings to the base

V101 moves `eindatum_gepland_waarschuwing` and `uiterlijke_einddatum_afdoening_waarschuwing` to
`zaaktype_configuration`. The first column gets the name `einddatum_gepland_waarschuwing` there, without the
typo of the CMMN column. It renames and re-points three child tables to the base: email parameters,
zaakafzender parameters, and mailtemplate parameters. The new names drop the `cmmn_` infix, as V86 and V89
did. The zaakafzender foreign key keeps `ON DELETE RESTRICT`, as V46 set it. The entity fields move up to
`ZaaktypeConfiguration`.

The versioning copies these settings for both engines. The einddatum-gepland window keeps its servicenorm
rule.

In the same PR, the readers of these settings move from the CMMN read service to the generic one:

- `ZaakRestService.listZaakWarnings` reads only the zaaktype UUID and the two windows of every configuration,
  through a projection query, so it loads no configuration entities.
- `ZaakRestService.listAfzendersVoorZaak`
- `ZaakTaskDueDateEmailNotificationService`
- `MailtemplateRESTService`
- `BrpClientService`

`BrpClientService` moves here too. Its fields were already on the base, and only the CMMN-typed lookup blocked
BPMN.

The BPMN REST path today assigns fields onto the found entity. It keeps that shape, so the moved fields
survive a BPMN `POST`.

A2 touches five Java classes. The A2-java PR converts them to Kotlin first, in two commits that keep the Git
history:

- `MailtemplateKoppelingRestService`
- `RESTMailtemplateKoppelingConverter`
- `RESTReplyToConverter`
- `RESTReplyTo`
- `MailtemplateRESTService`

`RESTReplyTo` becomes `RestReplyTo`, because detekt rejects all-caps acronyms in Kotlin class names. That renames
its OpenAPI schema, and the frontend follows the new name. Its boolean becomes `isSpeciaal`, also as JSON name, as in
`RestZaakAfzender`, and its fields stay non-null with defaults.
Moving classes changes which use of a shared schema SmallRye writes inline and which as a `$ref`. The contract
check therefore compares the two specs after it resolves every `$ref`.

### D4. A3: one entity, a process binding, and a CMMN extension

V102 replaces the inheritance:

- `zaaktype_configuration` loses `configuration_type`. Postgres drops the ENUM type when nothing uses it.
- `zaaktype_process_binding` gets these columns: `id`, `zaaktype_configuration_id` (UNIQUE, FK CASCADE),
  `process_engine` VARCHAR (`CMMN`/`BPMN`, CHECK constraint), and `definition_key` NOT NULL. It is filled from
  `zaaktype_cmmn_configuration.id_case_definition` where that is not null, and from
  `zaaktype_bpmn_configuration.bpmn_process_definition_key`.
- `zaaktype_cmmn_configuration` is renamed to `zaaktype_cmmn_extension`. It keeps `intake_mail` and
  `afronden_mail`, and its humantask and usereventlistener children. Those are CMMN plan item settings with no
  BPMN meaning. It drops `id_case_definition` and gets a UNIQUE foreign key column `zaaktype_configuration_id`.
  Migrated extensions keep the id of their configuration, so the foreign keys of the children keep their values.
  The children rename their foreign key column to `zaaktype_cmmn_extension_id`, because it points at the
  extension.
- `zaaktype_bpmn_configuration` is dropped.
- Before it splits the tables, V102 fails if a configuration has a row in the subclass table of the other engine.
  Hibernate writes a configuration only to the subclass table of its configuration type, and V85 gave the BPMN
  rows ids above the CMMN ids, so only a manual edit can create such a row.

`VARCHAR` with a CHECK constraint replaces the ENUM, because JPA maps the engine as `EnumType.STRING`, and
because a PG ENUM needs a type cast that the project configures nowhere.

The entity model:

- `ZaaktypeConfiguration` becomes concrete.
- `processBinding: ZaaktypeProcessBinding?` is a `@OneToOne`.
- `cmmnExtension: ZaaktypeCmmnExtension?` is a `@OneToOne`.

The table is `zaaktype_process_binding` and not `process_binding`, because every configuration table has the
`zaaktype_` prefix.

The engine enum is `ProcessEngine`. It clashes by name with Flowable's `ProcessEngine` only in `BpmnService`,
which imports the constant it needs. The engine of a configuration is `processBinding?.processEngine`. Rows that the REST API stores always have a
definition key. A legacy CMMN row without a case definition gets no binding. It is not valid for zaak creation
(spec), and the REST resources treat it as CMMN, as `GET /zaakafhandelparameters/{uuid}` does today when the
row is missing.

Alternative: put the engine and the key as two columns on `zaaktype_configuration`. That is RFC Option C of the
architecture note. It is simpler, and every configuration has at most one binding anyway. It is rejected here
because the story follows the RFC, and because the separate table keeps "no binding" a missing row instead of
two coupled nullable columns.

### D5. A3: one read service, one beheer service

`ZaaktypeConfigurationService` (read) and `ZaaktypeConfigurationBeheerService` (write) become concrete classes.
They replace the CMMN, BPMN, and generic services and the interface. Following the project rule for JPA code, a
`ZaaktypeConfigurationRepository` holds the queries and the persist/merge. The zaaktype notification moves to
the beheer service, which removes the circular dependency between the old CMMN read and beheer services.

| Function | Contract |
|---|---|
| `findConfiguration(zaaktypeUuid): ZaaktypeConfiguration?` | null when absent; cached |
| `readConfiguration(zaaktypeUuid): ZaaktypeConfiguration` | throws `ZaaktypeConfigurationNotFoundException` |
| `findCurrentConfiguration(zaaktypeOmschrijving): ZaaktypeConfiguration?` | newest by creatiedatum |
| `listCurrentConfigurationsByProductaanvraagtype(type): List<ZaaktypeConfiguration>` | newest per omschrijving, sorted by creatiedatum desc |
| `listConfigurationsBoundTo(engine): List<ZaaktypeConfiguration>` | for the BPMN list endpoint |
| `listDefinitionKeysBoundTo(engine): List<String>` | distinct definition keys, for the BPMN process definition admin |
| `listDeadlineWarningWindows()` | projection of three columns; cached |
| beheer `fetchConfiguration(zaaktypeUuid)` / `findStoredConfiguration(zaaktypeUuid)` | uncached reads for the REST write paths |
| beheer `storeConfiguration(configuration): ZaaktypeConfiguration` | bean validation of root and children; upsert by zaaktypeUuid; evicts caches |
| beheer `checkProductaanvraagtypeIsNotInUse(type, zaaktypeOmschrijving)` | one rule for both engines; excludes by omschrijving |
| beheer `updateZaaktypeConfiguration(zaaktypeUri)` / `upsertConfiguration(zaaktype)` | the zaaktype notification path (B3) |

These rules follow the project convention: `find` returns null and `read` throws. Every "current" query uses
the same correlated max-creatiedatum subquery, and every by-UUID query orders by creatiedatum. The
`REQUIRES_NEW` on the BPMN lookup goes away. It existed for a call from inside a Flowable transaction, and the
read service now reads through its cache.

The cache keeps the names `Caching.ZAC_ZAAKTYPECMMNCONFIGURATION_MANAGED` (by UUID, also for an absent
configuration) and `Caching.ZAC_ZAAKTYPECMMNCONFIGURATION` (the deadline warning windows). Cache names appear in the cache statistics
endpoints of `UtilRestService`, and those are part of the REST contract.

The REST resources map their payloads onto this one entity:

- `RestZaaktypeConfigurationConverter` maps `caseDefinition` to a CMMN binding.
- The BPMN resource maps `bpmnProcessDefinitionKey` to a BPMN binding.

The precedence contradiction disappears: `RestZaaktypeConverter`, `HealthCheckService`, and
`ZaakRestService.isValidForZaakCreation` all call `findConfiguration`. Validity is a function on the entity
(spec: "One answer for the configuration of a zaaktype"). For CMMN, `ZaakRestService` still adds the slow
zaaktype check against Open Zaak after the configuration check, as before.

Callers that read the CMMN plan item settings (plan items, task forms) take `configuration?.cmmnExtension` and
treat a missing configuration as before, when the CMMN read service returned an empty configuration.

### D6. B1: the process binding interface

```kotlin
interface ProcessBinding {
    val processEngine: ProcessEngine
    fun start(zaak: Zaak, zaaktype: ZaakType, definitionKey: String, processStartData: ProcessStartData)
    fun isZaaktypeReady(zaaktypeUri: URI): Boolean
    fun terminate(zaakUuid: UUID)
    fun delete(zaakUuid: UUID)
}

data class ProcessStartData(
    val caseData: Map<String, Any> = emptyMap(),
    val groupId: String? = null,
    val behandelaarId: String? = null,
    val communicatiekanaal: String? = null
)
```

`CmmnProcessBinding` wraps `CMMNService`, and `BpmnProcessBinding` wraps `BpmnService`. A
`ZaakProcessService` dispatcher injects `Instance<ProcessBinding>` and selects by
`configuration.processBinding.processEngine`. It has no `when`, so a third engine is one new class.

The dispatcher takes the configuration, not only the zaaktype UUID. The RFC lets the adapter look the
configuration up itself, but the caller already holds the configuration (`startZaak`, productaanvraag), and a
second lookup would be a second source of truth. The adapter gets only the definition key of the binding.

The callers pass the same `ProcessStartData`, and each adapter takes what its engine reads. CMMN gets the case
data (the aanvraaggegevens of a productaanvraag). BPMN gets the case data plus the groep, behandelaar and
communicatiekanaal as the zaak variables that the deployed process definitions read. The process variables
themselves do not change.

`isZaaktypeReady` moves the CMMN zaaktype health check out of `ZaakRestService`: the CMMN binding asks
`HealthCheckService`, and BPMN has no such check. Without a binding, a zaaktype is not ready.

`ProductaanvraagService` keeps its choice between the CMMN and the BPMN flow. The two flows differ in their order and
in the confirmation email, which B4 changes; only the process start inside each flow goes through the dispatcher.

`delete` serves `NotificationReceiver` on zaak delete. The zaak no longer exists in Open Zaak, so ZAC cannot
resolve its zaaktype. The receiver therefore calls `delete` on every binding. Each binding is a no-op when it
has no instance for the zaak. `BpmnService` gets a `deleteProcessInstance` that also removes the history,
mirroring `CMMNService.deleteCase`.

### D7. B2: resultaattype by omschrijving, expand only

V103 adds `niet_ontvankelijk_resultaattype_omschrijving` to `zaaktype_configuration` and
`resultaattype_omschrijving` to `zaaktype_completion_parameters`. Both columns are nullable.

- **Write path.** It writes both columns. The REST payload carries the UUID, and the converter resolves the
  omschrijving through `ZtcClientService.readResultaattype(uuid)`.
- **Read path.** The UUID that the REST API and the callers see is resolved per zaaktype version: list
  `readResultaattypen(zaaktypeUri)` and match the omschrijving, with the stored UUID as fallback while the
  omschrijving is null. Open Zaak guarantees that the omschrijving is unique within a zaaktype version, so no
  ambiguity check exists.
- **Backfill.** `ResultaattypeOmschrijvingBackfill` observes `@Initialized(ApplicationScoped.class)`, as
  `SolrDeployerService` does. It fills every null omschrijving through ZTC, row by row. It is idempotent and
  logs one summary line with the number of rows filled and the number left unresolved. A row stays null when
  ZTC fails or the resultaattype is gone, and the next start retries it. A Flyway Java migration is rejected,
  because it has no CDI access to the ZTC client and its credentials.

Dropping the UUID columns and the fallback is the contract step. It ships in a later release, after ops has
confirmed from the summary log that no row is left unresolved on every municipality environment.

### D8. B3: versioning as a pure function

`ZaaktypeHelperService` becomes `ZaaktypeConfigurationVersioning`. Its function
`createNextVersion(previous, newZaaktype): ZaaktypeConfiguration` returns a new, unsaved entity. It copies
these settings:

- every engine-agnostic setting
- the process binding
- the CMMN extension when one is present
- the resultaattype omschrijvingen

The CMMN servicenorm rule for the einddatum-gepland window stays. The copy drops the references whose
omschrijving does not exist in the new version.

`updateZaakbeeindigGegevens` is removed: with B2 an existing configuration needs no remap. The beheer
service's `upsertConfiguration` is the only caller. In order, it:

1. finds the existing configuration by UUID (done, nothing to change)
2. otherwise finds the current configuration by omschrijving
3. calls `createNextVersion`
4. stores the result
5. copies the SmartDocuments template mappings

A reflection unit test fills every `ZaaktypeConfiguration` member property with a non-default value, and
asserts that `createNextVersion` leaves none of them at its default. Identity fields are excluded. A new
field then fails the build until versioning copies it.

### D9. B4: confirmation email fallback

`SendConfirmationEmailDelegate.template` and `.from` become nullable `Expression?`. When an expression is
missing or resolves blank, the delegate uses the configuration's email parameters. If those are not
enabled, or name no template, it sends nothing and logs at FINE. The class keeps its name and package, because
deployed process definitions reference it. `ProductaanvraagService` does not start sending confirmation emails
for BPMN zaken. A BPMN process that wants the email already models the delegate, and a second email would be
a duplicate.

### D10. Tests

- **Unit tests.** Every new or changed service test is parameterised by engine, with one shared
  `listOf(CMMN, BPMN)` fixture in `AdminFixtures.kt`. It replaces the ad-hoc list in
  `ZaaktypeHelperServiceTest`.
- **Integration tests.**
  - Add a second version of one CMMN and one BPMN zaaktype to the Open Zaak seed data, with the same
    `identificatie` and a `datum_einde_geldigheid` on the first version. Use the existing
    `zaaktype-version-update-template.sql`.
  - `NotificationZaaktypeCompletionParametersTest` then runs against a real version chain.
  - Add itests for the productaanvraagtype check across engines and for BPMN cleanup on zaak delete.
- **Migration tests.** A test runs Flyway with `target` on an empty Testcontainers PostgreSQL up to the
  version before the chunk. Flyway runs as the `flyway/flyway` image of the Flyway version in
  `libs.versions.toml`, and the test reads and writes data with `psql` in the database container, so the
  itests need no JDBC driver or Flyway library. It inserts rows for each quarantine rule, plus valid rows, migrates to the
  latest version, and then asserts two things: valid rows are converted, and every invalid row is in the
  quarantine table with its full data.
- **Contract check.** The spec is not committed; `./gradlew generateOpenApiSpec` writes it to
  `build/generated/openapi/META-INF/openapi/openapi.json`. Before A1, generate it on `main` and save a copy
  as the baseline outside the repo. In every PR, regenerate it and `diff` it against that baseline. The diff
  must be empty.

## Risks / Trade-offs

- [V102 rewrites the configuration tables of every municipality] → One transactional migration (Postgres DDL
  is transactional). The itest stack and the migration tests run it against seed data. Before release, each
  migration chunk is deployed to the team's TEST environment, which holds real data. The migrations are
  forward-only and must be lossless on their own; no environment is expected to restore a database backup.
  Every migration copies data into its new place before it drops a column or a table. The migration tests
  assert that every value of every seeded row is still present after the migration, either in its new place
  or in the quarantine table.
- [Quarantined rows are forgotten, and the table stays forever] → Every quarantined row triggers a startup
  `WARNING`, and a follow-up ticket assigns the manual check and the drop (Migration Plan).
- [Unified "current" semantics change which row CMMN reads when versions collide] → The by-UUID lookup gains
  an ORDER BY creatiedatum. This only changes the result where two rows share a zaaktype UUID, which the
  UNIQUE(zaaktype_uuid) constraint forbids. Covered by a unit test.
- [The productaanvraagtype check now excludes by omschrijving for BPMN] → This is the intended fix. Existing
  data may hold two current configurations with one productaanvraagtype. Intake then uses the newest one and
  warns (spec), and the next save of either configuration fails the check until a beheerder resolves it.
- [B2 read path calls ZTC per resultaattype resolution] → `readResultaattypen` is cached per zaaktype URI, so
  each zaaktype version costs one ZTC call per cache period.
- [Backfill leaves rows unresolved when ZTC is down at startup] → The UUID fallback keeps behaviour unchanged,
  and the next start retries. The contract step waits for a clean summary.
- [`Instance<ProcessBinding>` resolution and `@Transactional` on adapters] → The adapters are
  `@ApplicationScoped` and delegate to the existing transactional services. A Weld unit or itest checks that
  both adapters resolve.
- [BPMN delete now removes process history] → History of a zaak that Open Zaak deleted has no owner. CMMN
  already removes it.

## Migration Plan

1. Merge A1 to B4 in order. Each release that contains A1–A3 runs V100–V102 at startup through
   `FlywayIntegrator`. After each migration chunk (A1, A2, A3, B2) merges, deploy it to the TEST environment
   with real data. Check the startup log, the quarantine table, and the configuration screens of a CMMN and a
   BPMN zaaktype before the next chunk merges.
2. After the release that contains A1–A3, check `zaaktype_configuration_migration_quarantine` on every
   environment. This is a manual step, tracked in a follow-up ticket:
   - restore the rows that turn out to be needed, with `jsonb_populate_record`
   - record the result per environment
   - when every environment is checked, drop the table by hand with `DROP TABLE`
3. After the release that contains B2, check the backfill summary log on every environment.
4. In a later release, a follow-up change drops the UUID columns and the fallback (outside this change).

Rollback is forward-only. A defect in a migration that has already run is fixed by a new forward migration
in the next release, never by editing the applied migration or by restoring a backup. Rows that a migration
moved out of the way are still in the quarantine table, so a forward fix can always reach them. Redeploying
the previous image is not possible after A2 or A3 has run, because that image expects the old tables.
