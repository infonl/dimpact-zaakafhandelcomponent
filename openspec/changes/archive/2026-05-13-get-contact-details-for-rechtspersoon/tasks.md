## 1. Extend readRechtspersoonByKvkNummer in KlantRestService

- [x] 1.1 In `KlantRestService.readRechtspersoonByKvkNummer`, introduce a second `async` call for `klantClientService.findDigitalAddressesForNonNaturalPerson(kvkNummer)` alongside the existing `kvkClientService.findRechtspersoonByKvkNummer(kvkNummer)` call, inside the existing `withContext(Dispatchers.IO)` block
- [x] 1.2 `await()` both deferred values, call `.toContactDetails()` on the digital addresses result, and apply `emailadres` and `telefoonnummer` to the returned `RestBedrijf` using `.apply { ... }`, matching the pattern in `readVestiging`
- [x] 1.3 Remove the `// TODO: get contactdetails for rechtspersoon from Open Klant` comment

## 2. Unit tests (KlantRestServiceTest)

- [x] 2.1 Add a `Given` block inside the existing `Context("Reading a rechtspersoon by KVK nummer")` for the case where Open Klant returns digital addresses (email + phone): mock `klantClientService.findDigitalAddressesForNonNaturalPerson(kvkNummer)` with a non-empty list, verify `emailadres` and `telefoonnummer` on the returned `RestBedrijf`
- [x] 2.2 Add a `Given` block for the case where Open Klant returns an empty list: mock `klantClientService.findDigitalAddressesForNonNaturalPerson(kvkNummer)` returning `emptyList()`, verify `emailadres` and `telefoonnummer` are null

## 3. Integration test data and test (KlantRestServiceTest itest)

- [x] 3.1 In `scripts/docker-compose/imports/openklant-database/database/1-setup-applicatie.sql`, add a telephone number `digitaalAdres` row linked to partij 2 (the `niet_natuurlijk_persoon` / KVK bedrijf partij); add a corresponding `TEST_RECHTSPERSOON_TELEPHONE_NUMBER` constant to `ItestConfiguration`
- [x] 3.2 Update the existing itest `When` block at `KlantRestServiceTest.kt:661` (`readRechtspersoonByKvkNummer`) to rename its `Then` description and add `emailadres` (`TEST_KVK_EMAIL`) and `telefoonnummer` (`TEST_RECHTSPERSOON_TELEPHONE_NUMBER`) to the `shouldEqualJson` assertion
