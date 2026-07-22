## 1. Fix KlantClientService.findRequestSpecificContactDetailsForKlantcontact

- [x] 1.1 In `src/main/kotlin/nl/info/client/klant/KlantClientService.kt`, inside `findRequestSpecificContactDetailsForKlantcontact`, change the digital address filtering from `verstrektDoorPartij == null` to `isStandaardAdres == false`:
  ```kotlin
  expandedBetrokkene?.digitaleAdressen
      ?.let { digitaleAdressen ->
          val nonPreferredDigitalAddresses = digitaleAdressen.filter { it.isStandaardAdres == false }
          if (nonPreferredDigitalAddresses.isEmpty()) null
          else nonPreferredDigitalAddresses.toContactDetails()
      }
  ```
  Remove the `wasPartij` check — a betrokkene linked to a partij can still have non-preferred request-specific addresses.
- [ ] 1.2 Remove the temporary `LOG.info` on line 186 (`"Expanded betrokkene for betrokkene with UUID ..."`) — retained pending verification on TEST environment.

## 2. Update unit tests in KlantClientServiceTest

- [x] 2.1 In `src/test/kotlin/nl/info/client/klant/KlantClientServiceTest.kt`, update existing test scenarios in both `findProductaanvraagSpecificContactDetails` and `findZaakSpecificContactDetails` contexts:
  - Happy-path tests (email+phone, only email, only phone, multiple same type): add `isStandaardAdres = false` to all address fixtures so they pass the new filter.
  - "No digital addresses" scenario: change assertion from non-null `ContactDetails(null, null)` to `result.shouldBeNull()`, since an empty filtered list now returns null.
  - "Betrokkene linked to partij" scenario: replace "wasPartij → null" with "wasPartij + non-preferred address → returns address", documenting that `wasPartij` is no longer a blocking condition.
  - "All addresses preferred" scenario: replace `verstrektDoorPartij` with `isStandaardAdres = true` to correctly represent the "saved preference" case.
- [x] 2.2 Add new `Given` blocks covering:
  - Mix of `isStandaardAdres = true` and `isStandaardAdres = false` addresses → only non-preferred returned (for both productaanvraag and zaak contexts).
  - Betrokkene with `initiator = false` → null (both contexts).
  - Betrokkene with `initiator = null` → null (both contexts).
  - Multiple betrokkenen where first is not initiator klant, second is → returns second's contact details (both contexts).
  - Multiple betrokkenen where first throws `NotFoundException`, second is valid initiator klant → iteration continues, returns second's contact details (both contexts).

## 3. Add integration test data for scenario 1.a.i

- [x] 3.1 In `scripts/docker-compose/imports/openklant-database/database/1-setup-applicatie.sql`, add:
  - **Klantcontact 5**: productaanvraag klantcontact for the saved-preferred-address scenario.
  - **Betrokkene 8**: linked to klantcontact 5 and partij 4 (BSN 999992958 / Anita van Buren).
  - **Digital address 12**: email `anita.van.buren@example.com`, `betrokkene_id = 8`, `partij_id = 4`, `is_standaard_adres = true` (simulates a saved preferred address that should not be treated as aanvraag-specific).
  - **Onderwerpobject 3**: links klantcontact 5 to `testFormulierKenmerkSavedPreferredAddress`.
  - Updated `ALTER SEQUENCE` block for new IDs.
- [x] 3.2 Add a new productaanvraag Objecten JSON fixture (`OBJECT_PRODUCTAANVRAAG_5_UUID`) in `../../../../scripts/docker-compose/imports/open-object/fixtures/demodata.json` with:
  - `bron.kenmerk` = `"testFormulierKenmerkSavedPreferredAddress"`
  - `initiator.value` = BSN `"999992958"` (Anita van Buren / partij 4)
  - `start_at` = `"1977-01-01"` → zaak identification `"ZAAK-1977-0000000001"`

## 4. Add itest constants

- [x] 4.1 In `src/itest/kotlin/nl/info/zac/itest/config/ItestConfiguration.kt`, add:
  - `OBJECT_PRODUCTAANVRAAG_5_UUID` — UUID of the new Objecten object
  - `OBJECT_PRODUCTAANVRAAG_5_BRON_KENMERK = "testFormulierKenmerkSavedPreferredAddress"`
  - `ZAAK_PRODUCTAANVRAAG_5_IDENTIFICATION = "ZAAK-1977-0000000001"`
  - `ZAAK_PRODUCTAANVRAAG_5_OMSCHRIJVING`, `ZAAK_PRODUCTAANVRAAG_5_TOELICHTING` — from the fixture
  - `TEST_PERSON_ANITA_VAN_BUREN_BSN = "999992958"` and `TEST_PERSON_ANITA_VAN_BUREN_EMAIL = "anita.van.buren@example.com"` — used to verify the fallback email is sent to the partij's saved preferred address

## 5. Add integration test scenario in NotificationProductaanvraagCmmnTest

- [x] 5.1 In `src/itest/kotlin/nl/info/zac/itest/NotificationProductaanvraagCmmnTest.kt`, add a new `Context/Given/When/Then` block for scenario 1.a.i (BSN initiator with saved preferred email address):
  - `Given`: productaanvraag 5 object exists in Objecten; Open Klant has klantcontact 5 whose betrokkene 8 has digital address 12 (`is_standaard_adres = true`) linked to partij 4.
  - `When`: an Objecten notification is sent to ZAC triggering productaanvraag processing.
  - `Then`:
    - Response is `204 No Content`.
    - The created zaak has `zaakSpecificContactDetails` null — asserted with `isNull("zaakSpecificContactDetails") shouldBe true`.
    - The confirmation email is sent to `TEST_PERSON_ANITA_VAN_BUREN_EMAIL` (the partij's saved preferred address), verified via GreenMail.
