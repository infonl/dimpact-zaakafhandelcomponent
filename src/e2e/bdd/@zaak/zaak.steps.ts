/*
 * SPDX-FileCopyrightText: 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

import { expect } from "@playwright/test";
import { Given, Then, When } from "../fixtures";
import { zaakPage } from "../screens/zaak";
import type { TestData } from "../support/test-data";
import { SEED, seededPersoonOrSkip } from "./seed";

Given(
  "a zaak in intake with a person as initiator",
  async ({ testData, $testInfo }) => {
    const { bsn } = seededPersoonOrSkip($testInfo);
    const { naam } = await testData.groepOf("behandelaar");
    await testData.createZaak({
      zaaktype: SEED.cmmnZaaktype,
      groep: naam,
      omschrijving: "zaak in intake",
      initiator: { bsn },
    });
  },
);

async function createZaakInBehandeling(
  testData: TestData,
  zaaktype: string,
  omschrijving: string,
) {
  const { naam } = await testData.groepOf("behandelaar");
  const zaak = await testData.createZaak({
    zaaktype,
    groep: naam,
    omschrijving,
  });
  await testData.finishIntake(zaak);
  return zaak;
}

Given("a zaak in behandeling", async ({ testData }) => {
  await createZaakInBehandeling(
    testData,
    SEED.cmmnZaaktype,
    "zaak in behandeling",
  );
});

Given(
  "a zaak {string} in behandeling",
  async ({ testData }, omschrijving: string) => {
    await createZaakInBehandeling(testData, SEED.cmmnZaaktype, omschrijving);
  },
);

Given(
  "a zaak {string} in behandeling, of a zaaktype that the hoofdzaak allows as deelzaak",
  async ({ testData }, omschrijving: string) => {
    await createZaakInBehandeling(
      testData,
      SEED.cmmnDeelzaaktype,
      omschrijving,
    );
  },
);

When("I open the zaak", async ({ page, testData }) => {
  await zaakPage(page).open(testData.zaak.identificatie);
});

When(
  "I open the zaak {string}",
  async ({ page, testData }, omschrijving: string) => {
    await zaakPage(page).open(
      testData.zaakDescribed(omschrijving).identificatie,
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

When("I choose the zaaktype {seeded}", async ({ page }, zaaktype: string) => {
  await page.getByRole("combobox", { name: "Zaaktype" }).click();
  await page.getByRole("option", { name: zaaktype, exact: true }).click();
});

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

Then(
  "the zaak lists the task {string} as afgerond",
  async ({ page, testData }, taak: string) => {
    const zaak = zaakPage(page);
    await zaak.open(testData.zaak.identificatie);
    await page.getByRole("switch", { name: "Toon afgeronde taken" }).check();
    await expect(zaak.taak(taak)).toContainText("Afgerond");
  },
);

Then("the status is {string}", async ({ page }, status: string) => {
  await zaakPage(page).expectGegeven("Status", status);
});
