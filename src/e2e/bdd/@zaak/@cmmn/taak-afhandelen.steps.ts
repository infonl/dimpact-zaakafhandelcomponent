/*
 * SPDX-FileCopyrightText: 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

import { expect, type Page } from "@playwright/test";
import { Then, When } from "../../fixtures";
import { zaakPage } from "../../screens/zaak";

function mijnTaken(page: Page) {
  return {
    /** Opens the takenlijst and waits until it has the answer of the search, so a missing taak is really missing. */
    async open() {
      await Promise.all([
        page.waitForResponse(
          (response) =>
            new URL(response.url()).pathname.endsWith("/rest/zoeken/list") &&
            response.ok(),
        ),
        page.goto("/taken/mijn"),
      ]);
    },

    taak(naam: string, zaakIdentificatie: string) {
      return page
        .getByRole("row")
        .filter({ hasText: naam })
        .filter({ hasText: zaakIdentificatie });
    },
  };
}

When(
  "I start the task {string} and assign it to myself",
  async ({ page, accountOfRole, testData }, taak: string) => {
    const zaak = zaakPage(page);
    await zaak.chooseFromMenu(taak);
    await page
      .getByRole("textbox", { name: "Vraag" })
      .fill(testData.unique("Mag dit door?"));
    const { naam } = await testData.groepOf("behandelaar");
    await page
      .getByRole("combobox", { name: "Taak toekennen aan groep" })
      .click();
    await page.getByRole("option", { name: naam }).click();
    await page
      .getByRole("combobox", { name: "Taak toekennen aan medewerker" })
      .click();
    await page
      .getByRole("option", { name: accountOfRole("behandelaar").displayName })
      .click();
    await page.getByRole("button", { name: "Starten" }).click();
    await expect(zaak.taak(taak)).toBeVisible();
  },
);

Then(
  "the task {string} appears in my takenlijst",
  async ({ page, testData }, taak: string) => {
    await testData.untilTaakIsIndexed(testData.zaak.identificatie, taak);
    const takenlijst = mijnTaken(page);
    await takenlijst.open();
    await expect(
      takenlijst.taak(taak, testData.zaak.identificatie),
    ).toBeVisible();
  },
);

When(
  "I open the task {string} from my takenlijst",
  async ({ page, testData }, taak: string) => {
    await mijnTaken(page)
      .taak(taak, testData.zaak.identificatie)
      .getByRole("link", { name: "Taak bekijken" })
      .click();
    await expect(page.getByRole("heading", { name: taak })).toBeVisible();
  },
);

When(
  "I complete the task with the decision {string} and the toelichting {string}",
  async ({ page }, beslissing: string, toelichting: string) => {
    await page.getByRole("radio", { name: beslissing, exact: true }).check();
    await page.getByRole("textbox", { name: "Toelichting" }).fill(toelichting);
    await page.getByRole("button", { name: "Opslaan & afronden" }).click();
    // A completed task returns to the list that opened it.
    await expect(
      page.getByRole("heading", { name: "Mijn taken", level: 1 }),
    ).toBeVisible();
  },
);

Then(
  "the task {string} is gone from my takenlijst",
  async ({ page, testData }, taak: string) => {
    const takenlijst = mijnTaken(page);
    await takenlijst.open();
    await expect(
      takenlijst.taak(taak, testData.zaak.identificatie),
    ).toBeHidden();
  },
);
