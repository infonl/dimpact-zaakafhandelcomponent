## Why

Zoekresultaten, werklijsten and the Koppelen search list show zaken (and their taken and documenten) of a
zaaktype as soon as the user holds *any* application role for that zaaktype, while opening or linking such a
zaak requires a read role (`raadpleger`, `behandelaar`, `coordinator`, `recordmanager` or `beheerder`). A user
whose only role on a zaaktype is, for example, `brp_zoeken` or `zaakspecifiek_geautoriseerd` (a PABC
misconfiguration) therefore sees the identificatie and omschrijving of zaken they may not open, and gets an
error dialog when they try. ZAC must only list what the user may read, even when PABC is misconfigured.

## What Changes

- The search index query that backs zoekresultaten, werklijsten and the Koppelen search list only admits
  zaaktypen for which the user holds a read role, either for that zaaktype or as an overall role. Zaaktypen
  for which the user only holds non-read roles are no longer admitted.
- Users who hold a read role for a zaaktype see exactly the same results as today; the existing
  zaakspecifiek geautoriseerd filtering stays unchanged.
- **BREAKING** (REST API): the `NOT_AUTHORISED_TO_LEZEN` value is removed from the `ZaakNotLinkableReason`
  enum returned by the Koppelen search, together with the frontend message
  `zaak.koppelen.niet-koppelbaar.NOT_AUTHORISED_TO_LEZEN` (nl and en). Such zaken can no longer appear in the
  Koppelen list, so the reason is unreachable. Only the ZAC frontend consumes this enum.
- The server-side read check when actually relating two zaken stays in place, so a request that bypasses the
  search list is still refused.

## Capabilities

### New Capabilities
- `zoekresultaten-leesrecht`: zoekresultaten, werklijsten and the Koppelen search list only contain zaken,
  taken and documenten of zaaktypen the user holds a read role for, and linking still verifies read access.

### Modified Capabilities
<!-- None: the zaakspecifiek geautoriseerde filtering (zaakspecifiek-geautoriseerde-zoekindex) and the OPA
     permission matrix (application-role-permission-matrix) keep their requirements. -->

## Impact

- Backend: `SearchService` (allowed-zaaktypen filter query), `ZaakNotLinkableReason`, `ZaakLinkData`
  (gerelateerd reason and `canBeRelatedTo`), and their unit tests; integration tests for search/koppelen
  where roles per zaaktype are exercised.
- REST API / OpenAPI: `ZaakNotLinkableReason` enum loses one value; regenerated frontend types.
- Frontend: `zaak-link.component.spec.ts` reason list, `nl.json` / `en.json` message keys.
- Local/itest seed data: a Keycloak functional role, group and test user plus a PABC mapping that grants only
  `brp_zoeken` in domein test 2, with the matching email variables in `.env.example`, `.env.tpl` and
  `docker-compose.yaml`.
- No data migration and no Solr reindex: the filter is applied at query time.
