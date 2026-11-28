## Context

`NotitiesComponent` renders in the right drawer (`actionsSidenav`) of the zaak view. It sorted the notes with `nieuwsteWijzigingEerst` on `tijdstipLaatsteWijziging`, and after a create it called `scrollIntoView` on an empty `#scrollTarget` div above the list.

The column `notitie.tijdstip_laatste_wijziging` is a `date` (`V1__notitie.sql`), although `Note.dateTimeLastModified` is a `ZonedDateTime`. The REST API returns `2026-11-26T00:00:00Z` for every note of that day, except in the response to the create, which carries the full time.

After a create, the `ZAAK_NOTITIES` websocket event makes the zaak view reload the notes list about 100 ms later.

## Decisions

- **Sort on `id`, ascending.** The id grows with every new note and does not change on an edit, so the order is stable, also when the list reloads. Sorting on the date cannot order the notes of one day. A note keeps its place when it is changed.
- **The list is the only scroll container.** The component host is a flex column of the full drawer height; `.notitie-body` takes the remaining height with `overflow-y: auto`. The toolbar, the dividers and `.notitie-acties` do not shrink.
- **Scroll after the new list is rendered.** `onSuccess` of the create sets a flag; an `afterRenderEffect` on the sorted notes scrolls `.notitie-body` to its `scrollHeight` and clears the flag. Scrolling directly in `onSuccess`, or in `afterNextRender`, runs before TanStack Query hands the new list to the component, so the new note is drawn after the scroll and pushes the field out of view.

## Alternatives considered

- **Newest first with the field above the list.** Consistent too, but it changes what employees see on TEST today.
- **Store the time of a note.** A Flyway migration to `timestamp with time zone` makes the time-based sort work for new notes. Existing notes stay at midnight, so the notes of one day still need a tiebreaker on `id`. Left for a later change.
- **Do not put the new note in the cache and wait for the reload.** Avoids the reorder, but the note shows only after a GET. Not needed once the sort is stable.

## Risks / Trade-offs

- A note that is changed keeps its place instead of moving to the end. The date of the last change still shows on the note.
