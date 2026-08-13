## Why

`net.atos.client.zgw.zrc.model` (24 Java files, including the `zaakobjecten` sub-package) is one of the last Java packages in the ZGW ZRC client layer. Project convention requires Java code to be converted to Kotlin when touched. None of the 24 classes have any unit test coverage today, even though several contain non-trivial logic — `Rol`'s polymorphic equality contract, five `Rol` subclasses' betrokkene-identity resolution (`getNaam`/`getIdentificatienummer`/`equalBetrokkeneIdentificatie`/`hashCodeBetrokkeneIdentificatie`), `Zaakobject`'s `isBagObject`/equality, and `ZaakListParameters`' enum-to-querystring mapping. Migrating without first establishing coverage risks silently changing behaviour used across zaak/rol/BAG-object handling.

## What Changes

- Add unit tests for the classes with non-trivial logic that currently have zero coverage: `Rol` (equality/hashCode contract), `RolMedewerker`, `RolNatuurlijkPersoon`, `RolNietNatuurlijkPersoon`, `RolOrganisatorischeEenheid`, `RolVestiging` (identity resolution per betrokkene type), `ZaakInformatieobject` (`getZaakUUID`), `Zaakobject` (`isBagObject`, equality), and the six `Zaakobject*`/`Object*` leaf classes' `getWaarde()` delegation
- Migrate all 24 classes in `net.atos.client.zgw.zrc.model` (and its `zaakobjecten` sub-package) to Kotlin at `nl.info.client.zgw.zrc.model`, using Kotlin idioms (data classes for simple holders, sealed/abstract classes preserved where polymorphism is required, `?.let`/`?:` instead of null checks)
- Update all import sites across `src/main/java` and `src/main/kotlin` (75+ files) to point to the new `nl.info.client.zgw.zrc.model` package

## Capabilities

### New Capabilities

- `zrc-domain-model`: Documents the testable behaviour contracts of the ZRC client's `Rol`/`Zaakobject` domain model (betrokkene-identity equality and naam/identificatienummer resolution per rol type, BAG-object classification, zaakinformatieobject UUID extraction, list-parameter query-string mapping) that previously had no spec or test coverage. No behaviour changes — this formalizes existing behaviour as part of the Kotlin migration.

### Modified Capabilities

_None — this is a mechanical Kotlin migration of internal client model classes with no change to externally observable behaviour._

## Impact

- `Rol<T>` and its 5 subclasses (`RolMedewerker`, `RolNatuurlijkPersoon`, `RolNietNatuurlijkPersoon`, `RolOrganisatorischeEenheid`, `RolVestiging`) — delete Java, add Kotlin equivalents; widely used across zaak/rol REST services, converters, and search indexing
- `RolListParameters`, `ZaakInformatieobjectListParameters`, `ZaakListParameters` — delete Java, add Kotlin equivalents; used by ZRC client query methods
- `ZaakInformatieobject` — delete Java, add Kotlin equivalent; used by document/zaak REST services and converters
- `Zaakobject` and its `zaakobjecten` sub-package (13 files: base class, `ZaakobjectMetObjectIdentificatie<T>`, 6 `Zaakobject*` leaf classes, 6 `Object*` identificatie holders, `ZaakobjectListParameters`) — delete Java, add Kotlin equivalents; used by BAG converters, search indexing, and zaakobject REST services
- No API contract change, no DB schema change, no new dependencies
