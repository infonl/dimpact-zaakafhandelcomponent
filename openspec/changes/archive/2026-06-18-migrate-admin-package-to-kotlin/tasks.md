## 1. Commit the already-deleted exception file

- [x] 1.1 Stage and commit the deletion of `src/main/java/net/atos/zac/admin/exception/ResulttaattypeNotFoundException.java` (currently an unstaged `D` in git status); verify no other code imports `net.atos.zac.admin.exception.ResulttaattypeNotFoundException` before committing

## 2. Add unit tests for MailTemplateKoppelingenService

- [x] 2.1 Create `src/test/kotlin/net/atos/zac/admin/MailTemplateKoppelingenServiceTest.kt` with a `BehaviorSpec` using MockK. Mock `EntityManager`, `CriteriaBuilder`, `CriteriaQuery<ZaaktypeCmmnMailtemplateParameters>`, `Root<ZaaktypeCmmnMailtemplateParameters>`, and `TypedQuery<ZaaktypeCmmnMailtemplateParameters>`. Construct the service with `MailTemplateKoppelingenService(entityManager)`.
- [x] 2.2 Add a `Given("A mail template koppeling exists")` block:
  - `When("find is called with the id")` → mock `entityManager.find(ZaaktypeCmmnMailtemplateParameters::class.java, id)` returning a non-null instance → `Then` assert result is `Optional.of(instance)` (or non-null if already testing the future Kotlin version)
  - `When("find is called with an unknown id")` → mock `entityManager.find` returning null → `Then` assert result is `Optional.empty()`
- [x] 2.3 Add `When("delete is called with an existing id")` → verify `entityManager.remove` is called with the found entity
- [x] 2.4 Add `Given("storeMailtemplateKoppeling with new entity (no id)")` → mock `entityManager.persist` → verify `entityManager.persist` called, returned object equals the input
- [x] 2.5 Add `Given("storeMailtemplateKoppeling with existing entity (id present)")` → mock `entityManager.find` returning existing, mock `entityManager.merge` → verify `entityManager.merge` called
- [x] 2.6 Add `Given("readMailtemplateKoppeling with existing id")` → mock `entityManager.find` returning entity → `Then` assert entity returned
- [x] 2.7 Add `Given("readMailtemplateKoppeling with unknown id")` → mock `entityManager.find` returning null → `Then` assert `shouldThrow<RuntimeException>` with message containing class name and id
- [x] 2.8 Add `Given("listMailtemplateKoppelingen")` → mock criteria query chain returning a two-element list → `Then` assert list size is 2

## 3. Add unit tests for ZaaktypeCmmnConfigurationService

- [x] 3.1 Create `src/test/kotlin/net/atos/zac/admin/ZaaktypeCmmnConfigurationServiceTest.kt` with a `BehaviorSpec`. Mock `ZaaktypeCmmnConfigurationBeheerService`. Construct the service with `ZaaktypeCmmnConfigurationService(zaaktypeCmmnConfigurationBeheerService)`.
- [x] 3.2 Add `Given("a zaaktype UUID")`:
  - `When("readZaaktypeCmmnConfiguration is called")` → mock `zaaktypeCmmnConfigurationBeheerService.fetchZaaktypeCmmnConfiguration(uuid)` → `Then` assert returned config matches mock; call again and verify `fetchZaaktypeCmmnConfiguration` was called only once (cache hit on second call)
- [x] 3.3 Add `When("listZaaktypeCmmnConfiguration is called twice")` → mock `zaaktypeCmmnConfigurationBeheerService.listZaaktypeCmmnConfiguration()` → verify called only once (list cache hit on second call)
- [x] 3.4 Add `When("cacheRemoveZaaktypeCmmnConfiguration is called")` → call `readZaaktypeCmmnConfiguration`, then `cacheRemoveZaaktypeCmmnConfiguration`, then `readZaaktypeCmmnConfiguration` again → verify `fetchZaaktypeCmmnConfiguration` called twice
- [x] 3.5 Add `When("clearManagedCache is called")` → populate cache, call `clearManagedCache()`, then read again → verify beheer service called again; assert return value contains `ZAC_ZAAKTYPECMMNCONFIGURATION_MANAGED`
- [x] 3.6 Add `When("clearListCache is called")` → populate list cache, call `clearListCache()`, then list again → verify beheer service called again; assert return value contains `ZAC_ZAAKTYPECMMNCONFIGURATION`

## 4. Migrate FormulierVeldDefinitie to Kotlin

- [x] 4.1 Create `src/main/kotlin/nl/info/zac/admin/model/FormulierVeldDefinitie.kt` as a Kotlin `enum class` in package `nl.info.zac.admin.model`. Single entry `ADVIES(ReferenceTable.SystemReferenceTable.ADVIES)`. Constructor parameter becomes `val defaultTabel: ReferenceTable.SystemReferenceTable`.
- [x] 4.2 Delete `src/main/java/net/atos/zac/admin/model/FormulierVeldDefinitie.java`.
- [x] 4.3 Update `FormulierDefinitie.java` import from `net.atos.zac.admin.model.FormulierVeldDefinitie` to `nl.info.zac.admin.model.FormulierVeldDefinitie` (temporary; file is deleted in task 5).

## 5. Migrate FormulierDefinitie to Kotlin

- [x] 5.1 Create `src/main/kotlin/nl/info/zac/admin/model/FormulierDefinitie.kt` as a Kotlin `enum class` in package `nl.info.zac.admin.model`. Each entry has constructor parameter `val veldDefinities: Set<FormulierVeldDefinitie>`. Implement entries: `DEFAULT_TAAKFORMULIER(emptySet())`, `AANVULLENDE_INFORMATIE(emptySet())`, `ADVIES(setOf(FormulierVeldDefinitie.ADVIES))`, `EXTERN_ADVIES_VASTLEGGEN(emptySet())`, `EXTERN_ADVIES_MAIL(emptySet())`, `GOEDKEUREN(emptySet())`, `DOCUMENT_VERZENDEN_POST(emptySet())`.
- [x] 5.2 Delete `src/main/java/net/atos/zac/admin/model/FormulierDefinitie.java`.
- [x] 5.3 Update all import sites from `net.atos.zac.admin.model.FormulierDefinitie` → `nl.info.zac.admin.model.FormulierDefinitie`:
  - `src/main/kotlin/nl/info/zac/app/planitems/converter/FormulierKoppelingConverter.kt`
  - `src/main/kotlin/nl/info/zac/app/planitems/converter/RESTPlanItemConverter.kt`
  - `src/main/kotlin/nl/info/zac/app/planitems/model/DefaultHumanTaskFormulierKoppeling.kt`
  - `src/main/kotlin/nl/info/zac/app/planitems/model/RESTPlanItem.kt`
  - `src/main/kotlin/nl/info/zac/app/planitems/PlanItemsRestService.kt`
- [x] 5.4 Update `FormulierKoppelingConverter.kt` to call `.veldDefinities` as a property (not `getVeldDefinities()`) since the Kotlin enum uses a property.
- [x] 5.5 Update `FormulierVeldDefinitie` import in `FormulierKoppelingConverter.kt` from `net.atos.zac.admin.model` → `nl.info.zac.admin.model`.

## 6. Migrate HumanTaskReferentieTabel to Kotlin

- [x] 6.1 Create `src/main/kotlin/nl/info/zac/admin/model/HumanTaskReferentieTabel.kt` in package `nl.info.zac.admin.model`. Annotate with `@Entity`, `@Table(schema = SCHEMA, name = "humantask_referentie_tabel")`, `@SequenceGenerator(...)`, `@AllOpen`. Fields: `@Id @GeneratedValue @Column var id: Long? = null`, `@NotNull @ManyToOne @JoinColumn var tabel: ReferenceTable? = null`, `@NotNull @ManyToOne @JoinColumn var humantask: ZaaktypeCmmnHumantaskParameters? = null`, `@NotBlank @Column var veld: String? = null`. Add secondary constructor `constructor(veld: String, tabel: ReferenceTable)`. Implement `equals` (based on `tabel` and `veld`) and `hashCode` matching the Java originals.
- [x] 6.2 Delete `src/main/java/net/atos/zac/admin/model/HumanTaskReferentieTabel.java`.
- [x] 6.3 Update `src/main/java/net/atos/zac/app/admin/converter/RestHumanTaskReferenceTableConverter.java`: change import from `net.atos.zac.admin.model.HumanTaskReferentieTabel` → `nl.info.zac.admin.model.HumanTaskReferentieTabel`.
- [x] 6.4 Update `src/test/kotlin/nl/info/zac/admin/model/AdminFixtures.kt`: change import from `net.atos.zac.admin.model.HumanTaskReferentieTabel` → `nl.info.zac.admin.model.HumanTaskReferentieTabel`.
- [x] 6.5 Verify `src/test/kotlin/net/atos/zac/admin/model/HumanTaskReferentieTabelTest.kt` and `HumanTaskParametersTest.kt` still compile (they already import `HumanTaskReferentieTabel` via the fixture).

## 7. Migrate MailTemplateKoppelingenService to Kotlin

- [x] 7.1 Create `src/main/kotlin/nl/info/zac/admin/MailTemplateKoppelingenService.kt` in package `nl.info.zac.admin`. Annotate `@ApplicationScoped @Transactional`. Constructor-inject `EntityManager`. Replace `Optional<T>` return of `find` with `T?`. Replace verbose `if (x != null) Optional.of(x) else Optional.empty()` with `entityManager.find(...)`. Replace `readMailtemplateKoppeling` null check with `?: throw RuntimeException(...)`. Replace criteria query in `listMailtemplateKoppelingen` with the idiomatic Kotlin form (same logic).
- [x] 7.2 Delete `src/main/java/net/atos/zac/admin/MailTemplateKoppelingenService.java`.
- [x] 7.3 Update `src/main/java/net/atos/zac/app/admin/MailtemplateKoppelingRESTService.java`: change import from `net.atos.zac.admin.MailTemplateKoppelingenService` → `nl.info.zac.admin.MailTemplateKoppelingenService`.
- [x] 7.4 Update the unit test created in task 2 (`MailTemplateKoppelingenServiceTest.kt`): change import to `nl.info.zac.admin.MailTemplateKoppelingenService`. Adjust assertions for nullable return type (task 2.2: `find` now returns `T?`, not `Optional<T>`).

## 8. Migrate ZaaktypeCmmnConfigurationService to Kotlin

- [x] 8.1 Create `src/main/kotlin/nl/info/zac/admin/ZaaktypeCmmnConfigurationService.kt` in package `nl.info.zac.admin`. Annotate `@ApplicationScoped`. Implement `Caching`. Move `INADMISSIBLE_TERMINATION_ID` and `INADMISSIBLE_TERMINATION_REASON` to `companion object`. Move `CACHES` map and `createCache` to `companion object` (or keep as instance-level private if preferred; both work). Constructor-inject `ZaaktypeCmmnConfigurationBeheerService`. Keep all public method signatures identical.
- [x] 8.2 Delete `src/main/java/net/atos/zac/admin/ZaaktypeCmmnConfigurationService.java`.
- [x] 8.3 Update import sites from `net.atos.zac.admin.ZaaktypeCmmnConfigurationService` → `nl.info.zac.admin.ZaaktypeCmmnConfigurationService` in:
  - `src/main/kotlin/nl/info/zac/app/planitems/converter/RESTPlanItemConverter.kt`
  - `src/main/kotlin/nl/info/zac/app/planitems/PlanItemsRestService.kt`
  - `src/main/kotlin/nl/info/zac/app/util/UtilRestService.kt`
  - `src/main/kotlin/nl/info/zac/app/admin/ZaaktypeCmmnConfigurationRestService.kt`
  - `src/main/kotlin/nl/info/zac/app/task/converter/RestTaskConverter.kt`
  - `src/main/kotlin/nl/info/zac/app/zaak/ZaakRestService.kt`
  - `src/main/kotlin/nl/info/zac/app/zaak/converter/RestZaaktypeConverter.kt`
  - `src/main/kotlin/nl/info/zac/admin/ZaaktypeCmmnConfigurationBeheerService.kt`
- [x] 8.4 Update constant import sites `net.atos.zac.admin.ZaaktypeCmmnConfigurationService.INADMISSIBLE_TERMINATION_ID` and `INADMISSIBLE_TERMINATION_REASON` → `nl.info.zac.admin.ZaaktypeCmmnConfigurationService.INADMISSIBLE_TERMINATION_ID` etc. in `ZaaktypeCmmnConfigurationRestService.kt` and `ZaakRestService.kt`.
- [x] 8.5 Update the unit test created in task 3 (`ZaaktypeCmmnConfigurationServiceTest.kt`): change import to `nl.info.zac.admin.ZaaktypeCmmnConfigurationService`.

## 9. Verify

- [x] 9.1 Run `./gradlew compileKotlin` and resolve any remaining compile errors.
- [x] 9.2 Run `./gradlew test --tests "nl.info.zac.admin.*" --tests "net.atos.zac.admin.*"` and confirm all tests pass.
- [x] 9.3 Run `./gradlew spotlessApply detektApply` on the new Kotlin files.
- [x] 9.4 Verify no remaining `net.atos.zac.admin` imports exist in `src/`: `grep -r "net.atos.zac.admin" src/`.
