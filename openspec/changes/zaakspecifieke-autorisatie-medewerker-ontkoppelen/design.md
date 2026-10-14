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

### A medewerker can ontkoppel themselves

A zaakspecifiek geautoriseerde medewerker who no longer needs the zaak can ontkoppel themselves. The zaak keeps
its behandelaar, who cannot be ontkoppeld. Afterwards the employee usually has no access to the zaak they are
looking at, so the zaak view does not refresh it but goes to the dashboard.

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
design). Keying on the medewerker rather than the rol UUID also keeps the frontend free of rol UUIDs. For the
same reason the list of medewerkers shows such a medewerker once.

The deletion, the reindex and the Solr commit all happen under the per-zaak lock, so a concurrent taak
assignment on the same pod cannot write an older set of rollen to the index after them. The commit makes the
medewerker leave the search results at once instead of at Solr's next auto-commit.

### Who may ontkoppelen

The story names the zaakbehandelaar, the recordmanager and the beheerder. Ontkoppelen asserts `wijzigen`, as
adding does, so the same employees can add and ontkoppel. That right is a little broader than the story: the
`coordinator` also has it on an open zaak, and `systeemrol_behandelaar_alle_zaaktypen` on every zaak (see
`docs/solution-architecture/accessControlPolicies.md`). This is accepted: a coordinator can already hand the
zaak to another behandelaar, which changes who has access just as much, and a separate right for one action
would split adding and ontkoppelen across two policies.

## Risks / Trade-offs

- [A taak is assigned to the medewerker while they are being ontkoppeld] → Assigning an existing taak grants
  the ZGM rol and assigns the taak under the per-zaak lock, and ontkoppelen reads the open taken under it, so
  on one pod one of the two wins cleanly. Two windows remain: starting a taak from a plan item grants the rol
  before it creates the taak, and the lock is in memory, so two pods are not serialised. In both cases a new
  taakbehandelaar can lose access to the zaak while their taak stays assigned. Releasing the taak and assigning
  it to them again restores it; assigning it to the same medewerker without releasing it first does not, since
  an unchanged assignee gets no new rol. Holding the lock while a plan item starts would also hold it while mail is sent, and a lock across pods
  needs a shared store, so this is accepted, like the same trade-off for adding.
- [Reindexing one of the zaak, its taken or its documenten fails after the rollen are deleted] → The request
  still succeeds and the failure is logged, as for adding a medewerker and assigning a taak, which use the same
  reindex. Opening the object is refused at once anyway, because the policy reads the rollen live; only its
  search result stays visible until the next reindex. Failing the request instead would leave the rollen
  deleted and give the user an error that a retry cannot fix.

## Migration Plan

None.

## Open Questions

None.
