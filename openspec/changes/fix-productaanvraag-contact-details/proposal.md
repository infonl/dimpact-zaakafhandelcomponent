## Why

When a citizen with an existing Open Klant profile submits a productaanvraag and updates their contact address while ticking "Sla mijn gegevens op voor de volgende keer" (scenario 1.a.i), Open Klant creates a new digital address linked to **both** the betrokkene (interaction participant) and the partij (persoon entity). ZAC's `findContactDetailsForKlantcontact` fetches that address from the betrokkene and returns it as a zaak-specific contact detail — even though it is simply the citizen's newly saved preferred address, not a one-time aanvraag-specific contact detail. This causes the zaak detail page to show incorrect zaak-specific contact details and, via `linkProductaanvraagSpecificContactDetailsToZaak`, incorrectly links the klantcontact to the zaak as if it were a zaak-scoped address.

When a citizen uses their existing preferred address without changes (scenario 1.a.ii.1), Open Klant creates **no** new digital address on the betrokkene, so ZAC already returns null — that part works correctly.

## What Changes

- In `KlantClientService.findContactDetailsForKlantcontact`, filter the betrokkene's `digitaleAdressen` to only include those where `verstrektDoorPartij == null` (betrokkene-only addresses). Addresses that are also linked to a partij are not aanvraag/zaak-specific.
- As a result, `findProductaanvraagSpecificContactDetails` returns `null` in scenario 1.a.i → `linkProductaanvraagSpecificContactDetailsToZaak` is not called → `findZaakSpecificContactDetails` returns null → zaak detail page shows no zaak-specific contact details.
- Email sending in `ProductaanvraagEmailService` already falls back to `fetchEmailForNatuurlijkPersoon` / `fetchEmail` (partij-level lookup) when `productaanvraagSpecificEmailAddress` is null, so confirmation emails continue to be sent correctly.
- Add integration test data (new klantcontact, betrokkene, digital address with partij_id set) and a new `Given/When/Then` block in `NotificationProductaanvraagCmmnTest` covering scenario 1.a.i.

## Capabilities

### Modified Capabilities

- `productaanvraag-contact-details-retrieval`: `findContactDetailsForKlantcontact` now correctly excludes partij-linked digital addresses, so only genuinely aanvraag-specific (betrokkene-only) addresses are returned as zaak-specific contact details.

## Impact

- `KlantClientService.findContactDetailsForKlantcontact` — logic change, one-line filter addition
- `1-setup-applicatie.sql` — new test data rows (klantcontact, betrokkene, digital address, onderwerpobject)
- `ItestConfiguration.kt` — new constants for the new scenario
- `NotificationProductaanvraagCmmnTest.kt` — new test scenario verifying null zaak-specific contact details and correct email delivery
- No API contract change
- No new dependencies
- Existing scenarios (anonymous request-specific, BSN with request-specific, vestiging/KVK) are unaffected because those digital addresses already have `partij_id = NULL`
