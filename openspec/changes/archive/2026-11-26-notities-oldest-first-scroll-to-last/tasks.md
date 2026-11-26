## 1. Frontend: notes panel

- [x] 1.1 Sort the notes on `id`, ascending (`oldestFirst`), instead of on `tijdstipLaatsteWijziging`. Verify with a test in `notities.component.spec.ts`: notes with dates in the opposite order of their ids, two of them on the same day, show in id order.
- [x] 1.2 Make `.notitie-body` the only scroll container: the host is a flex column of the full drawer height, the toolbar, dividers and `.notitie-acties` do not shrink. Remove the `#scrollTarget` div.
- [x] 1.3 Replace the `scrollIntoView` after a create with a flag in `onSuccess` and an `afterRenderEffect` that scrolls `.notitie-body` to its `scrollHeight`. Verify with a test: after adding a note, only the element holding the new note and the field *Voeg een notitie toe* (not the heading) scrolls, to its full height, and `scrollIntoView` is not called.
- [x] 1.4 Update the add test: the new note shows at the bottom of the list.
- [x] 1.5 Run `npx ng test --test-path-patterns=src/app/notities`, ESLint and `tsc` for `tsconfig.json` and `tsconfig.spec.json`.
- [x] 1.6 Check on 4200 against the local stack with a real POST on a long list: after adding a note, the list is at its bottom, the field is in view, the heading and *Sluiten* stay in place, also after the websocket reload.
