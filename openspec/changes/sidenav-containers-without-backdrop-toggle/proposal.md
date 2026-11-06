## Why

[PZ-13357](https://dimpact.atlassian.net/browse/PZ-13357): since Angular Material 22, an open drawer in mode `over`
makes the page content inert. The menu and the actions panel share one drawer container, and in auto mode the menu
is always open in mode `over`. So we switch the container's backdrop on and off ourselves, and after the Angular 22
upgrade ([PZ-13313](https://dimpact.atlassian.net/browse/PZ-13313)) that needed a workaround to keep the page
clickable after closing the actions panel. Material does not check inert again when the backdrop changes, only when
the mode changes.

## What Changes

- The menu gets its own outer drawer container, without a backdrop.
- On the zaak, taak and document view, the actions panel gets its own inner drawer container inside the content of
  the menu container, with the default Material behaviour: a backdrop and inert content while it is open.
- The code that switches the backdrop on and off, and the workaround, are removed.
- The global styles for the page padding, the auto mode margin and the height on small screens apply to one of the
  two containers only.

## Capabilities

### New Capabilities

None.

### Modified Capabilities

None. Users see no change: the menu works in the modes open, closed and auto, and the page is clickable after the
actions panel closes through the close button, Escape or a click on the backdrop.

## Impact

- **Frontend**: the zaak, taak and document view, the 8 admin pages, their shared view base classes and the global
  styles.
- **Backend, database, Solr, OPA, Helm charts**: no change.
