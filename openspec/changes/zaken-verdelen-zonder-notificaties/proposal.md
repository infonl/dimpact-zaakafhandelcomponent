## Why

When an employee distributes (verdelen) or releases (vrijgeven) a selection of zaken from the zaken werkvoorraad,
the progress dialog waits for a `ZAAK_ROLLEN` screen event per zaak. ZAC only sends that event when the `rol`
notification of Open Notificaties comes in, so the screen waits on the chain Open Zaak → Open Notificaties → ZAC.
When notifications are slow or never arrive, the progress lags or ends after 5 minutes with
"Het proces duurde te lang en is afgebroken.", while the zaken in Open Zaak are already distributed. And an
unexpected error from Open Zaak on one zaak stops the whole batch: the other zaken are not processed and the
employee only sees the timeout.

ZAC already knows when a zaak is done: when Open Zaak has confirmed the rol changes. This change makes the
werkvoorraad follow ZAC itself instead of the notifications, as agreed in the architecture discussion of
[PZ-13239](https://dimpact.atlassian.net/browse/PZ-13239).
Story: [PZ-13284](https://dimpact.atlassian.net/browse/PZ-13284).

## What Changes

- **A ZAC event per zaak.** After the rol changes of a zaak are confirmed by Open Zaak, ZAC sends a
  `ZAKEN_VERDELEN` or `ZAKEN_VRIJGEVEN` screen event for that zaak (`UPDATED`), and a `SKIPPED` one for a zaak it
  skips. Today skipped zaken get a `ZAAK_ROLLEN` `SKIPPED` event; that moves to the new event.
- **The werkvoorraad listens to ZAC, not to notifications.** The zaken werkvoorraad counts progress on the new
  events and no longer on `ZAAK_ROLLEN`. Rol notifications keep doing everything they do today (signaleringen,
  the search index, the `ZAAK_ROLLEN` event that updates other screens such as the zaakdetailpagina), but they no
  longer move the werkvoorraad progress.
- **The batch ends on the final event.** The werkvoorraad finishes when ZAC's final `ZAKEN_VERDELEN` /
  `ZAKEN_VRIJGEVEN` event for the batch arrives, as the taken werkvoorraad already does. The vrijgeven dialog
  starts sending a `screenEventResourceId`; the verdelen dialog sends the one the werkvoorraad listens to.
- **One failing zaak no longer stops the batch.** ZAC logs the error, continues with the next zaak and always
  sends the final event. The werkvoorraad then shows how many zaken could not be distributed or released.
- **Rights once per zaaktype.** Within a batch, ZAC asks the PABC which groups may handle a zaaktype once per
  zaaktype, and skips the per-zaak lookup of the *zaakspecifiek geautoriseerd* eigenschap for zaaktypes that do
  not define it (the remaining part of [PZ-12678](https://dimpact.atlassian.net/browse/PZ-12678) for this flow).
- **No extra 5 second wait.** ZAC indexes each zaak before it reports it, so the werkvoorraad reloads right after
  the final event and the Solr commit.

## Capabilities

### New Capabilities

- `werkvoorraad-zaken-verdelen-vrijgeven`: distributing and releasing a selection of zaken from the zaken
  werkvoorraad. No spec describes this yet, so the capability records the current behaviour too.
  - **Existing behaviour, recorded as it is**: *Verdelen and vrijgeven run in the background*,
    *ZAC skips zaken it may not or cannot change*.
  - **New behaviour of this story**: *The werkvoorraad follows ZAC's own event per zaak*, *The batch ends on
    ZAC's final event*, *An error on one zaak does not stop the batch*, *Rol notifications do not move the
    werkvoorraad progress*, *Rights are determined once per zaaktype in a batch*.

### Modified Capabilities

None. `zaakspecifieke-autorisatie-activatie` keeps its requirements on which zaakspecifiek geautoriseerde zaken
are skipped and the message about them; this change does not alter them.

## Impact

- **Backend**
  - `ZaakService.assignZaken`, `releaseZaken`, `assignZaakFromBatch`, `releaseZaakFromBatch`, `isUserInGroup`,
    `isAuthorisedForApplicationRoleAndZaaktype` (`src/main/kotlin/nl/info/zac/zaak/ZaakService.kt:126-545`).
  - `ZaakService.assignZaak(zaak, zaakAssignment, reason)` (`ZaakService.kt:198`) and
    `ZaakspecifiekeAutorisatieService.readZaakToewijzing` (`ZaakspecifiekeAutorisatieService.kt:85`), which take
    the known marking from the batch. `assignZaak` is also called by the single-zaak endpoints in
    `ZaakAssignAndReleaseRestService.kt`; their behaviour does not change.
  - `ScreenEventType` (`src/main/kotlin/nl/info/zac/websocket/event/ScreenEventType.kt:79-81, 340-348`): a
    `skipped(uuid)` factory.
  - REST: `PUT /rest/zaken/lijst/verdelen` and `PUT /rest/zaken/lijst/vrijgeven` keep path and payload; the
    vrijgeven call now gets a `screenEventResourceId` filled in. No caller in `scripts/test-data/`.
- **Frontend**
  - `zaken-werkvoorraad.component.ts:333-441`, `zaken-verdelen-dialog.component.ts:164`,
    `zaken-vrijgeven-dialog.component.ts:75-82`, their specs, and two new texts per language in `nl.json` /
    `en.json`.
  - `BatchProcessService` is reused as it is.
- **Tests**
  - `ZaakServiceTest` ("Assigning zaken in a batch" `:797`, "Releasing zaken" `:1342`): the `ZAAK_ROLLEN.skipped`
    expectations move to the new event, and the test at `:1081` that expects a read error to stop the batch
    changes to the new behaviour.
  - Itests `ZaakRestServiceTest.kt:1066-1345` and `:2120-2260` (they listen for `ZAAK_ROLLEN` `SKIPPED` at
    `:2126`).
- **Docs**: the "Werk verdelen" / "Werk vrijgeven" chapters of the gebruikershandleiding
  (`docs/manuals/ZAC-gebruikershandleiding/ZAC-gebruikershandleiding.md:345-389`).
- No Flyway, Solr schema, Flowable or configuration changes.

### Out of scope

- Distributing and releasing taken from the taken werkvoorraad: ZAC already sends those events itself.
- Assigning a single zaak from the zaakdetailpagina or with *Aan mij toekennen*.
- The per-notification zaakspecifiek check in `NotificationReceiver.handleRolIndexing`
  (`NotificationReceiver.kt:301`), the other half of PZ-12678.
- Telling the employee *which* zaken failed; the story asks for the number only.
