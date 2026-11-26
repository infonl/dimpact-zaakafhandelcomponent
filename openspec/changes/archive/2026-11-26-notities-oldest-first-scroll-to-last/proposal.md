## Why

After an employee adds a note, the notes panel moves up as a whole: the heading *Notities (n)* slides partly out of view and, on a long list, the field *Voeg een notitie toe* disappears. The panel scrolls the new note into view with `scrollIntoView`, which also scrolls the drawer containers around it. That call dates from the floating notes card and stayed when the notes moved into the side panel.

The order of the notes is also not what the specification says. The code sorts the newest change first, but the database stores `tijdstip_laatste_wijziging` as a date without a time. Notes of the same day therefore keep the order of the backend, oldest first. Employees see the newest note at the bottom, right above the field, and that is the behaviour on TEST. A note just added briefly shows at the top and moves to the bottom when the list reloads.

## What Changes

- The notes show in the order they were added: the oldest at the top, the newest at the bottom, right above the field *Voeg een notitie toe*.
- Only the list of notes scrolls. The heading *Notities (n)* and the button *Sluiten* stay in place, and the panel itself never moves.
- After adding a note, the list scrolls to the bottom, so the new note and the field are in view.

## Capabilities

### New Capabilities

### Modified Capabilities

- `zaak-notities`: the order of the notes, where a new note shows, and what scrolls in the notes panel.

## Impact

- Frontend only: `src/main/app/src/app/notities/`.
- No REST API change, so no change to `scripts/test-data/`.
- No backend change. Storing the time of a note (a migration of `notitie.tijdstip_laatste_wijziging` to `timestamp with time zone`) is left for a later change.
