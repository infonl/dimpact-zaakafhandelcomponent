## 1. Fix KlantClientService.findContactDetailsForKlantcontact

- [x] 1.1 In `src/main/kotlin/nl/info/client/klant/KlantClientService.kt`, inside `findContactDetailsForKlantcontact` (line 187), change:
  ```kotlin
  expandedBetrokkene?.digitaleAdressen?.toContactDetails()
  ```
  to:
  ```kotlin
  expandedBetrokkene?.digitaleAdressen
      ?.filter { it.verstrektDoorPartij == null }
      ?.takeIf { it.isNotEmpty() }
      ?.toContactDetails()
  ```
- [x] 1.2 Remove the temporary `LOG.info` on line 186 (`"Expanded betrokkene for betrokkene with UUID ..."`) — the root cause is now understood and fixed.

## 2. Update unit tests in KlantClientServiceTest

- [x] 2.1 In `src/test/kotlin/nl/info/client/klant/KlantClientServiceTest.kt`, inside the existing `findContactDetailsForKlantcontact` test context, add a `Given` block covering the case where the betrokkene's digital addresses all have `verstrektDoorPartij` set (non-null): mock `getBetrokkeneWithDigitaleAdressen` returning addresses with `verstrektDoorPartij` set, assert the function returns `null`.
- [x] 2.2 Verify (or add) a `Given` block covering the case where digital addresses have `verstrektDoorPartij == null` and assert contact details are returned — to document the happy path is preserved.

## 3. Add integration test data for scenario 1.a.i

- [x] 3.1 In `scripts/docker-compose/imports/openklant-database/database/1-setup-applicatie.sql`, add:
  - **Klantcontact 5**: productaanvraag klantcontact simulating profile-exists + address-changed-and-saved scenario.
  - **Betrokkene 6**: linked to klantcontact 5 and partij 1 (BSN 999993896).
  - **Digital address 12**: email, `betrokkene_id = 6`, `partij_id = 1` (simulates address saved to both betrokkene and partij per scenario 1.a.i).
  - **Onderwerpobject 3**: links klantcontact 5 to the formulierKenmerk for the new productaanvraag object (e.g. `"testFormulierKenmerkSavedPreferredAddress"`).
  - Update the `ALTER SEQUENCE` block at the bottom if any new sequence IDs are needed.
- [x] 3.2 Add a new productaanvraag Objecten JSON fixture (e.g. `OBJECT_PRODUCTAANVRAAG_5_UUID`) in `scripts/docker-compose/imports/objects-api/fixtures/demodata.json` with:
  - `bron.kenmerk` = `"testFormulierKenmerkSavedPreferredAddress"`
  - `initiator.value` = BSN `"999993896"` (same persoon as partij 1)
  - `start_at` = `"1977-01-01"` → zaak identification "ZAAK-1977-0000000001"

## 4. Add itest constants

- [x] 4.1 In `src/itest/kotlin/nl/info/zac/itest/config/ItestConfiguration.kt`, add:
  - `OBJECT_PRODUCTAANVRAAG_5_UUID` — UUID of the new Objecten object
  - `OBJECT_PRODUCTAANVRAAG_5_BRON_KENMERK = "testFormulierKenmerkSavedPreferredAddress"`
  - `ZAAK_PRODUCTAANVRAAG_5_IDENTIFICATION` — expected zaak identification
  - `ZAAK_PRODUCTAANVRAAG_5_OMSCHRIJVING`, `ZAAK_PRODUCTAANVRAAG_5_TOELICHTING` — from the fixture
  - `ZAAK_PRODUCTAANVRAAG_5_PARTIJ_EMAIL = "hendrika.janse@example.com"` — the partij's preferred email (address 1 in SQL), used to verify the fallback email send

## 5. Add integration test scenario in NotificationProductaanvraagCmmnTest

- [x] 5.1 In `src/itest/kotlin/nl/info/zac/itest/NotificationProductaanvraagCmmnTest.kt`, add a new `Given/When/Then` block (after the existing productaanvraag 4 scenario) for scenario 1.a.i:
  - `Given`: productaanvraag 5 object exists in Objecten; Open Klant has klantcontact 5 whose betrokkene has a digital address linked to both betrokkene and partij 1.
  - `When`: an Objecten notification is sent to ZAC triggering productaanvraag processing.
  - `Then`:
    - Response is `204 No Content`.
    - The created zaak (retrieved via `GET /zaken/zaak/id/$ZAAK_PRODUCTAANVRAAG_5_IDENTIFICATION`) has `zaakSpecificContactDetails` null/absent — assert with `isNull("zaakSpecificContactDetails")`.
    - The confirmation email is sent to `ZAAK_PRODUCTAANVRAAG_5_PARTIJ_EMAIL` (the partij fallback address), verifiable via GreenMail.
