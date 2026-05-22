## Context

`KlantClientService.findContactDetailsForKlantcontact` (lines 181–192) fetches the first betrokkene from a klantcontact, calls `getBetrokkeneWithDigitaleAdressen(betrokkene.uuid)`, and passes all returned `digitaleAdressen` to `toContactDetails()` without filtering. The `DigitaalAdres` Java model has two nullable foreign-key fields:
- `verstrektDoorBetrokkene` — the betrokkene that owns this address
- `verstrektDoorPartij` — the partij linked to this address (non-null means the address is persisted as a general party preference, not an aanvraag-specific detail)

In SQL (`klantinteracties_digitaaladres`) this maps to `betrokkene_id` and `partij_id`.

Existing test data for request-specific scenarios (digital addresses 9–11 in `1-setup-applicatie.sql`) all have `partij_id = NULL`. The new bug scenario requires data where `partij_id` is NOT NULL.

`findProductaanvraagSpecificContactDetails` returns `ProductaanvraagSpecificContactDetails?`; a null return prevents `linkProductaanvraagSpecificContactDetailsToZaak` from being called. `findZaakSpecificContactDetails` searches for klantcontacten via an onderwerpobject with `codeObjecttype = "zaak"` — if the link is never created, it returns null. Both code paths converge on `findContactDetailsForKlantcontact`, so the fix at that single point fixes both the productaanvraag processing and the zaak detail retrieval.

The email fallback in `ProductaanvraagEmailService.sendConfirmationOfReceiptEmailFromProductaanvraag` already handles `productaanvraagSpecificEmailAddress == null` by calling `extractBetrokkeneEmail(betrokkene)`, which delegates to `fetchEmailForNatuurlijkPersoon` (BSN path) or `fetchEmail` (KVK path). Those fetch from partij digital addresses, which is exactly where the saved address lives in scenario 1.a.i.

## Goals / Non-Goals

**Goals:**
- Filter out partij-linked digital addresses from betrokkene contact-detail lookups
- Ensure scenario 1.a.i yields null `zaakSpecificContactDetails` in the zaak detail response
- Ensure the confirmation email is still sent via the partij address fallback in scenario 1.a.i
- Add integration test coverage for scenario 1.a.i in `NotificationProductaanvraagCmmnTest`
- Leave scenarios 1.a.ii.1, 1.b (new profile), anonymous, KVK/vestiging unchanged

**Non-Goals:**
- Changing the BPMN path (`NotificationProductaanvraagBpmnTest`) — same `KlantClientService` method is used, so it benefits automatically, but no new BPMN itest is added in this change
- Changing how `findDigitalAddressesForNaturalPerson` / `findDigitalAddressesForVestiging` / `findDigitalAddressesForNonNaturalPerson` work
- Any API contract change

## Decisions

### Filter `verstrektDoorPartij == null` inside `findContactDetailsForKlantcontact`

Apply the filter at the single shared extraction point rather than in each caller (`findZaakSpecificContactDetails`, `findProductaanvraagSpecificContactDetails`). This avoids duplication and guarantees consistent behaviour.

```kotlin
// before
expandedBetrokkene?.digitaleAdressen?.toContactDetails()

// after
expandedBetrokkene?.digitaleAdressen
    ?.filter { it.verstrektDoorPartij == null }
    ?.takeIf { it.isNotEmpty() }
    ?.toContactDetails()
```

`takeIf { isNotEmpty() }` ensures we return `null` rather than `ContactDetails(null, null)` when all addresses are partij-linked, keeping the calling code's null-check semantics consistent.

Alternative considered: filtering in each caller. Rejected — two callers, same semantic rule, higher maintenance risk.

### New itest data: klantcontact 5, betrokkene 6, digital address 12, onderwerpobject 3

Add the minimum rows needed to simulate scenario 1.a.i in the OpenKlant database. Reuse persoon partij 1 (BSN 999993896) since that represents the "profile already exists" case. Digital address 12 has both `betrokkene_id = 6` and `partij_id = 1` to trigger the bug path.

### Test asserts `zaakSpecificContactDetails` is null or absent

Use `isNull("zaakSpecificContactDetails")` (JSONObject API) so the test is robust to both JSON `null` and key absence, matching how `RestZaakConverter` serialises a null Kotlin field.

## Risks / Trade-offs

- **Scenarios where address is legitimately on both betrokkene and partij but intended as request-specific** → not possible per Open Klant's documented contract; "Sla mijn gegevens op" is the only operation that creates both links simultaneously, and it means "save as my default", not "use only for this request".
- **`toContactDetails()` on an empty list** → guarded by `takeIf { isNotEmpty() }` returning null before reaching `toContactDetails`.
- **Temporary `LOG.info` in `findContactDetailsForKlantcontact`** → the comment says it is temporary; it can be removed as part of this change since we now understand the root cause.
