## Context

The zsa access data of a zaak consists of two answers:
- **is the zaak zsa?** Answered by `zrcClientService.isZaakspecifiekGeautoriseerd(uuid)`, a `listZaakeigenschappen`
  call to Open Zaak.
- **who are its geautoriseerde medewerkers?** The *Behandelaar* rollen plus the *Zaakspecifiek geautoriseerde
  medewerker* rollen, via `ZaakspecifiekeAutorisatieService.readZaakToewijzing(...).geautoriseerdeMedewerkerIds`,
  a `listRollen` call.

It is worked out in these places:

| Place | What it does now |
|---|---|
| `PolicyService.readZaakRechten(zaak, zaaktype, user)` | zsa call, plus `listRollen` when zsa (private `Zaak.isGeautoriseerdeMedewerkerOf`) |
| `PolicyService.readDocumentRechten(eio, lock, zaak)` | the same |
| `PolicyService.readTaakRechten(taskInfo, omschrijving)` | zsa call, plus `readZaak` and `listRollen` when zsa |
| `PolicyService` zoekobject overloads (zaak, taak, document) | `isZsa && user.id in zaakGeautoriseerdeMedewerkers`, the same expression three times |
| `RestZaakConverter.toRestZaak` | its own zsa call for `isZaakspecifiekGeautoriseerd` |
| `ReindexSupportService.zaakAutorisatieGegevens` (private) | builds `ZaakAutorisatieGegevens` (flag plus lazy medewerkers) for the index |

Some requests repeat the same calls. Each line below is one call to Open Zaak asking whether the zaak is zsa:

```
GET  zaken/zaak/{uuid}         readZaakRechten → zsa?   toRestZaak → zsa?                       = 2×
GET  taken/zaak/{uuid}         readZaakRechten → zsa?   per taak: readTaakRechten → zsa? (+ readZaak + listRollen)
                                                                                                = 1 + N×
GET  informatieobjecten?zaak   readZaakRechten → zsa?   per document: readZaak + readDocumentRechten → zsa?
                                                                                                = 1 + N×
PATCH zaken/zaak/{uuid}        readZaakRechten → zsa?   isAlreadyZaakspecifiekGeautoriseerd → zsa?   (before the writes)
```

`ZaakAutorisatieGegevens` (`nl.info.zac.search.model`) is already exactly the object needed: a flag plus a lazily
read list of medewerkers. Today only the index uses it.

## Goals / Non-Goals

**Goals:**
- One function produces the zsa access data, and both the rechten and the index use it.
- One check answers "is user X a geautoriseerde medewerker", replacing the six copies in `PolicyService`.
- The read paths in the table reuse the data within a request.
- Behaviour stays identical: the same OPA input, responses and index entries.

**Non-Goals:**
- Merging the zaak and taak assignment (write) flows.
- Adding access checks that are not there today (documenten without zaak context, taak signaleringen, notities,
  bulk verdelen of zaken).
- OPA rego changes.
- Any change to Java code.
- `RestGerelateerdeZaakConverter`, which reads rechten per related zaak. Those are different zaken, so nothing can
  be reused.
- `NotificationReceiver`, whose zsa check only drives reindexing.
- Optimising the separate `listRollen` that `RestZaakConverter` makes for groep, behandelaar and initiator.

## Decisions

### Reuse `ZaakAutorisatieGegevens` instead of introducing a new type

It already has the right shape, and the lazy medewerkers keep today's behaviour that rollen are only read for a zsa
zaak. Add one function to it:

```kotlin
fun isGeautoriseerdeMedewerker(userId: String) = isZaakspecifiekGeautoriseerd && userId in geautoriseerdeMedewerkers
```

The class stays in `nl.info.zac.search.model`. Moving it to `nl.info.zac.zaak.model` would read better, but it
touches every index converter for no behavioural gain. That can be done later.

### The derivation moves to `ZaakspecifiekeAutorisatieService`

`readZaakAutorisatieGegevens(zaak: Zaak)` and `readZaakAutorisatieGegevens(zaakUuid: UUID)` move there, taking the
body of today's private `ReindexSupportService.zaakAutorisatieGegevens`. `ReindexSupportService` delegates to them,
and its memoising lookup for the index stays where it is.

### Explicit passing, not a request-scoped cache

The live `PolicyService` overloads, `RestZaakConverter.toRestZaak`, `RestTaskConverter.convert` and
`RestInformatieobjectConverter.convertToREST` get an optional `zaakAutorisatieGegevens` parameter. Its default reads
the data, as today. The read paths in the table read it once and pass it down.

*Alternative considered:* a `@RequestScoped` cache keyed by zaak UUID. That would need no signature changes, but a
cache is invisible to its callers. `updateZaak`, the taak assignment paths and `markZaakspecifiekGeautoriseerd`
change the marking or the rollen in the middle of a request, and a cache would then serve stale data to the
response. Preventing that would require invalidating the cache after every write. Explicit passing makes the reuse
visible exactly where it happens, and after a write the default re-reads.

### The three zoekobject overloads build the same object

Build it from the fields the zoekobject already has:
`ZaakAutorisatieGegevens(zoekObject.isZaakspecifiekGeautoriseerd) { zoekObject.zaakGeautoriseerdeMedewerkers.orEmpty() }`.
Then call `isGeautoriseerdeMedewerker(loggedInUser.id)`. That makes it one check for both the live and the Solr
paths, with no new data source.

### Which read paths reuse the data

- **`ZaakRestService.readZaak` and `readZaakById`:** read the data once, then pass it to `readZaakRechten` and
  `toRestZaak`.
- **`ZaakRestService.updateZaak`:** read the data once at the start for `readZaakRechten` and
  `isAlreadyZaakspecifiekGeautoriseerd`. The final `toRestZaak` uses its default, because it must re-read after
  `markZaakspecifiekGeautoriseerd` and `assignZaak`.
- **`TaskRestService.listTasksForZaak`:** read the data once, then pass it to `readZaakRechten` and to
  `RestTaskConverter.convert(tasks, zaakAutorisatieGegevens)`, which passes it to `readTaakRechten` for each taak.
  `readTaakRechten` then no longer needs `readZaak` when the data is given.
- **`EnkelvoudigInformatieObjectRestService.listEnkelvoudigInformatieobjectenVoorZaak`:** read the data once for the
  zaak that is already read. Pass it to `convertToREST(zaakInformatieObject, zaak, zaakAutorisatieGegevens)`, so
  the zaak is not read again for every document either.

## Risks / Trade-offs

- [A caller passes data read before a write it made itself] → Only the read-only paths listed above pass the data.
  The spec requirement "after a write the data is read again" is covered by a test on `updateZaak` marking a zaak.
  Every new parameter defaults to reading the data, so a forgotten argument is slower, never wrong.
- [`ZaakAutorisatieGegevens` lives in the search package but is now used by rechten too] → Accepted for now; see
  Decisions.
- [Signature changes ripple into unit tests] → The new parameters are optional with defaults, so existing calls
  compile. Tests that verify exact calls on `zrcClientService` may need adjusting, which is intended because the
  calls really do drop.

## Migration Plan

None. It is a refactoring with no data, API or configuration change. Rollback is a revert.

## Open Questions

None.
