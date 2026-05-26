## Why

When a citizen with an existing Open Klant profile submits a productaanvraag and updates their contact address while ticking "Sla mijn gegevens op voor de volgende keer" (scenario 1.a.i), Open Klant creates a new digital address linked to **both** the betrokkene (interaction participant) and the partij (persoon entity), and marks it as the preferred (`isStandaardAdres = true`) address. ZAC's `findRequestSpecificContactDetailsForKlantcontact` fetches that address from the betrokkene and returns it as a zaak-specific contact detail — even though it is simply the citizen's newly saved preferred address, not a one-time aanvraag-specific contact detail. This causes the zaak detail page to show incorrect zaak-specific contact details and, via `linkProductaanvraagSpecificContactDetailsToZaak`, incorrectly links the klantcontact to the zaak as if it were a zaak-scoped address.

When a citizen uses their existing preferred address without changes (scenario 1.a.ii.1), Open Klant creates **no** new digital address on the betrokkene, so ZAC already returns null — that part works correctly.

## What Changes

- In `KlantClientService.findRequestSpecificContactDetailsForKlantcontact`, iterate all betrokkenen via `firstNotNullOfOrNull` and select the first betrokkene where `initiator == true && rol == RolEnum.KLANT`; a `NotFoundException` during fetch skips that betrokkene rather than aborting the lookup.
- Filter the selected betrokkene's `digitaleAdressen` to only include those where `isStandaardAdres == false` (explicitly non-preferred addresses). Preferred addresses (`isStandaardAdres = true`) and addresses with no preference marker (`isStandaardAdres = null`) are not aanvraag/zaak-specific.
- As a result, `findProductaanvraagSpecificContactDetails` returns `null` in scenario 1.a.i → `linkProductaanvraagSpecificContactDetailsToZaak` is not called → `findZaakSpecificContactDetails` returns null → zaak detail page shows no zaak-specific contact details.
- The `wasPartij` field on a betrokkene is no longer checked; a betrokkene linked to a partij can still have genuinely request-specific (non-preferred) digital addresses.
- Email sending in `ProductaanvraagEmailService` already falls back to `fetchEmailForNatuurlijkPersoon` / `fetchEmail` (partij-level lookup) when `productaanvraagSpecificEmailAddress` is null, so confirmation emails continue to be sent correctly.
- Add integration test data (new klantcontact 5, betrokkene 8, digital address 12 with `is_standaard_adres = true`) and a new `Given/When/Then` block in `NotificationProductaanvraagCmmnTest` covering scenario 1.a.i.

## Capabilities

### Modified Capabilities

- `productaanvraag-contact-details-retrieval`: `findRequestSpecificContactDetailsForKlantcontact` now correctly excludes preferred (`isStandaardAdres = true`) and unset (`isStandaardAdres = null`) digital addresses, so only explicitly non-preferred (`isStandaardAdres = false`) addresses are returned as zaak-specific contact details.

## Impact

- `KlantClientService.findRequestSpecificContactDetailsForKlantcontact` — logic change: iterate all betrokkenen selecting first with `initiator == true && rol == KLANT`; filter digital addresses on `isStandaardAdres == false` replacing the earlier `verstrektDoorPartij == null` approach; `wasPartij` check removed
- `1-setup-applicatie.sql` — new test data rows: klantcontact 5, betrokkene 8 (BSN 999992958 / Anita van Buren, partij 4), digital address 12 (`is_standaard_adres = true`, `betrokkene_id = 8`, `partij_id = 4`), onderwerpobject 3
- `ItestConfiguration.kt` — new constants for the new scenario; email verified via existing `TEST_PERSON_ANITA_VAN_BUREN_EMAIL`
- `NotificationProductaanvraagCmmnTest.kt` — new test scenario verifying null zaak-specific contact details and correct email delivery to the saved preferred address
- No API contract change
- No new dependencies
- Existing scenarios (anonymous request-specific, BSN with request-specific, vestiging/KVK) are unaffected because those digital addresses already have `is_standaard_adres = false`
