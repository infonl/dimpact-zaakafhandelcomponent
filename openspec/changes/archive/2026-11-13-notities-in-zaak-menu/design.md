## Context

The notes of a zaak live in `NotitiesComponent` (`src/main/app/src/app/notities/`). The zaak view renders it at the
end of its content when `notitieRechten.canLezen || canWijzigen`. The component draws a fixed mini FAB at the bottom
right with a `matBadge` holding the count, and a fixed `mat-card` that it toggles with a `showNotes` signal.

It already uses `injectQuery` for the notes list and the logged-in user, and `injectMutation` for create, update and
delete; the mutations patch the query cache. `NotitieService.listNotities` sets `staleTime: StaleTimes.Instant`. The
websocket listener for `ZAAK_NOTITIES` sits in the component, so it lives as long as the component.
The text fields are read through template reference variables and a `viewChild` on `nativeElement`, and the component
clears the field by writing `nativeElement.value`.

The zaak menu is built by `buildZaakMenu` in `zaken/zaak-view/utils/zaak-view-menu.builder.ts` from a `computed` in
`ZaakViewComponent`. A `ButtonMenuItem`'s `title` is a translation key and also the id of the side action:
`ZaakSideActionService.activeAction` holds it, and the zaak view template switches the right drawer
(`actionsSidenav`) on it. `side-nav.component.html` renders `title | translate` as label and tooltip.

## Design

Figma: [Dimpact - Confidential, node 6010:118763](https://www.figma.com/design/hwqjMzbwU1gqPIWeOqg8hb/Dimpact---Confidential?node-id=6010-118763)

One frame: the zaak view with the notes panel open.

- **Menu on the left.** The first section lists *E-mail versturen*, *Document maken*, *Document toevoegen*,
  *Document verzenden*, **Notities (2)**, *Zaakdata*. The *Notities* item uses the icon `sticky_note_2` (the same
  icon as the old button). In the frame it is in the active colour (primary blue), because its panel is open. The
  count is plain text in brackets, in the same colour as the label: no badge. (The frame names the section *Actie*;
  in the code today it is *Zaak*, the label the Jira item asks for. The section keeps its current name.)
- **Side panel on the right**, over the content with the grey backdrop, like the other zaak actions.
  - Heading: the icon `sticky_note_2` and *Notities (2)* in primary blue, a close icon (X) at the right. A divider
    below.
  - The notes as cards, one under the other. Each card: the name of the employee in bold at the top left (*Test
    User1*), the date at the top right (*01-01-2026*), the text below (*Dit is een notitie bij een zaak.*), and at the
    bottom right an edit (pencil) and a delete (bin) icon button. The second card (*Beheerder1*) has no icons: a note
    the employee may not edit.
  - Below the list: a filled text field with the placeholder *Voeg een notitie toe* and a round primary send button
    (paper plane) inside it, at the right.
  - A divider, then a button *Sluiten* at the bottom left.
- **Removed:** the round notes button with the red count badge at the bottom right of the zaak view, and the floating
  notes card it opened.

States that the frame does not show, taken from the screen today:

- **Empty:** a zaak without notes shows *Notities* without a number in the menu and the heading, and the panel shows
  only the text field. No empty text.
- **Loading:** the list stays empty until the notes are loaded; the menu shows *Notities* without a number until then.
- **Error:** a failing call shows the standard ZAC error snackbar, as today.
- **Edit mode:** the note's text becomes a field *Wijzig uw notitie* with a character counter and the buttons save
  (check) and cancel (X), as today. The field *Voeg een notitie toe* is disabled meanwhile.
- **Read-only:** no text field and no edit or delete buttons.
- **Collapsed menu:** icon only; the tooltip shows *Notities (2)*.

## Goals / Non-Goals

**Goals:**

- The notes action in the zaak menu, the panel in the right drawer, the count as text.
- The count stays current while the panel is closed.
- In the code this touches: no DOM reads for form state, signals and signal inputs only, TanStack Query for all
  server state, cache updates instead of `StaleTimes.Instant`.

**Non-Goals:**

- No change to the REST API, the rights or the backend.
- No confirmation dialog for deleting a note (there is none today).
- No notes on the taak view or anywhere else outside the zaak view.
- No wider refactoring of `ZaakViewComponent` (its field-based websocket listeners and the non-signal `zaak` field
  stay as they are). The extra guidance asks for a full move to TanStack Query and signals; in the notes code that is
  done here, the rest of the zaak view is a separate item.

## Decisions

### The panel is a side action of the zaak view

The menu item is a `ButtonMenuItem` with title `notities` that calls `handlers.openSideAction()`. The zaak view
template gets `@if (activeAction === "notities")` with `<zac-notities [sideNav]="actionsSidenav" ...>` in the right
drawer, like `zac-zaakdata`. The component drops the FAB, the `mat-card`, `showNotes`, `toggleNotitieContainer`,
`MatBadgeModule` and the fixed positioning in its `.less`; it gets the `mat-toolbar` heading with a close button and
a *Sluiten* button that call `sideNav().close()`, as the other panels do.

Alternative: keep the floating card and only move the trigger to the menu. Rejected: the design shows the right
drawer, and the drawer brings the backdrop and inert content the other actions have.

### The count is a property of the menu item, not part of the title

`ButtonMenuItem` gets an optional `count?: number` (constructor option). `side-nav.component.html` renders
`{{ menuItem.title | translate }}{{ count ? " (" + count + ")" : "" }}` for the label and the tooltip, through one
small helper so both stay the same. The title stays the plain key `notities`, so `activeAction` and the
`@if (activeAction === "notities")` switch do not depend on the number.

Alternative: a translation key `Notities ({{aantal}})` with params on the menu item. Rejected: the title is also the
action id, and the side nav would need translate params for one item.

### The zaak view owns the notes query and the websocket listener

`ZaakViewComponent` gets `notitiesQuery = injectQuery(() => ({ ...notitieService.listNotities(uuid), enabled: canLezen
|| canWijzigen }))` and passes `notitiesQuery.data()?.length` to `buildZaakMenu` (a new `aantalNotities: number | null`
parameter; `null` when the employee may not see notes, which also leaves the item out). The `ZAAK_NOTITIES` websocket
listener moves from `NotitiesComponent` to the zaak view, in an `effect` with `onCleanup` keyed on the zaak uuid, so
the count updates while the panel is closed. `NotitiesComponent` keeps its own `injectQuery` on the same query key:
TanStack deduplicates it, and the panel reads from the same cache.

`NotitieService.listNotities` drops `staleTime: StaleTimes.Instant`: the mutations write the cache and the websocket
listener invalidates it, so the default stale time holds (the cache strategy the frontend is moving to).

Alternative: the panel reports its count up through an output. Rejected: the panel is only rendered while open, and
the count must show before it is opened.

### Form state in signals

`NotitiesComponent` keeps the new text in `nieuweNotitieTekst = signal("")` and the text under edit in
`bewerkteNotitieTekst = signal("")`, bound on the `matInput` textarea with `[value]` and `(input)`. The shared
`zac-textarea` is not used: it has no slot for the send button inside the field, and these fields are not part of a
form group. The character counters and the disabled state of the
send button read those signals. On success the mutation sets the signal to `""` instead of writing `nativeElement`.
The `viewChild` on `notitieTekst` goes; the `scrollTarget` `viewChild` stays, it scrolls the new note into view.

### Rights

`notitieRechtenQuery` stays in the zaak view and is passed to the panel as today. The rule "show when `canLezen` or
`canWijzigen`" moves from the template `@if` around `<zac-notities>` into the menu builder input.

## Risks / Trade-offs

- [The menu `computed` rebuilds all items when the count changes] → The items are cheap objects and the side nav
  tracks them by `id ?? type + title`, so the DOM is kept. Accepted.
- [Specs and the e2e test find notes by the button name "Notities"] → The zaak view specs that look for the button
  move to the menu item, which also has the accessible name *Notities* (plus the count). No e2e test covers notes
  today (checked `src/e2e`).
- [An employee with only `canWijzigen` gets the list query] → The rule is the same as today (the component fetched the
  list in that case too); the backend decides what it returns.

## Migration Plan

Frontend only, no data change. Rollback is a revert of the pull request.

## Assumptions

- The item goes in the existing *Zaak* section (the Jira item suggests it; the frame calls the section *Actie*, but
  renaming the section is not part of this item), after *Document verzenden* and before *Zaakdata*, as in the frame.
- The action shows for every zaak the employee may read or change notes on, also a closed zaak and a zaak driven by
  a process, just as the button showed before. The `canBehandelen` and document rules that govern the items around
  it do not apply.
- With 0 notes, and while loading, the label is *Notities* without "(0)".
- The panel heading shows the count too, as in the frame.
- The label key stays `notities` (*Notities* / *Notes*) and the button uses the existing `actie.sluiten`; no new
  translation key is needed.
- Delete stays without a confirmation dialog, as today.
- The workgroup decided to show the count, behind the label (Jira comments by Karin Masselink); the frame matches.
