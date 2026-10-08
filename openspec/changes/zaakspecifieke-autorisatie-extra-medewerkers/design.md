## Context

`ZaakspecifiekeAutorisatieService.grantZaakspecifiekeAutorisatie` already adds a *Zaakspecifiek geautoriseerde
medewerker* (ZGM) rol and skips an existing holder. `grantZaakspecifiekeAutorisatieToTaakbehandelaar` wraps it
with the per-zaak lock, skips the zaakbehandelaar and reindexes the zaak, its taken and its documenten.

Once the rol exists, everything else already works:
- `PolicyService` reads the rollen live, so OPA grants access at once;
- the reindex puts the medewerker in `zaakGeautoriseerdeMedewerkers`, so werkvoorraden and zoekresultaten follow;
- the betrokkenen tab filters out roltypen with `omschrijvingGeneriek = behandelaar`;
- the zaakhistorie shows a rol from the Open Zaak audittrail, with the roltype omschrijving as label.

The groep picker can reuse `GET /rest/identity/zaaktype/{zaaktype}/behandelaar-groups`. PABC can also list the
groepen holding `zaakspecifiek_geautoriseerd` for a zaaktype through
`PabcClientService.getGroupsByApplicationRoleAndZaaktype`; a new
`IdentityService.listUserIdsForApplicationRoleAndZaaktype` turns those into the ids of their members.

## Goals / Non-Goals

**Goals:** every requirement in `specs/zaakspecifieke-autorisatie-extra-medewerkers/spec.md`, reusing the
existing grant, lock and reindex.

**Non-Goals:** the medewerkers tab (PZ-12079), removing access (PZ-12046), a PABC endpoint listing medewerkers
per role.

## Decisions

### The koppeling lives in the side menu, not in the zaak edit form

The ticket asks for a "koppelen optie" and links a Figma design for "koppelen van een medewerker". The side
menu's *Koppelingen* section is where every other koppeling lives, so "Medewerker toevoegen" goes there and
opens a side panel, like *Betrokkene koppelen*.

### The backend works out the candidates

`GET /rest/zaken/zaak/{uuid}/zaakspecifiek-geautoriseerde-medewerkers/kandidaten?groepId=…` returns the users
of the groep minus everyone who already has access:
- the zaakbehandelaar and ZGM rol holders (`ZaakToewijzing.geautoriseerdeMedewerkerIds`);
- members of the groepen PABC returns for `zaakspecifiek_geautoriseerd` and the zaaktype.

The frontend then needs no knowledge of IAM. The endpoint refuses a groep that is not a `behandelaar` groep
for the zaaktype.

### The endpoints live in their own REST service

`ZaakRestService` is already large. A new `ZaakspecifiekeAutorisatieRestService` under the same `zaken` path holds
both endpoints, like the other `zaken` REST services split off before it.

### Adding repeats the checks

`POST /rest/zaken/zaak/{uuid}/zaakspecifiek-geautoriseerde-medewerkers` with `{ groepId, medewerkerId }`:
1. asserts `wijzigen` on the zaak;
2. refuses a zaak that is not zaakspecifiek geautoriseerd (`ERROR_CODE_ZAAK_NOT_ZAAKSPECIFIEK_GEAUTORISEERD`);
3. refuses a groep that is not a `behandelaar` groep for the zaaktype
   (`ERROR_CODE_GROUP_NOT_BEHANDELAAR_FOR_ZAAKTYPE`), or a medewerker outside it (existing
   `ERROR_CODE_USER_NOT_IN_GROUP`);
4. refuses a medewerker who already has access (`ERROR_CODE_MEDEWERKER_ALREADY_ZAAKSPECIFIEK_GEAUTORISEERD`);
5. grants under the per-zaak lock and reindexes.

Refusals use `InputValidationFailedException`, as the other zaakspecifieke autorisatie errors do. A missing roltype gives the existing
`ERROR_CODE_ZAAKSPECIFIEK_GEAUTORISEERDE_MEDEWERKER_ROLTYPE_NOT_FOUND`.

### Same rol, same toelichting

The rol keeps the shared `roltoelichting`. The audit toelichting is the text taakbehandelaars already get,
"Zaakspecifiek geautoriseerd medewerker van zaak {zaaknummer}", so the zaakhistorie shows every ZGM rol the
same way.

## Risks / Trade-offs

- [The kandidaten call does one PABC call plus one Keycloak call per `zaakspecifiek_geautoriseerd` groep]
  → Few such groepen are expected; accepted. A groep PABC returns but Keycloak does not know is skipped, and
  group members are read page by page, so large groepen are complete.
- [The candidate list can be stale when another employee adds the same medewerker meanwhile] → The POST
  refuses the duplicate.
- [The per-zaak lock is in memory, so two pods can each add the same medewerker at the same moment] → Same
  trade-off as the zaak and taak assignment flows, which use this lock. A second identical rol grants no extra
  access. Removing access (PZ-12046) must remove every rol of the medewerker.

## Migration Plan

None. Rollback leaves added rollen in Open Zaak, where they keep granting access. That is intended: the
access was granted on purpose, and it is the same rol previous zaak- and taakbehandelaars keep. Removing it is
PZ-12046.

## Open Questions

None.
