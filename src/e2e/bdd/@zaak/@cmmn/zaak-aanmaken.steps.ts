/*
 * SPDX-FileCopyrightText: 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

import { expect, type Page } from "@playwright/test";
import { ENV } from "../../../config/env";
import { Given, Then, When } from "../../fixtures";
import { zakenWerkvoorraad } from "../../screens/werkvoorraad";
import { zaakPage } from "../../screens/zaak";

async function openInitiatorPanel(page: Page) {
  await page.getByRole("button", { name: "Initiator koppelen" }).click();
}

Given(
  "the zaaktype {seeded} is set up and assigns new zaken to groep {seeded}",
  async ({ testData }, zaaktype: string, groep: string) => {
    await testData.ensureZaaktype(zaaktype, { groep });
  },
);

When(
  "I search the persoon with BSN {string} and select {string} as initiator",
  async ({ page }, bsn: string, naam: string) => {
    await openInitiatorPanel(page);
    const personen = page.getByRole("tabpanel", { name: "Persoon" });
    await personen.getByRole("textbox", { name: "BSN" }).fill(bsn);
    await personen.getByRole("button", { name: "Zoeken" }).click();
    await personen
      .getByRole("row", { name: naam })
      .getByRole("button", { name: "Selecteren" })
      .click();
  },
);

When(
  "I search the bedrijf with KvK {seeded} and select {seeded} as initiator",
  async ({ page }, kvkNummer: string, naam: string) => {
    await openInitiatorPanel(page);
    await page.getByRole("tab", { name: "Bedrijf" }).click();
    const bedrijven = page.getByRole("tabpanel", { name: "Bedrijf" });
    await bedrijven
      .getByRole("textbox", { name: "KvK-nummer" })
      .fill(kvkNummer);
    // The KVK test double of the kind stack only answers a search for a rechtspersoon.
    await bedrijven.getByRole("combobox", { name: "Type" }).click();
    await page.getByRole("option", { name: "Rechtspersoon" }).click();
    await bedrijven.getByRole("button", { name: "Zoeken" }).click();
    await bedrijven
      .getByRole("row", { name: naam })
      .getByRole("button", { name: "Selecteren" })
      .click();
  },
);

When(
  "I search an address in the BAG and add it as BAG object",
  async ({ page }) => {
    const { bag } = ENV;
    await page.getByRole("button", { name: "BAG object koppelen" }).click();
    const bagSearchText = page.getByRole("textbox", { name: "BAG objecten" });
    await bagSearchText.fill(bag.searchText);
    await bagSearchText.press("Enter");
    await page
      .getByRole("row", { name: bag.adres })
      .getByRole("button", { name: "Koppelen" })
      .click();
    // The click disables the button, so the focus leaves the panel, and the panel only closes on an Escape that
    // happens inside it. Its close button has no accessible name.
    await bagSearchText.press("Escape");
    await expect(bagSearchText).toBeHidden();
  },
);

Then("the zaak shows {seeded} as initiator", async ({ page }, naam: string) => {
  await zaakPage(page).expectInitiator(naam);
});

Then("the zaak shows that address as BAG object", async ({ page }) => {
  await zaakPage(page).expectBagObject(ENV.bag.adres);
});

Then(
  "the zaak is assigned to groep {seeded}",
  async ({ page }, groep: string) => {
    await zaakPage(page).expectGegeven("Groep", groep);
  },
);

Then(
  "the zaak shows that no ontvangstbevestiging is sent",
  async ({ page }) => {
    await zaakPage(page).expectNoOntvangstbevestiging();
  },
);

Then(
  "the zaak appears in the zaken werkvoorraad",
  async ({ page, testData }) => {
    const { identificatie } = testData.zaak;
    await testData.untilZaakIsIndexed(identificatie);
    const werkvoorraad = zakenWerkvoorraad(page);
    await werkvoorraad.open();
    await werkvoorraad.expectZaak(identificatie);
  },
);
