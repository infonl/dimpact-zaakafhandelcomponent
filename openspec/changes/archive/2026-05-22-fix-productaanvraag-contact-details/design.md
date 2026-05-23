## Context

`KlantClientService.findRequestSpecificContactDetailsForKlantcontact` (renamed from the earlier `findContactDetailsForKlantcontact`) fetches the first betrokkene from a klantcontact, calls `getBetrokkeneWithDigitaleAdressen(betrokkene.uuid)`, and passes the returned `digitaleAdressen` through a filter before calling `toContactDetails()`. The `DigitaalAdres` Java model has a nullable boolean field:
- `isStandaardAdres` — whether this address is the citizen's saved preferred address

In SQL (`klantinteracties_digitaaladres`) this maps to `is_standaard_adres`.

Existing test data for request-specific scenarios (digital addresses 9–11 in `1-setup-applicatie.sql`) all have `is_standaard_adres = false`. The new bug scenario requires data where `is_standaard_adres = true`.

`findProductaanvraagSpecificContactDetails` returns `ProductaanvraagSpecificContactDetails?`; a null return prevents `linkProductaanvraagSpecificContactDetailsToZaak` from being called. `findZaakSpecificContactDetails` searches for klantcontacten via an onderwerpobject with `codeObjecttype = "zaak"` — if the link is never created, it returns null. Both code paths converge on `findRequestSpecificContactDetailsForKlantcontact`, so the fix at that single point fixes both the productaanvraag processing and the zaak detail retrieval.

The email fallback in `ProductaanvraagEmailService.sendConfirmationOfReceiptEmailFromProductaanvraag` already handles `productaanvraagSpecificEmailAddress == null` by calling `extractBetrokkeneEmail(betrokkene)`, which delegates to `fetchEmailForNatuurlijkPersoon` (BSN path) or `fetchEmail` (KVK path). Those fetch from partij digital addresses, which is exactly where the saved address lives in scenario 1.a.i.

## Goals / Non-Goals

**Goals:**
- Filter out preferred (`isStandaardAdres = true`) and unmarked (`isStandaardAdres = null`) digital addresses from betrokkene contact-detail lookups; only include explicitly non-preferred (`isStandaardAdres = false`) addresses
- Ensure scenario 1.a.i yields null `zaakSpecificContactDetails` in the zaak detail response
- Ensure the confirmation email is still sent via the partij address fallback in scenario 1.a.i
- Add integration test coverage for scenario 1.a.i in `NotificationProductaanvraagCmmnTest`
- Leave scenarios 1.a.ii.1, 1.b (new profile), anonymous, KVK/vestiging unchanged

**Non-Goals:**
- Changing the BPMN path (`NotificationProductaanvraagBpmnTest`) — same `KlantClientService` method is used, so it benefits automatically, but no new BPMN itest is added in this change
- Changing how `findDigitalAddressesForNaturalPerson` / `findDigitalAddressesForVestiging` / `findDigitalAddressesForNonNaturalPerson` work
- Any API contract change

## Decisions

### Filter `isStandaardAdres == false` inside `findRequestSpecificContactDetailsForKlantcontact`

Apply the filter at the single shared extraction point rather than in each caller (`findZaakSpecificContactDetails`, `findProductaanvraagSpecificContactDetails`). This avoids duplication and guarantees consistent behaviour. Only addresses explicitly marked as non-preferred (`isStandaardAdres = false`) are returned; `null` (unset) is treated the same as `true` (preferred) because only `false` unambiguously signals "this address was provided specifically for this request".

```kotlin
// before
expandedBetrokkene?.digitaleAdressen
    ?.filter { it.verstrektDoorPartij == null }
    ?.takeIf { it.isNotEmpty() }
    ?.toContactDetails()

// after
expandedBetrokkene?.digitaleAdressen
    ?.let { digitaleAdressen ->
        val nonPreferredDigitalAddresses = digitaleAdressen.filter { it.isStandaardAdres == false }
        if (nonPreferredDigitalAddresses.isEmpty()) null
        else nonPreferredDigitalAddresses.toContactDetails()
    }
```

The `isEmpty()` guard ensures we return `null` rather than `ContactDetails(null, null)` when no non-preferred address exists, keeping the calling code's null-check semantics consistent.

Alternative considered: filtering on `verstrektDoorPartij == null`. Rejected — Open Klant sets `partij_id` on any address linked to a partij regardless of whether it is preferred, and there are legitimate cases where a non-preferred request-specific address also has a `partij_id`. The `isStandaardAdres` flag directly encodes the "saved preference vs. one-time detail" distinction.

Alternative considered: checking `wasPartij != null` on the betrokkene. Rejected — a betrokkene linked to a partij can still submit a genuinely request-specific (non-preferred) digital address; the partij link alone does not disqualify the address.

### New itest data: klantcontact 5, betrokkene 8, digital address 12, onderwerpobject 3

Add the minimum rows needed to simulate scenario 1.a.i in the OpenKlant database. Use the second persoon partij 4 (BSN 999992958, Anita van Buren) since that represents the "profile already exists" case with a distinct email from partij 1. Digital address 12 has `is_standaard_adres = true` and `partij_id = 4` to trigger the bug path.

Note: simulating a *newly* saved preferred address (where the citizen updates their address during a productaanvraag) cannot be fully replicated in static SQL init data because it requires making API calls to Open Klant during the test. The static data represents the state *after* Open Klant has already persisted the saved preferred address.

### Test asserts `zaakSpecificContactDetails` is null or absent

Use `isNull("zaakSpecificContactDetails")` (JSONObject API) so the test is robust to both JSON `null` and key absence, matching how `RestZaakConverter` serialises a null Kotlin field.

## Risks / Trade-offs

- **Scenarios where `isStandaardAdres = null` but the address is genuinely request-specific** → not expected per Open Klant's documented contract; Open Formulieren sets `isStandaardAdres = false` on all request-specific addresses. If a future form integration omits the flag, the address would be silently excluded — acceptable given the alternative (incorrectly treating saved preferences as request-specific) is worse.
- **`toContactDetails()` on an empty list** → guarded by the `isEmpty()` check returning null before reaching `toContactDetails`.
- **Temporary `LOG.info` in `findRequestSpecificContactDetailsForKlantcontact`** → retained; can be removed in a follow-up once the fix has been verified on the TEST environment.
