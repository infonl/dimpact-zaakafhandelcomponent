## Context

A medewerker has individual access to a zaakspecifiek geautoriseerde zaak when they hold the behandelaar rol
or a *Zaakspecifiek geautoriseerde medewerker* (ZGM) rol on it (`ZaakToewijzing.geautoriseerdeMedewerkerIds`).
ZGM rollen are added for manually added medewerkers, previous zaakbehandelaars and current and previous
taakbehandelaars. The *Medewerkers* tab lists both kinds of rol through
`GET /rest/zaken/zaak/{uuid}/zaakspecifiek-geautoriseerde-medewerkers`.

Once a ZGM rol is deleted, everything else follows, as it did for adding:
- `PolicyService` reads the rollen live, so OPA refuses access at once;
- a reindex removes the medewerker from `zaakGeautoriseerdeMedewerkers`, so werkvoorraden and zoekresultaten
  follow;
- the zaakhistorie shows a deleted rol from the Open Zaak audittrail as *ontkoppeld*.

## Goals / Non-Goals

**Goals:** every requirement in `specs/zaakspecifieke-autorisatie-extra-medewerkers/spec.md`, reusing the
existing lock, reindex and reden dialog.

**Non-Goals:** lifting the zaakspecifieke autorisatie of the zaak (PZ-12022); removing access that comes from
the `zaakspecifiek_geautoriseerd` application role.

## Decisions

### Ontkoppelen is an action in the Medewerkers tab

The tab lists who has access, so the action sits on the row, like *Betrokkene ontkoppelen* in the
betrokkenen tab. It opens the shared reden dialog with a required, multiline reden.

### The backend decides which rows can be ontkoppeld

Each `RestGeautoriseerdeMedewerker` gets `isOntkoppelbaar`: true for a ZGM rol whose medewerker is neither the
zaakbehandelaar nor the behandelaar of an open taak of the zaak. The frontend shows the action for those rows
when the employee has `wijzigen`. It needs no knowledge of taken or roltypen.

### Ontkoppelen works per medewerker, not per rol

`DELETE /rest/zaken/zaak/{uuid}/zaakspecifiek-geautoriseerde-medewerkers/{medewerkerId}` with `{ reden }`:
1. asserts `wijzigen` on the zaak;
2. refuses a zaak that is not zaakspecifiek geautoriseerd (existing
   `ERROR_CODE_ZAAK_NOT_ZAAKSPECIFIEK_GEAUTORISEERD`);
3. under the per-zaak lock, refuses the zaakbehandelaar or the behandelaar of an open taak
   (`ERROR_CODE_MEDEWERKER_CANNOT_BE_ONTKOPPELD`) and a medewerker without a ZGM rol
   (`ERROR_CODE_MEDEWERKER_NOT_ZAAKSPECIFIEK_GEAUTORISEERD`);
4. deletes every ZGM rol of the medewerker with the reden as audit toelichting, and reindexes.

Deleting every rol covers the duplicate a concurrent addition on two pods can leave behind (see the PZ-12023
design). Keying on the medewerker rather than the rol UUID also keeps the frontend free of rol UUIDs.

### Who may ontkoppelen

The story names the zaakbehandelaar, the recordmanager and the beheerder. They are exactly the roles that get
`wijzigen` on a zaak, so ontkoppelen asserts `wijzigen`, as adding does.

## Risks / Trade-offs

- [A taak is assigned to the medewerker while they are being ontkoppeld] → Assigning an existing taak grants
  the ZGM rol and assigns the taak under the per-zaak lock, and ontkoppelen reads the open taken under it, so
  on one pod one of the two wins cleanly. Two windows remain: starting a taak from a plan item grants the rol
  before it creates the taak, and the lock is in memory, so two pods are not serialised. In both cases a new
  taakbehandelaar can lose access to the zaak while their taak stays assigned; reassigning the taak restores
  it. Holding the lock while a plan item starts would also hold it while mail is sent, and a lock across pods
  needs a shared store, so this is accepted, like the same trade-off for adding.
- [An employee ontkoppelt themselves] → Allowed. They lose access, which is what they asked for.

## Migration Plan

None.

## Open Questions

None.
