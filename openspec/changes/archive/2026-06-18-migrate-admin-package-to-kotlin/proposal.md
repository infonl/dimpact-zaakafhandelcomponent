## Why

`net.atos.zac.admin` is the last remaining Java package in the admin domain. Project convention requires all Java to be converted to Kotlin. The two service classes (`MailTemplateKoppelingenService`, `ZaaktypeCmmnConfigurationService`) have no unit tests, making a safe migration difficult without first establishing coverage. The three model classes (`FormulierDefinitie`, `FormulierVeldDefinitie`, `HumanTaskReferentieTabel`) are also still Java and referenced by several Kotlin files that currently import from `net.atos.zac.admin.model`.

## What Changes

- Add unit tests for `MailTemplateKoppelingenService` (all 5 public methods) and `ZaaktypeCmmnConfigurationService` (cache read, list, invalidation, clear, and cache statistics) using the BehaviorSpec/MockK pattern already established in `nl.info.zac.admin`
- Migrate both service classes to Kotlin at `nl.info.zac.admin`, using constructor injection and Kotlin idioms (no `Optional`, use nullable types; `?.let`/`?: throw` instead of explicit null checks)
- Migrate `FormulierDefinitie`, `FormulierVeldDefinitie`, and `HumanTaskReferentieTabel` model classes to Kotlin at `nl.info.zac.admin.model`
- Commit the already-deleted `ResulttaattypeNotFoundException.java` (currently unstaged `D` in working tree)
- Update all import sites across `src/main/kotlin` and `src/main/java` to point to the new `nl.info.zac.admin` package

## Capabilities

### Modified Capabilities

- `admin-services`: `MailTemplateKoppelingenService` and `ZaaktypeCmmnConfigurationService` move to `nl.info.zac.admin` — same behaviour, Kotlin implementation
- `admin-models`: `FormulierDefinitie`, `FormulierVeldDefinitie`, `HumanTaskReferentieTabel` move to `nl.info.zac.admin.model` — same behaviour, Kotlin implementation

## Impact

- `MailTemplateKoppelingenService` — delete Java, add Kotlin equivalent; update one Java caller (`MailtemplateKoppelingRESTService.java`)
- `ZaaktypeCmmnConfigurationService` — delete Java, add Kotlin equivalent; update ~8 Kotlin callers across app layer
- `FormulierDefinitie` / `FormulierVeldDefinitie` — delete Java, add Kotlin equivalents; update ~5 Kotlin callers in `app/planitems`
- `HumanTaskReferentieTabel` — delete Java, add Kotlin equivalent; update `RestHumanTaskReferenceTableConverter.java` caller
- `ResulttaattypeNotFoundException.java` — already deleted from working tree; commit that deletion
- No API contract change, no DB schema change, no new dependencies
