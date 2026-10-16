## Context

`GET /rest/health-check/zaaktypes` (`HealthCheckRestService.listZaaktypeInrichtingschecks`) lists the
non-concept, currently valid zaaktypes of the catalogus and builds an inrichtingscheck for each, reading its
statustypen, resultaattypen, roltypen and more from Open Zaak. Most checks only make sense for CMMN.

ZAC caches catalogue data from Open Zaak in `ZtcClientService`; entries expire one hour after last access. The
*Zaaktype catalogus synchronisatie* card calls `DELETE /rest/health-check/ztc-cache` to clear all those caches
and `GET /rest/health-check/ztc-cache` to show when that was last done. A zaaktype notification from Open Zaak
already calls `ZaaktypeConfigurationBeheerService.updateZaaktypeConfiguration`, which clears the zaaktype,
roltype, resultaattype, statustype and eigenschap caches.

## Goals / Non-Goals

**Goals:** every requirement in `specs/zaaktype-inrichtingscheck/spec.md` and
`specs/zaaktypecatalogus-cache/spec.md`.

**Non-Goals:** BPMN-specific inrichtingschecks; changing the checks for CMMN zaaktypes; removing the admin
endpoint in `UtilRestService` that clears all caches.

## Decisions

### Filter BPMN zaaktypes with one query, before any check

`listZaaktypeInrichtingschecks` asks `ZaaktypeConfigurationService.listConfigurationsBoundTo(ProcessEngine.BPMN)`
once for the zaaktypes bound to BPMN and skips those before it builds a check. So a BPMN zaaktype costs no
Open Zaak reads and does not appear in the response. Checking each zaaktype and then dropping the BPMN ones
would still do every read; a query per zaaktype would add a database call per zaaktype.

A zaaktype without a configuration, or bound to CMMN, stays in: the check is how a beheerder finds a zaaktype
that still needs its zaakafhandelparameters. The requirement *One answer for the configuration of a zaaktype*
in `zaaktype-configuration` still holds: the health check reads the same configuration as the rest of ZAC.

### Remove the button and its endpoints

The button only cleared the ZTC caches. A beheerder had to know when to press it, and the last sync time on the
card suggested a scheduled synchronisation that does not exist. Both `ztc-cache` endpoints go with the card;
nothing else, not the itest or e2e tests, calls them. The admin endpoint in `UtilRestService` stays as a manual
fallback.

### A zaaktype notification clears every catalogue cache

`updateZaaktypeConfiguration` also clears the informatieobjecttype, zaaktype-informatieobjecttype and
besluittype caches, so a zaaktype notification clears every cache the button cleared. Expiry alone is not
enough: `expireAfterAccess` keeps an entry that is read often alive, so stale data could stay indefinitely.
Clearing whole caches, rather than only the entries of the changed zaaktype, matches what the method already
does for the other caches; zaaktypes change rarely.

## Risks / Trade-offs

- [Open Zaak changes an informatieobjecttype or besluittype without a zaaktype notification, or a notification
  is missed] → The cached data stays until it expires or the next zaaktype notification. A beheerder can still
  clear all caches with the admin endpoint.
- [Every zaaktype notification clears all catalogue caches] → A few extra Open Zaak reads after a zaaktype
  change; acceptable given how rarely zaaktypes change.

## Migration Plan

Delivered as a stack of three pull requests: this OpenSpec change; hiding BPMN zaaktypes; removing the
synchronise button. No data migration. A client of the removed `ztc-cache` endpoints must stop calling them.
