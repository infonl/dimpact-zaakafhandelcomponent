## Context

The Zaakafhandelparameters page lists the configurations of all zaaktype versions in the default catalogus. ZAC creates the configuration of a new zaaktype version when it handles the notification about that version (`NotificationReceiver.handleZaaktype` → `ZaaktypeConfigurationBeheerService.updateZaaktypeConfiguration`). That call also clears the ZTC caches of the zaaktype and the cached list of zaaktypen.

`ScreenEventType.getEvents` maps no notification on the `zaaktypen` channel to a screen event, so the open page does not learn about the change. The list query has `staleTime: StaleTimes.Instant` and `refetchOnWindowFocus: true`. A websocket reconnection already invalidates every query.

## Goals / Non-Goals

**Goals:**
- An open Zaakafhandelparameters page shows a new zaaktype version, with the configuration ZAC created for it, without a reload and without the beheerder switching tabs or windows.
- The page reads its list only when it opens or when ZAC signals a change, and not on focus.

**Non-Goals:**
- A screen event after a beheerder saves a configuration in ZAC, so that a second beheerder sees it.
- Zaaktype versions for which Open Zaak sends no notification. In Open Zaak 1.31.2, the version in the kind stack, these are a version published with the bulk action "Publiceer geselecteerde" (`publish_selected` only calls `publish()`) or with `POST /zaaktypen/{uuid}/publish` (`ConceptPublishMixin._publish` only saves). The "Publiceren" button of a zaaktype opens its publish page, which sends an `update` notification after its transaction commits.
- Screen events for besluittypen or informatieobjecttypen.

## Decisions

### D1. A screen event type for the list of zaaktype configurations

`ScreenEventType` gets `ZAAKTYPE_CONFIGURATIES`, without an object of its own, like `ZAKEN_VERDELEN`. ZAC sends it with opcode `UPDATED` and the fixed id `zaaktype-configuraties`, through `ScreenEventType.updated(eventResourceId)`.

The page shows all zaaktype versions at once and cannot know the UUID of a new version before it exists, so the event does not carry a zaaktype UUID.

### D2. `handleZaaktype` sends the event after the configuration is committed

`handleZaaktype` sends the event after `updateZaaktypeConfiguration` returns, for each `create` or `update` notification about a zaaktype. `NotificationReceiver` runs without a transaction, and `updateZaaktypeConfiguration` runs in a transaction of its own (`REQUIRED`), so the configuration is committed and its cache cleared (`AFTER_COMPLETION`) when the event goes out. A page that reads its list on the event therefore gets the new configuration.

The event is also sent for a concept version, and for an update that changes no configuration: the call cleared the cached list of zaaktypen, so the list may have changed anyway, for example the end date of a version. The event is not sent when handling the notification fails, nor for a `destroy` notification, which `handleZaaktype` ignores. A failure can come after the call cleared the cached list of zaaktypen, but a page that read its list then could show the new version without its configuration, so ZAC does not signal it; Open Notificaties does not retry, because the receiver answers with `204` in either case.

Mapping the `zaaktypen` channel in `ScreenEventType.getEvents` is not chosen: that event would also go out when `handleZaaktype` fails, and it would carry the UUID of the zaaktype version as its id.

This follows [ADR 0008](../../../docs/solution-architecture/decisions/0008-update-the-ui-without-waiting-for-notifications-on-zac-initiated-changes.md): a change made by another system, here Open Zaak, reaches the UI through its notification.

### D3. The page invalidates its list on the event

`ParametersComponent` adds a websocket listener for `UPDATED` / `ZAAKTYPE_CONFIGURATIES` / `zaaktype-configuraties` when it is created and removes it when it is destroyed. On the event it invalidates the list query, so that the shown page is read again with its current filter, sort and page.

`staleTime: StaleTimes.Instant` stays: the listener exists only while the page is open, so the page must read its list when a beheerder opens it again after an event went by. `refetchOnWindowFocus: true` is removed, so the query falls back to the default `false` of `ZacQueryClient.GET`. Reading on focus adds nothing once the event exists: for a publication without a notification, ZAC does not clear its cached list of zaaktypen either, so a read on focus would return the same list.

### D4. What the tests prove

`NotificationReceiverTest` proves that the event is sent after `updateZaaktypeConfiguration` returns, and not for a `destroy` notification or a failure. That the configuration is committed at that moment follows from D2, not from a test. The integration test proves that a zaaktype notification reaches a subscribed websocket as `UPDATED` / `ZAAKTYPE_CONFIGURATIES` / `zaaktype-configuraties`. It cannot prove the order of commit and event, because ZAC answers the notification only after it has committed the configuration and handed the event to `EventingService`, which delivers it asynchronously.

## Risks / Trade-offs

- [An event is missed while the websocket is disconnected] → The websocket service invalidates every query when it reconnects, and the page reads its list each time it opens.
- [Open Zaak sends no notification for some ways of publishing] → No event either; the page shows the version only when the ZTC list cache is cleared. Out of scope, see Non-Goals.
- [Many zaaktype notifications in a short time, for example a catalogus import, make every open page read its list for each of them] → The backend builds the page from its caches; `slim-zaaktype-list-payloads` measured 10 ms for 211 zaaktypes. `invalidateQueries` cancels a running read of the query before it reads it again (`cancelRefetch: true` by default).
