## Why

`refresh-zaaktype-configuration-list` ([PZ-13638](https://dimpact.atlassian.net/browse/PZ-13638)) let the Zaakafhandelparameters page read its list again with `refetchOnWindowFocus: true`. The `focusManager` of TanStack Query 5 reacts only to `visibilitychange`. A beheerder who publishes a new zaaktype version in Open Zaak in another browser window, and then clicks back into ZAC while its tab stayed visible, still sees the previous list. The row then links to the previous version, whose procesmodel is locked.

## What Changes

- The page reads its list again when its window gets the focus, which also covers coming back from another window.
- `refetchOnWindowFocus: true` is removed from the list query.

## Capabilities

### New Capabilities
- none

### Modified Capabilities
- `zaaktype-configuration`: the list page also reads its list again when the beheerder comes back from another browser window.

## Impact

- Frontend: `ParametersComponent` and its spec. A spec fixture that dated a cached page six minutes back, to pass the former 5-minute stale time, is removed.
- No backend, REST, OpenAPI or database change; the test-data scripts do not change.
