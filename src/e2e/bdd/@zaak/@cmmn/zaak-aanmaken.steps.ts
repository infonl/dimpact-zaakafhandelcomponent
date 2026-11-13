/*
 * SPDX-FileCopyrightText: 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

import { expect, type Page } from "@playwright/test";
import { Given, Then, When } from "../../_core/fixture";
import { setUpZaaktype } from "../../support/zaakafhandelparameters";

// The BAG test double of the kind stack, in scripts/kind/seed/bag-wiremock, only answers this search text.
const BAG_STUB = {
  searchText: "fake search text",
  adres: "Dam 1, 1012JS Amsterdam",
};

function required<Value>(value: Value | undefined, description: string) {
  if (value === undefined) {
    throw new Error(`This scenario has no ${description} yet.`);
  }
  return value;
}

async function openInitiatorPanel(page: Page) {
  // The button that opens the panel has no accessible name, so the form field that holds it finds it.
  await page
    .locator("mat-form-field")
    .filter({ hasText: "Initiator" })
    .getByRole("button")
    .click();
}

async function gegevensOfZaak(page: Page) {
  await page.getByRole("tab", { name: "Gegevens" }).click();
  return page.getByRole("tabpanel", { name: "Gegevens" });
}

Given(
  "the zaaktype {string} is set up and assigns new zaken to groep {string}",
  async (
    { apiAs, whileNoOtherWorkerDoes },
    zaaktype: string,
    groep: string,
  ) => {
    const api = await apiAs("beheerder");
    await whileNoOtherWorkerDoes(`set up the zaaktype ${zaaktype}`, () =>
      setUpZaaktype(api, zaaktype, groep),
    );
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
  async ({ page, zaakOfScenario }) => {
    await page.getByRole("button", { name: "BAG object koppelen" }).click();
    const bagSearch = page.locator("zac-bag-zoek");
    await bagSearch
      .getByRole("textbox", { name: "BAG objecten" })
      .fill(BAG_STUB.searchText);
    await bagSearch
      .getByRole("button", { name: "Zoeken", exact: true })
      .click();
    await bagSearch
      .getByRole("row", { name: BAG_STUB.adres })
      .getByRole("button", { name: "Koppelen" })
      .click();
    await bagSearch.getByRole("button", { name: "Annuleren" }).click();
    zaakOfScenario.bagAdres = BAG_STUB.adres;
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
  async ({ page }, omschrijving: string) => {
    await page
      .getByRole("textbox", { name: "Omschrijving" })
      .fill(omschrijving);
  },
);

When("I create the zaak", async ({ page, zaakOfScenario }) => {
  await page.getByRole("button", { name: "Aanmaken" }).click();
  await expect(page).toHaveURL(/\/zaken\/ZAAK-[\d-]+$/);
  zaakOfScenario.identificatie = new URL(page.url()).pathname.split("/").pop();
});

Then(
  "the zaak opens with status {string}",
  async ({ page, zaakOfScenario }, status: string) => {
    const identificatie = required(zaakOfScenario.identificatie, "zaak");
    await expect(
      page.getByRole("heading", { name: identificatie, level: 1 }),
    ).toBeVisible();
    await expect(await gegevensOfZaak(page)).toContainText(
      new RegExp(`Status\\s*${status}`),
    );
  },
);

Then("the zaak shows {string} as initiator", async ({ page }, naam: string) => {
  await expect(
    page.getByRole("button", { name: `Initiator ${naam}` }),
  ).toBeVisible();
});

Then(
  "the zaak shows that address as BAG object",
  async ({ page, zaakOfScenario }) => {
    const adres = required(zaakOfScenario.bagAdres, "BAG object");
    await page.getByRole("tab", { name: "BAG objecten" }).click();
    await expect(
      page.getByRole("tabpanel", { name: "BAG objecten" }),
    ).toContainText(adres);
  },
);

Then(
  "the zaak is assigned to groep {string}",
  async ({ page }, groep: string) => {
    await expect(await gegevensOfZaak(page)).toContainText(
      new RegExp(`Groep\\s*${groep}`),
    );
  },
);

Then(
  "the zaak shows that no ontvangstbevestiging is sent",
  async ({ page }) => {
    await expect(
      page.getByRole("option", { name: "Geen bevestiging verstuurd" }),
    ).toBeVisible();
  },
);

Then(
  "the zaak appears in the zaken werkvoorraad",
  async ({ page, zaakOfScenario }) => {
    const identificatie = required(zaakOfScenario.identificatie, "zaak");
    await page.goto("/zaken/werkvoorraad");
    // The filter fields of the werkvoorraad have no label. The first one filters on the zaaknummer.
    const zaaknummerFilter = page
      .getByRole("table")
      .getByRole("textbox")
      .first();
    await zaaknummerFilter.fill(identificatie);
    await zaaknummerFilter.press("Enter");

    // The search index gets the zaak a moment after ZAC creates it.
    await expect(async () => {
      await page.reload();
      await expect(page.getByRole("cell", { name: identificatie })).toBeVisible(
        {
          timeout: 3_000,
        },
      );
    }).toPass({ timeout: 60_000 });
  },
);
