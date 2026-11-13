## 1. Frontend: notes in the zaak menu

- [ ] 1.1 `ButtonMenuItem` takes an optional count; `side-nav.component.html` renders the label and tooltip as
      `<title> (<count>)` when the count is above 0. Verify with a test in `side-nav.component.spec.ts` for a count of
      2 (label *Notities (2)*) and of 0 (label *Notities*).
- [ ] 1.2 `buildZaakMenu` takes the number of notes (`null` when the employee may not see notes) and adds the item
      `notities` with icon `sticky_note_2` after the document actions and before *Zaakdata*. Verify with tests in
      `zaak-view-menu.builder.spec.ts`: item present with its count and position; absent for `null`; present on a
      closed zaak and a process-driven zaak.
- [ ] 1.3 `NotitieService.listNotities` drops `StaleTimes.Instant`. `ZaakViewComponent` adds the notes query (enabled
      only with `canLezen || canWijzigen`), passes its length to the menu builder, and owns the `ZAAK_NOTITIES`
      websocket listener in an `effect` with `onCleanup`. Verify with zaak view specs: the menu shows *Notities (2)*
      for 2 notes, no item without rights, and the count changes after the cache is invalidated through the listener.
- [ ] 1.4 The zaak view template renders `<zac-notities [sideNav]="actionsSidenav" ...>` in the right drawer for the
      active action `notities`, and no longer at the end of the content. Verify with a zaak view spec: choosing
      *Notities* in the menu opens the panel; there is no notes button at the bottom right.
- [ ] 1.5 `NotitiesComponent` becomes a side panel: `mat-toolbar` heading with icon, *Notities (n)* and a close
      button, the notes list, the add field, and a *Sluiten* button; it takes a `sideNav` input. Remove the FAB, the
      badge, `showNotes`, the toggle, `MatBadgeModule` and the fixed positioning in the `.less`. Verify with
      `notities.component.spec.ts`: heading with count, close and *Sluiten* close the drawer, read-only shows no
      controls.
- [ ] 1.6 `NotitiesComponent` keeps the new and the edited text in signals; drop the `notitieTekst` `viewChild` and the
      `nativeElement.value` writes; the counters and the disabled send button read the signals; the websocket
      listener is removed from the component. Verify with specs: send is disabled while empty, the field is empty
      after a save, cancel restores the old text, add/update/delete update the list and the count.
- [ ] 1.7 Update the zaak view specs (`zaak-view.component*.spec.ts`) and `notities.component.harness.spec.ts` that
      look for the old *Notities* button, and remove `NotitiesComponent` from spec imports where it is no longer
      needed. Verify with `npm test -- --test-path-patterns "notities|zaak-view|side-nav"`.
- [ ] 1.8 Run `npm run lint`, `npx ngc -p tsconfig.app.json --noEmit` and `./gradlew spotlessApply`; check the zaak
      view in the local stack: the item, the count, opening and closing the panel, adding and deleting a note.
