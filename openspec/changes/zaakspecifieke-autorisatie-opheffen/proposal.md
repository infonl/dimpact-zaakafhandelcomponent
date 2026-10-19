## Why

[PZ-10200](https://dimpact.atlassian.net/browse/PZ-10200) lets an employee mark a zaak as zaakspecifiek
geautoriseerd, but deliberately left no way back: the update refuses to remove the marking. A zaak that no longer
needs to be shielded, or that was marked by mistake, stays restricted for good.
[PZ-12022](https://dimpact.atlassian.net/browse/PZ-12022) lets an employee remove the marking again.

## What Changes

- **Removing the marking while editing the zaakgegevens**: an employee with `wijzigen` on a zaakspecifiek
  geautoriseerde zaak can untick *Zaak zaakspecifiek autoriseren* in the edit form. **BREAKING** for the
  REST contract only in the sense that `isZaakspecifiekGeautoriseerd: false` on a marked zaak is now accepted
  instead of refused with `msg.error.zaakspecifieke.autorisatie.cannot.be.lifted`; that error code is removed.
- **Saving asks for confirmation** in a pop-up without a reden field; the edit form already asks for a reden.
- **Removing deletes** every *Zaakspecifiek geautoriseerde medewerker* rol and the `ZAAK_GEAUTORISEERD`
  zaakeigenschap of the zaak in Open Zaak, with the reden as audit toelichting, and reindexes the zaak, its
  taken and its documenten. The zaak then behaves as a normal zaak again: every employee authorised for the
  zaaktype has their rechten on it, it can be released, the lock icon and the *Medewerkers* tab disappear.
- **Marking the zaak again** starts from an empty set of extra medewerkers.
- **The zaakhistorie** shows each removed *Zaakspecifiek geautoriseerde medewerker* with the reden.

## Capabilities

### New Capabilities

None.

### Modified Capabilities

- `zaakspecifieke-autorisatie-activatie`: the marking can be removed again.

## Impact

- **Backend**: `ZrcClient`/`ZrcClientService` get a delete for a zaakeigenschap; `ZaakspecifiekeAutorisatieService`
  decides on and performs the removal; `ZaakRestService.updateZaak` calls it. The exception
  `ZaakspecifiekeAutorisatieCannotBeLiftedException` and its error code go.
- **Frontend**: the checkbox in the zaakgegevens edit form stays enabled on a marked zaak, a confirmation dialog,
  i18n texts.
- **Docs**: gebruikershandleiding.
- **Solr, OPA, Open Zaak catalogus**: no change.
