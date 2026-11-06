## Why

[PZ-13306](https://dimpact.atlassian.net/browse/PZ-13306): ZAC cannot derive a brondatum for a resultaattype whose
afleidingswijze is *Ander datumkenmerk* (`ander_datumkenmerk`) or *Zaakobject* (`zaakobject`). When Open Zaak is
configured that way, problems only appear when a behandelaar closes a zaak. A functioneel beheerder must be warned
beforehand, in the inrichtingscheck.

## What Changes

- **New inrichtingscheck error**: a CMMN zaaktype with a resultaattype whose afleidingswijze is
  `ander_datumkenmerk` or `zaakobject` is reported as not valid, with an error that names those resultaattypes.
  Because CMMN zaaktypes must pass the check to be ready, no zaak of this zaaktype can be created until it is fixed.
- **REST contract**: `RestZaaktypeInrichtingscheck` gets a list `resultaattypesMetNietOndersteundeBrondatum`.
- **Frontend**: the inrichtingscheck page shows the error in the expanded row of the zaaktype.
- **Handleiding**: states that ZAC does not support the afhandelwijzen *Ander datumkenmerk* and *Zaakobject*.

## Capabilities

### New Capabilities

None.

### Modified Capabilities

- `zaaktype-inrichtingscheck`: adds the check for unsupported afleidingswijzen of the brondatum.

## Impact

- **Backend**: `HealthCheckService`, `ZaaktypeInrichtingscheck`, `RestZaaktypeInrichtingscheck` and its converter.
- **Frontend**: `inrichtingscheck.component` (html, spec), i18n `nl.json` and `en.json`, generated types.
- **Docs**: `docs/manuals/inrichting-zaakafhandelcomponent/inrichting-zaakafhandelcomponent.md`.
- **Database, Solr, OPA, Open Zaak**: no change.
