## Why

[PZ-10893](https://dimpact.atlassian.net/browse/PZ-10893): the notes of a zaak open from a round button at the bottom
right of the zaak view. Behandelaars look at the menu on the left and often miss that button. The button also shows
the number of notes in a red badge, and behandelaars read that badge as "there is something new".

## What Changes

- The zaak menu on the left gets the action *Notities*, in the *Zaak* section, after *Document verzenden* and before
  *Zaakdata*. It shows only to an employee who may read or change notes.
- The label shows the number of notes behind the name, as plain text: *Notities (2)*. No red badge.
- The action opens the notes in the side panel on the right, like the other zaak actions: a heading *Notities (2)*
  with a close button, the notes newest first, the field *Voeg een notitie toe* with a send button, and a button
  *Sluiten*.
- The round notes button at the bottom right of the zaak view and its floating card are removed.
- The number in the menu stays current while the panel is closed: a change to the notes by another employee updates
  it.
- Refactoring in the same code: the notes panel keeps the text it edits in signals instead of reading the DOM, the
  notes stay in the TanStack Query cache instead of being fetched again on each use, and the menu item can show a
  count without a separate translation per number.

## Capabilities

### New Capabilities

- `zaak-notities`: where an employee finds the notes of a zaak, how the number of notes is shown, and who sees them.

### Modified Capabilities

## Impact

- Frontend only: `src/main/app/src/app/notities/`, the zaak view (`zaken/zaak-view/`, its menu builder and specs),
  the side nav menu items (`shared/side-nav/`), and `assets/i18n` if a key is added.
- No REST API change, so no change to `scripts/test-data/`.
- No backend change.
