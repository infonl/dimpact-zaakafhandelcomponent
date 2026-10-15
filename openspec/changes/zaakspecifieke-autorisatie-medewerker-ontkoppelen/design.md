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
- the zaakhistorie shows a deleted rol from the Open Zaak audittrail as *detached*.

## Goals / Non-Goals

**Goals:** every requirement in `specs/zaakspecifieke-autorisatie-extra-medewerkers/spec.md`, reusing the
existing lock, reindex and reden dialog.

**Non-Goals:** lifting the zaakspecifieke autorisatie of the zaak (PZ-12022); removing access that comes from
the `zaakspecifiek_geautoriseerd` application role.

## Decisions

### Detaching is an action in the Medewerkers tab

The tab lists who has access, so the action sits on the row, like *Betrokkene ontkoppelen* in the
betrokkenen tab. It opens the shared reden dialog with a required, multiline reden.

### The backend decides which rows can be detached

Each `RestGeautoriseerdeMedewerker` gets `isDetachable`: true for a ZGM rol whose medewerker is neither the
zaakbehandelaar nor the behandelaar of an open taak of the zaak. The frontend shows the action for those rows
when the employee has `wijzigen`. It needs no knowledge of taken or roltypen.

The list leaves out rollen without a medewerker identificatie. Open Zaak allows them, but only rollen created
outside ZAC can lack one, and such a rol cannot be detached by id. Every listed medewerker therefore has an id,
and `RestGeautoriseerdeMedewerker.id` is not nullable.

Detaching, adding a medewerker and granting a taakbehandelaar access all reindex the zaak, its taken and its
documenten through one `IndexingService` function, under the per-zaak lock.

### A medewerker can detach themselves

A zaakspecifiek geautoriseerde medewerker who no longer needs the zaak can detach themselves. The zaak keeps
its behandelaar, who cannot be detached. Afterwards the employee usually has no access to the zaak they are
looking at, so the zaak view does not refresh it but goes to the dashboard.

### Detaching works per medewerker, not per rol

`DELETE /rest/zaken/zaak/{uuid}/zaakspecifiek-geautoriseerde-medewerkers/{medewerkerId}` with `{ reden }`:
1. asserts `wijzigen` on the zaak;
2. refuses a zaak that is not zaakspecifiek geautoriseerd (existing
   `ERROR_CODE_ZAAK_NOT_ZAAKSPECIFIEK_GEAUTORISEERD`);
3. under the per-zaak lock, refuses the zaakbehandelaar or the behandelaar of an open taak
   (`ERROR_CODE_MEDEWERKER_CANNOT_BE_DETACHED`) and a medewerker without a ZGM rol
   (`ERROR_CODE_MEDEWERKER_NOT_ZAAKSPECIFIEK_GEAUTORISEERD`);
4. deletes every ZGM rol of the medewerker with the reden as audit toelichting, and reindexes.

Deleting every rol covers the duplicate a concurrent addition on two pods can leave behind (see the PZ-12023
design). Keying on the medewerker rather than the rol UUID also keeps the frontend free of rol UUIDs. For the
same reason the list of medewerkers shows such a medewerker once.

The deletion, the reindex and the Solr commit all happen under the per-zaak lock, so a concurrent taak
assignment on the same pod cannot write an older set of rollen to the index after them. The commit makes the
medewerker leave the search results at once instead of at Solr's next auto-commit.

### Who may detach

The story names the zaakbehandelaar, the recordmanager and the beheerder. Detaching asserts `wijzigen`, as
adding does, so the same employees can add and detach. That right is a little broader than the story: the
`coordinator` also has it on an open zaak, and `systeemrol_behandelaar_alle_zaaktypen` on every zaak (see
`docs/solution-architecture/accessControlPolicies.md`). This is accepted: a coordinator can already hand the
zaak to another behandelaar, which changes who has access just as much, and a separate right for one action
would split adding and detaching across two policies.

## Risks / Trade-offs

- [A taak is assigned to the medewerker while they are being detached] → Assigning an existing taak grants
  the ZGM rol and assigns the taak under the per-zaak lock, and detaching reads the open taken under it, so
  on one pod one of the two wins cleanly. Some windows remain: starting a taak from a plan item grants the rol
  before it creates the taak; the lock is in memory, so two pods are not serialised; an assignment reads the
  taak before it takes the lock, so a second, concurrent assignment of the same taak plus an detachment can
  interleave; and the taak is reindexed after the lock is released, so its search entry can briefly show an
  older set of rollen. In these cases a new taakbehandelaar can lose access to the zaak while their taak stays
  assigned, or an detached medewerker can still find a taak in the search results, which they cannot open.
  Releasing the taak and assigning it to them again restores access; assigning it to the same medewerker
  without releasing it first does not, since an unchanged assignee gets no new rol. Each case needs several
  employees acting on the same zaak at the same moment. Closing them would mean holding the lock across the
  whole taak assignment and plan item flow, including sending mail, or a lock shared between pods, so they are
  accepted, like the same trade-off for adding.
- [Reindexing one of the zaak, its taken or its documenten fails after the rollen are deleted] → The request
  still succeeds and the failure is logged, as for adding a medewerker and assigning a taak, which use the same
  reindex. Opening the object is refused at once anyway, because the policy reads the rollen live; only its
  search result stays visible until the next reindex. Failing the request instead would leave the rollen
  deleted and give the user an error that a retry cannot fix.

## Migration Plan

None.

## Open Questions

None.
