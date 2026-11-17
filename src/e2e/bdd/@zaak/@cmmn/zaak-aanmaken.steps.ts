/*
 * SPDX-FileCopyrightText: 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

import { expect, type Page, type TestInfo } from "@playwright/test";
import { ENV } from "../../../config/env";
import { Given, Then, When } from "../../fixtures";
import { zakenWerkvoorraad } from "../../screens/werkvoorraad";
import { zaakPage } from "../../screens/zaak";

function bagTestDoubleOrSkip(testInfo: TestInfo) {
  testInfo.skip(
    !ENV.testDoubles,
    `The target "${ENV.target}" has no BAG test double.`,
  );
  // skip() ends the test when the target has no test doubles, so they exist from here on.
  return ENV.testDoubles!.bag;
}

async function openInitiatorPanel(page: Page) {
  // eslint-disable-next-line playwright/no-raw-locators -- the button has no accessible name until the fix that names it "Initiator koppelen" is on main
  const initiatorField = page
    .locator("mat-form-field")
    .filter({ hasText: "Initiator" });
  await initiatorField.getByRole("button").click();
}

Given(
  "the zaaktype {string} is set up and assigns new zaken to groep {string}",
  async ({ testData }, zaaktype: string, groep: string) => {
    await testData.ensureZaaktype(zaaktype, { groep });
  },
);

When("I open the form to create a zaak", async ({ page }) => {
  await page.goto("/");
  await page.getByRole("button", { name: "Zaak toevoegen" }).click();
  await expect(
    page.getByRole("heading", { name: "Zaak aanmaken", level: 1 }),
  ).toBeVisible();
});

When("I choose the zaaktype {string}", async ({ page }, zaaktype: string) => {
  await page.getByRole("combobox", { name: "Zaaktype" }).click();
  await page.getByRole("option", { name: zaaktype, exact: true }).click();
});

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
  "I search the bedrijf with KvK {string} and select {string} as initiator",
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
  "I search an address from the BAG stub and add it as BAG object",
  async ({ page, $testInfo }) => {
    const bag = bagTestDoubleOrSkip($testInfo);
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

When(
  "I choose the communicatiekanaal {string}",
  async ({ page }, communicatiekanaal: string) => {
    await page.getByRole("combobox", { name: "Communicatiekanaal" }).click();
    await page
      .getByRole("option", { name: communicatiekanaal, exact: true })
      .click();
  },
);

When(
  "I enter the omschrijving {string}",
  async ({ page, testData }, omschrijving: string) => {
    await page
      .getByRole("textbox", { name: "Omschrijving" })
      .fill(testData.unique(omschrijving));
  },
);

When("I create the zaak", async ({ page, testData }) => {
  await page.getByRole("button", { name: "Aanmaken" }).click();
  await expect(page).toHaveURL(/\/zaken\/ZAAK-[\d-]+$/);
  const identificatie = new URL(page.url()).pathname.split("/").pop();
  if (!identificatie) throw new Error(`${page.url()} names no zaak.`);
  testData.rememberZaak({ identificatie });
});

Then(
  "the zaak opens with status {string}",
  async ({ page, testData }, status: string) => {
    const zaak = zaakPage(page);
    await zaak.expectOpened(testData.zaak.identificatie);
    await zaak.expectGegeven("Status", status);
  },
);

Then("the zaak shows {string} as initiator", async ({ page }, naam: string) => {
  await zaakPage(page).expectInitiator(naam);
});

Then(
  "the zaak shows that address as BAG object",
  async ({ page, $testInfo }) => {
    await zaakPage(page).expectBagObject(bagTestDoubleOrSkip($testInfo).adres);
  },
);

Then(
  "the zaak is assigned to groep {string}",
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
