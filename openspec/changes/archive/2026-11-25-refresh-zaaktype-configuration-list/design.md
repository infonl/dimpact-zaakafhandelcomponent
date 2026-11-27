## Context

The list query of the Zaakafhandelparameters page used the defaults of `ZacQueryClient.GET`: fresh for 5 minutes and not read again on window focus. Decision D6 of `paged-zaaktype-configuration-list` kept those defaults. ZAC creates the configuration of a new zaaktype version itself when Open Notificaties reports the publication, so the list changes without any action in ZAC. The edit page already reads its configuration on every visit (`staleTime: 0` in `ZaakafhandelParametersResolver`).

## Goals / Non-Goals

**Goals:**
- A beheerder who opens the list, or comes back to it from another tab, sees each zaaktype version that exists at that moment.

**Non-Goals:**
- Pushing changes to a list that stays open in the active tab. That needs a websocket event for zaaktype configurations, which does not exist.
- Changing the backend caches; they are already cleared when a configuration is stored or a zaaktype changes.

## Decisions

### D1. The list is stale at once

The list query sets `staleTime: StaleTimes.Instant`. Opening the page reads the list again. It keeps `keepPreviousData` and its `gcTime`, so a beheerder who returns sees the previous rows at once, and these are replaced when the response arrives. `LOAD_PER_VISIT` would drop the rows and show the loading message instead, so it is not chosen.

### D2. The list is read again on window focus

The list query sets `refetchOnWindowFocus: true`. A beheerder usually publishes the new version in an Open Zaak tab and then returns to the ZAC tab, where the list is still open; without this, that list stays as it was.

## Risks / Trade-offs

- [One more request per visit and per focus] → The backend builds the page from the ZTC cache and the cache of zaaktype configurations; `paged-zaaktype-configuration-list` measured 10 ms for 211 zaaktypes.
- [A beheerder who returns within seconds of the publication can still see the previous list, because ZAC has not yet processed the notification] → The edit page shows the geldigheid of the zaaktype version at the top, and the next visit or focus shows the new version.
