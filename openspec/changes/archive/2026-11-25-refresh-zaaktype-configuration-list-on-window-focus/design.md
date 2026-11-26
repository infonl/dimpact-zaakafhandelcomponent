## Context

Since `refresh-zaaktype-configuration-list` the list query of the Zaakafhandelparameters page is stale at once and sets `refetchOnWindowFocus: true`. The `focusManager` of TanStack Query 5 listens only to `visibilitychange`, which does not fire while the ZAC tab stays visible, for example when Open Zaak is open in a window next to it. A websocket reconnection invalidates every query, the list included, but only after the connection was lost, so it cannot be relied on.

## Goals / Non-Goals

**Goals:**
- A beheerder who comes back to the page from another window or tab sees each zaaktype version that exists at that moment.

**Non-Goals:**
- Pushing changes to a list in the window that has the focus. That needs a websocket event for zaaktype configurations, which does not exist.
- Reading other lists again on focus.

## Decisions

### D1. The page refetches its list when its window gets the focus

The page listens to the `focus` event of the window (`host: { "(window:focus)": ... }`) and then refetches the list query. Activating the tab also gives its window the focus, so this covers coming back from another tab too, and `refetchOnWindowFocus: true` is removed.

Changing the event listener of the `focusManager` for the whole app is not chosen: it would affect every query that keeps the default `refetchOnWindowFocus`, and TanStack Query dropped the `focus` event in version 5 because it also fires after, for example, a file picker or an `alert`.

## Risks / Trade-offs

- [One request each time the window gets the focus] → The backend builds the page from the ZTC cache and the cache of zaaktype configurations; `paged-zaaktype-configuration-list` measured 10 ms for 211 zaaktypes.
- [The global loading bar shows briefly each time the window gets the focus] → Accepted; the rows stay visible while the list is read again.
- [Switching windows quickly cancels and restarts the request] → The last request wins, and a cancelled request is not reported as an error.
