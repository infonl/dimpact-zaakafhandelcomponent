## Why

After a beheerder publishes a new version of a zaaktype in Open Zaak, the Zaakafhandelparameters page kept showing the list it had read before, for up to 5 minutes ([PZ-13638](https://dimpact.atlassian.net/browse/PZ-13638)). The row of that zaaktype still linked to the previous version, which already has zaken, so its procesmodel could not be changed. Only reloading the browser page showed the new version, whose procesmodel can be changed.

## What Changes

- The Zaakafhandelparameters page reads its list again each time the beheerder opens it.
- The page also reads its list again when the beheerder comes back to the browser tab, for example from Open Zaak.

## Capabilities

### New Capabilities
- none

### Modified Capabilities
- `zaaktype-configuration`: the list page shows the configurations as they are when the beheerder opens the page or comes back to it.

## Impact

- Frontend: the list query of `ParametersComponent`, and its spec.
- No backend, REST, OpenAPI or database change; the test-data scripts do not change.
- Decision D6 of `paged-zaaktype-configuration-list` said that a page stays fresh for 5 minutes. That no longer holds.
