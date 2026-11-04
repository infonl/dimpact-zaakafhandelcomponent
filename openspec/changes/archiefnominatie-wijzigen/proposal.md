## Why

A recordmanager or beheerder cannot correct the archiefnominatie of an afgehandelde zaak: Open Zaak derives it
from the resultaattype when the zaak is closed and ZAC offers no way to change it.
[PZ-9845](https://dimpact.atlassian.net/browse/PZ-9845) lets them switch it between *Blijvend bewaren* and
*Vernietigen*, with a reden, to fix mistakes, meet the needs of a proceseigenaar or apply a legal exception.

The story is run as a spike: ZAC changes the archiefnominatie only, and records what Open Zaak does and does not do
with the other archiefgegevens (see `design.md`).

## What Changes

- **New right `wijzigen_archief`** in `zaak-rechten.rego`, for the `recordmanager` and `beheerder` roles on an
  afgehandelde zaak, exposed as `canWijzigenArchief` in the zaak rechten.
- **New endpoint** `PUT /rest/zaken/zaak/{uuid}/archiefnominatie` with the new archiefnominatie and a mandatory
  reden. ZAC patches the archiefnominatie in Open Zaak with the reden as audit toelichting and returns the updated
  zaak.
- **Rights matrix** in `accessControlPolicies.md` and the gebruikershandleiding list the new right.
- **Later, once designed**: the *Wijzigen archiefgegevens* action in the zaak action menu and its pop-up.

## Capabilities

### New Capabilities

- `zaak-archiefnominatie-wijzigen`: changing the archiefnominatie of an afgehandelde zaak.

### Modified Capabilities

None.

## Impact

- **Backend**: `zaak-rechten.rego`, `ZaakRechten`, `RestZaakRechten`, a new `ZaakArchiefRestService` and its
  request model.
- **Frontend**: generated types only; the action and pop-up follow when the design
  ([PZ-13224](https://dimpact.atlassian.net/browse/PZ-13224)) is ready.
- **Open Zaak**: ZAC's application needs the `zaken.geforceerd-bijwerken` scope, which it already needs to change
  afgehandelde zaken.
- **Docs**: rights matrix and gebruikershandleiding.
- **Solr**: no change; the zaak is reindexed from the Open Notificaties notification, as for every zaak update.
