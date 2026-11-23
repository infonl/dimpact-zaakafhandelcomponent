/*
 * SPDX-FileCopyrightText: 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

import { expect } from "@playwright/test";
import { Given, Then, When } from "../../fixtures";
import { confirmDialog, findDialog } from "../../screens/dialog";
import { afgehandeldeZaken } from "../../screens/werkvoorraad";
import { zaakPage } from "../../screens/zaak";
import { dataOf } from "../../support/zac-api";
import { SEED } from "../seed";

Given(
  "the zaak has a document {string} of type {string}",
  async ({ testData }, titel: string, informatieobjecttype: string) => {
    await testData.addDocument(testData.zaak, { titel, informatieobjecttype });
  },
);

Given(
  "a closed zaak with the resultaat {string}",
  async ({ testData }, resultaat: string) => {
    const { naam } = await testData.groepOf("behandelaar");
    const zaak = await testData.createZaak({
      zaaktype: SEED.cmmnZaaktype,
      groep: naam,
      omschrijving: "gesloten zaak",
    });
    await testData.finishIntake(zaak);
    await testData.closeZaak(zaak, resultaat);
  },
);

When(
  "I record a besluit and link the document {string}",
  async ({ page, testData }, titel: string) => {
    await zaakPage(page).chooseFromMenu("Besluit vastleggen");
    await page.getByRole("combobox", { name: "Besluit" }).click();
    await page.getByRole("option", { name: SEED.besluittype }).click();
    // The panel has no accessible name, and the documents of the zaak stay in the page behind it, inert.
    await page
      .getByRole("group")
      .filter({ has: page.getByRole("columnheader", { name: "Documenttype" }) })
      .getByRole("row", { name: testData.documentTitled(titel) })
      .getByRole("checkbox")
      .check();
    await page.getByRole("button", { name: "Aanmaken" }).click();
    await expect(
      page.getByRole("heading", { name: "Besluit vastleggen" }),
    ).toBeHidden();
  },
);

Then(
  "the zaak shows the besluit with the document {string}",
  async ({ page, testData }, titel: string) => {
    const besluit = page
      .getByRole("region")
      .filter({ hasText: SEED.besluittype });
    await expect(
      besluit.getByRole("row", { name: testData.documentTitled(titel) }),
    ).toBeVisible();
  },
);

When(
  "I close the zaak with the resultaat {string}",
  async ({ page }, resultaat: string) => {
    const dialog = await zaakPage(page).afhandelen(resultaat);
    await expect(dialog).toBeHidden();
  },
);

Then(
  "the zaak appears in the list of afgehandelde zaken",
  async ({ page, testData }) => {
    const { identificatie } = testData.zaak;
    await testData.untilZaakIsIndexed(identificatie, "afgesloten");
    const lijst = afgehandeldeZaken(page);
    await lijst.open();
    await lijst.expectZaak(identificatie);
  },
);

When(
  "I reopen the zaak with the reason {string}",
  async ({ page }, reden: string) => {
    await zaakPage(page).chooseFromMenu("Zaak heropenen");
    const dialog = findDialog(page, "Zaak heropenen");
    await dialog
      .getByRole("textbox", { name: "Reden waarom zaak wordt heropend" })
      .fill(reden);
    await confirmDialog(dialog, "Zaak heropenen");
  },
);

// The zaak page does not show the resultaat, so the step reads it from the REST API.
Then("the zaak has no resultaat", async ({ apiAs, testData }) => {
  const api = await apiAs("recordmanager");
  const { identificatie } = testData.zaak;
  const zaak = await dataOf(
    api.GET("/rest/zaken/zaak/id/{identificatie}", {
      params: { path: { identificatie } },
    }),
  );
  expect(zaak.resultaat).toBeUndefined();
});

Then(
  "the zaak is gone from the list of afgehandelde zaken",
  async ({ page, testData }) => {
    const { identificatie } = testData.zaak;
    await testData.untilZaakIsIndexed(identificatie, "open");
    const lijst = afgehandeldeZaken(page);
    await lijst.open();
    await lijst.expectNoZaak(identificatie);
  },
);
