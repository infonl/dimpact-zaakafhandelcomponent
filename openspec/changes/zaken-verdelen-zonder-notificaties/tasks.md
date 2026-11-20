## 1. Backend PR: ZAC reports each zaak itself

- [ ] 1.1 Add `skipped(uuid: UUID)` and `updated(eventResourceId: String, detail: String)` to `ScreenEventType`;
  cover both in `ScreenEventTypeTest`.
- [ ] 1.2 `assignZaken` / `releaseZaken`: read and handle one zaak at a time; send `ZAKEN_VERDELEN` /
  `ZAKEN_VRIJGEVEN` `updated(zaak.uuid)` after `assignZaak` returns and `skipped(zaak.uuid)` wherever
  `ZAAK_ROLLEN.skipped(zaak)` is sent now (keep that send for now, design decision 5). In `isUserInGroup` send
  `skipped(uuid)` per UUID without reading the zaken.
- [ ] 1.3 Catch any other exception per zaak, log it at `SEVERE` with the zaak UUID only, and continue. Move the
  group and user lookups of `ZaakAssignAndReleaseRestService` (`:67-79`) inside the batch's `try`. Send exactly
  one final event in `finally`, with as detail the number of zaken that got no per-zaak event.
- [ ] 1.4 Per batch, in `assignZaken` and `releaseZaken`: call the PABC once per zaaktype (verdelen only); pass
  `isZaakspecifiekGeautoriseerd = false` through `assignZaak` to `readZaakToewijzing` for zaaktypes without the
  `ZAAK_GEAUTORISEERD` eigenschap.
- [ ] 1.5 `ZaakServiceTest`: per-zaak `UPDATED` and `SKIPPED` events for verdelen and vrijgeven; an Open Zaak error
  and a read error on one zaak (replaces the test at `:1081`): the other zaken are processed, no per-zaak event
  for the failed one, the final event carries the failed count (`0` when all succeed); a failing group or user
  lookup and a failing group membership check each send one final event that counts every zaak as failed
  (also in `ZaakAssignAndReleaseRestServiceTest`); the PABC is called once
  for two zaken of one zaaktype; no `listZaakeigenschappen` for a zaaktype without the eigenschap, and still for
  one with it, for both verdelen and vrijgeven. Run `./gradlew test --tests "nl.info.zac.zaak.ZaakServiceTest"`.
- [ ] 1.6 Itest in `ZaakRestServiceTest` ("Assigning zaken from the list", "Releasing zaken from the list"):
  listen for `ZAKEN_VERDELEN` / `ZAKEN_VRIJGEVEN` per zaak UUID and assert an `UPDATED` event per zaak plus the
  final event with failed count `0`. Add a case with a zaak UUID that does not exist in Open Zaak: the other
  zaken get `UPDATED`, the unknown one gets no event, and the final event reports `1` failed zaak (AC8,
  ADR 0008). Run `./gradlew itest --tests "nl.info.zac.itest.ZaakRestServiceTest"`.
- [ ] 1.7 `./gradlew spotlessApply detektApply` and `./gradlew detekt`; SPDX headers of touched files.

## 2. Frontend PR: the werkvoorraad follows ZAC

- [ ] 2.1 `zaken-werkvoorraad.component.ts`: one `screenEventResourceId` per batch passed to both dialogs;
  `progressSubscription` on `ZAKEN_VERDELEN` / `ZAKEN_VRIJGEVEN`; `finalSubscription` with opcode `ANY`; drop the
  up-front `notProcessed` marking for verdelen; reload without the 5 second delay.
- [ ] 2.2 `BatchProcessService`: an optional `onNotification(event)` on `finalSubscription`, called before
  `finally`. In the werkvoorraad, read the failed count from the final event's `detail` there (not in `finally`,
  which also runs on timeout) and show `msg.zaken.verdelen.mislukt.*` / `msg.zaken.vrijgeven.mislukt.*` with
  the count; add these keys to `nl.json` and `en.json` with the four texts of the spec.
- [ ] 2.3 `zaken-verdelen-dialog` and `zaken-vrijgeven-dialog`: send the `screenEventResourceId` they receive.
- [ ] 2.4 Specs: werkvoorraad (progress on the new events, a `ZAAK_ROLLEN` event does not count, final event ends
  the batch, error message with the count from the final event, no message when the count is `0`, the final event
  before the last per-zaak event, a timeout shows only the timeout message), `BatchProcessService` (the final
  callback gets the event; without it nothing changes) and both dialogs (send the given ID). Run
  `cd src/main/app && npm test -- zaken-werkvoorraad zaken-verdelen-dialog zaken-vrijgeven-dialog batch-process` and
  `npm run lint`.
- [ ] 2.5 Remove the `ZAAK_ROLLEN.skipped` sends from `ZaakService` and their unit test expectations; move the
  itests at `ZaakRestServiceTest.kt:2120-2260` to the `ZAKEN_VERDELEN` / `ZAKEN_VRIJGEVEN` `SKIPPED` event. Run
  both test commands of group 1 and `./gradlew detekt`.
- [ ] 2.6 Try it out in the local kind stack (`./start-kind.sh -l -b`) as a coordinator on the zaken
  werkvoorraad: distribute and release a selection; scale the Open Notificaties deployment to zero replicas
  and repeat: the progress ends without timeout message and the rows show the new behandelaar. `zac-proof` can
  guide this.

## 3. Docs PR

- [ ] 3.1 Gebruikershandleiding, "Werk verdelen" and "Werk vrijgeven"
  (`docs/manuals/ZAC-gebruikershandleiding/ZAC-gebruikershandleiding.md:345-389`): a zaak that could not be
  distributed or released is reported with a message that gives the number.
- [ ] 3.2 Archive this change (`zac-apply-specs`, step 5).
