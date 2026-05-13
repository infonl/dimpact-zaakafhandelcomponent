## Context

`readRechtspersoonByKvkNummer` wraps its KVK lookup in a `runBlocking { withContext(Dispatchers.IO) { ... } }` coroutine scope but never launches a second async call for contact details, despite having a TODO comment to do so. `KlantClientService.findDigitalAddressesForNonNaturalPerson(kvkNummer)` is the exact counterpart already used for vestiging and natural person flows. `RestBedrijf` already carries `emailadres` and `telefoonnummer` fields, so no model changes are needed.

## Goals / Non-Goals

**Goals:**
- Fetch Open Klant digital addresses for a rechtspersoon concurrently with the KVK lookup
- Merge email and phone into the `RestBedrijf` response
- Achieve feature parity with `readVestiging` for contact-detail enrichment
- Add unit and integration test coverage

**Non-Goals:**
- Changing the `readRechtspersoonByRsin` (legacy) endpoint — it stays contact-detail-free by design
- Modifying `KlantClientService` — the required method already exists
- Any API contract change — response shape is unchanged

## Decisions

### Use the existing `readVestiging` concurrency pattern verbatim

Replace the body of `readRechtspersoonByKvkNummer` with two `async` calls inside the existing `withContext(Dispatchers.IO)` scope — one for `kvkClientService.findRechtspersoonByKvkNummer(kvkNummer)` and one for `klantClientService.findDigitalAddressesForNonNaturalPerson(kvkNummer)` — then `await()` both and merge.

Alternative considered: sequential calls. Rejected because the whole point of the existing coroutine scaffold is latency reduction; making the second call sequential would be inconsistent and slower.

### No null-guard needed for KVK number

Unlike `readVestiging` which accepts an optional `kvkNummer` and skips the Klant call when null, `readRechtspersoonByKvkNummer` always has a valid KVK number (validated by `@Length(min=8, max=8)`). No conditional branch required.

## Risks / Trade-offs

- **Open Klant returns no data for rechtspersoon** → `toContactDetails()` returns an object with null fields; `emailadres`/`telefoonnummer` on `RestBedrijf` stay null. Same graceful degradation as vestiging. No extra handling needed.
- **Open Klant call fails** → unhandled exception will propagate to the caller, same as current behaviour for KVK failures. Acceptable; both systems are required infrastructure.
- **Latency increase** → none; calls run concurrently. If KVK is already the bottleneck, total latency is unchanged.
