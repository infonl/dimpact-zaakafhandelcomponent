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

**Goals:** every requirement in `specs/zaaktype-inrichtingscheck/spec.md`.

**Non-Goals:** BPMN-specific inrichtingschecks; changing the checks for CMMN zaaktypes; removing the admin
endpoint in `UtilRestService` that clears all caches; changing which caches a zaaktype notification clears.

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

### Leave the cache clearing on a zaaktype notification as it is

A zaaktype notification keeps clearing the zaaktype, roltype, resultaattype, statustype and eigenschap caches,
and not the informatieobjecttype, zaaktype-informatieobjecttype and besluittype caches the button also
cleared. Those three are cached per zaaktype URL, and a published change in Open Zaak creates a new zaaktype
version with a new URL, which is not cached yet. Clearing them as well would only help in rare cases, so the
admin endpoint covers those instead.

## Risks / Trade-offs

- [Cached informatieobjecttypen or besluittypen of a zaaktype change without a new zaaktype version, or a
  notification is missed] → The cached data stays until nobody reads it for an hour. A beheerder clears it with
  the admin endpoint, which the handleiding describes.

## Migration Plan

Delivered as a stack of three pull requests: this OpenSpec change; hiding BPMN zaaktypes; removing the
synchronise button. No data migration. A client of the removed `ztc-cache` endpoints must stop calling them.
