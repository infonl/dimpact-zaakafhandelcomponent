## Why

`refresh-zaaktype-configuration-list` ([PZ-13638](https://dimpact.atlassian.net/browse/PZ-13638)) let the Zaakafhandelparameters page read its list again with `refetchOnWindowFocus: true`. A beheerder who publishes a new zaaktype version in Open Zaak and comes back to ZAC can still see the previous list: the `focusManager` of TanStack Query 5 reacts only to `visibilitychange`, so not to a beheerder who clicks back into a window that stayed visible. The row then links to the previous version, whose procesmodel cannot be changed once that version has zaken. The description of PZ-13638 leaves refreshing the open list through a websocket event to a separate ticket; this change is that work ([PZ-13642](https://dimpact.atlassian.net/browse/PZ-13642)).

Reading the list again on every focus change would fetch without knowing whether anything changed. ZAC knows when the list changes: it handles the notification about the zaaktype version and creates its configuration. ZAC should tell the open page, so that the page reads its list only then.

## What Changes

- ZAC sends a new screen event, `ZAAKTYPE_CONFIGURATIES`, each time it has handled a `create` or `update` notification about a zaaktype.
- The Zaakafhandelparameters page listens to this screen event while it is open and then reads its list again.
- `refetchOnWindowFocus: true` is removed from the list query; the page no longer reads its list because its tab gets the focus.
- The page keeps reading its list each time a beheerder opens it.

## Capabilities

### New Capabilities
- none

### Modified Capabilities
- `zaaktype-configuration`: ZAC signals a handled zaaktype notification to the open list page, and the list page reads its list again on that signal instead of on tab focus.

## Impact

- Backend: `ScreenEventType`, `NotificationReceiver`, `NotificationReceiverTest` and the integration test `NotificationZaaktypeCompletionParametersTest`. The generated OpenAPI spec gets the new `ScreenEventType` value; no REST path or payload changes, so the test-data scripts do not change.
- Frontend: the generated types, `ParametersComponent` and its spec.
- Not in scope: a screen event after a beheerder saves a configuration in ZAC, and the zaaktype versions for which Open Zaak sends no notification, such as a version published with the bulk action "Publiceer geselecteerde" or with `POST /zaaktypen/{uuid}/publish`. The publish page of a zaaktype in Open Zaak does send an `update` notification.
