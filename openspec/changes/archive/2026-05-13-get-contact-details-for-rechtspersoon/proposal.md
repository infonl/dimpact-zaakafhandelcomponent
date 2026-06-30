## Why

`readRechtspersoonByKvkNummer` currently has a TODO to fetch contact details from Open Klant but never does, leaving email and phone blank for rechtspersonen retrieved by KVK number. `KlantClientService.findDigitalAddressesForNonNaturalPerson` already exists and is unused by this endpoint.

## What Changes

- Extend `readRechtspersoonByKvkNummer` in `KlantRestService` to concurrently fetch digital addresses via `klantClientService.findDigitalAddressesForNonNaturalPerson(kvkNummer)` alongside the existing KVK lookup, then merge email/phone into the `RestBedrijf` response (same pattern as `readVestiging`)
- Remove the TODO comment
- Add unit test coverage for the new concurrent fetch behaviour (success + no-contact-details cases)
- Add integration test coverage for the endpoint returning contact details

## Capabilities

### New Capabilities

- `rechtspersoon-contact-details`: Retrieve and return email/phone contact details for a rechtspersoon identified by KVK number via the Open Klant digital-addresses API

### Modified Capabilities

<!-- none -->

## Impact

- `KlantRestService.readRechtspersoonByKvkNummer` — logic change, adds parallel coroutine call
- `KlantRestServiceTest` (unit) — new test cases
- `KlantRestServiceTest` (itest) — new integration test assertion
- No API contract change (`RestBedrijf` already has `emailadres` and `telefoonnummer` fields)
- No new dependencies
