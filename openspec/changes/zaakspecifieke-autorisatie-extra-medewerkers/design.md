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
`PabcClientService.getGroupsByApplicationRoleAndZaaktype`.

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

### Adding repeats the checks

`POST /rest/zaken/zaak/{uuid}/zaakspecifiek-geautoriseerde-medewerkers` with `{ groepId, medewerkerId }`:
1. asserts `wijzigen` on the zaak;
2. refuses a zaak that is not zaakspecifiek geautoriseerd;
3. refuses a groep that is not a `behandelaar` groep for the zaaktype, or a medewerker outside it;
4. refuses a medewerker who already has access (`ERROR_CODE_MEDEWERKER_ALREADY_ZAAKSPECIFIEK_GEAUTORISEERD`);
5. grants under the per-zaak lock and reindexes.

Refusals reuse `InputValidationFailedException` with a new error code, as the other zaakspecifieke autorisatie
errors do. A missing roltype gives the existing
`ERROR_CODE_ZAAKSPECIFIEK_GEAUTORISEERDE_MEDEWERKER_ROLTYPE_NOT_FOUND`.

### Same rol, same toelichting

The rol keeps the shared `roltoelichting`. The audit toelichting is the text taakbehandelaars already get,
"Zaakspecifiek geautoriseerd medewerker van zaak {zaaknummer}", so the zaakhistorie shows every ZGM rol the
same way.

## Risks / Trade-offs

- [The kandidaten call does one PABC call plus one Keycloak call per `zaakspecifiek_geautoriseerd` groep]
  → Few such groepen are expected; accepted.
- [The candidate list can be stale when another employee adds the same medewerker meanwhile] → The POST
  refuses the duplicate.

## Migration Plan

None. Rollback leaves added rollen in Open Zaak, where they keep granting access.

## Open Questions

None.
