/*
 * SPDX-FileCopyrightText: 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

import { expect, type Page } from "@playwright/test";
import { Then, When } from "../../fixtures";
import { confirmDialog, findDialog } from "../../screens/dialog";
import { zaakPage } from "../../screens/zaak";
import { SEED } from "../seed";

async function chooseResultaatToClose(page: Page) {
  await zaakPage(page).chooseFromMenu("Zaak afhandelen");
  const dialog = findDialog(page, "Zaak afhandelen");
  await dialog.getByRole("combobox", { name: "Resultaat" }).click();
  await page.getByRole("option", { name: SEED.resultaat }).click();
  await dialog.getByRole("button", { name: "Afhandelen" }).click();
  return dialog;
}

When(
  "I link the zaak {string} as deelzaak",
  async ({ page, testData }, omschrijving: string) => {
    const { identificatie } = testData.zaakDescribed(omschrijving);
    await testData.untilZaakIsIndexed(identificatie);
    await zaakPage(page).chooseFromMenu("Zaak koppelen");
    const panel = page.getByRole("region", { name: "Zaak koppelen" });
    await panel.getByRole("combobox", { name: "Soort koppeling" }).click();
    await page
      .getByRole("option", { name: "Hoofdzaak koppelen aan deelzaak" })
      .click();
    await panel
      .getByRole("textbox", { name: "Zaaknummer" })
      .fill(identificatie);
    await panel.getByRole("button", { name: "Zoeken" }).click();
    await panel
      .getByRole("row", { name: identificatie })
      .getByRole("button", { name: "Zaak koppelen" })
      .click();
    await expect(panel).toBeHidden();
  },
);

Then(
  "the zaak {string} shows {string} under {string}",
  async (
    { page, testData },
    zaak: string,
    gekoppeldeZaak: string,
    tab: string,
  ) => {
    const zaakpagina = zaakPage(page);
    await zaakpagina.open(testData.zaakDescribed(zaak).identificatie);
    await expect(
      (await zaakpagina.openTab(tab)).getByRole("row", {
        name: testData.zaakDescribed(gekoppeldeZaak).identificatie,
      }),
    ).toBeVisible();
  },
);

Then(
  "I cannot close the zaak {string} while {string} is open",
  async ({ page, testData }, hoofdzaak: string, deelzaak: string) => {
    const zaakpagina = zaakPage(page);
    await zaakpagina.open(testData.zaakDescribed(deelzaak).identificatie);
    await zaakpagina.expectGegeven("Status", "In behandeling");
    await zaakpagina.open(testData.zaakDescribed(hoofdzaak).identificatie);
    await chooseResultaatToClose(page);
    const fout = findDialog(page, "Er is een fout opgetreden");
    await expect(fout).toContainText(
      "Deze deelzaken moeten eerst afgesloten worden voordat de zaak afgesloten kan worden.",
    );
    await confirmDialog(fout, "Sluiten");
  },
);

async function closeZaak(page: Page, identificatie: string) {
  const zaakpagina = zaakPage(page);
  await zaakpagina.open(identificatie);
  const dialog = await chooseResultaatToClose(page);
  await expect(dialog).toBeHidden();
  await zaakpagina.expectGegeven("Status", "Afgerond");
}

When(
  "I close the zaak {string} with a resultaat",
  async ({ page, testData }, zaak: string) => {
    await closeZaak(page, testData.zaakDescribed(zaak).identificatie);
  },
);

Then(
  "I can close the zaak {string} with a resultaat",
  async ({ page, testData }, zaak: string) => {
    await closeZaak(page, testData.zaakDescribed(zaak).identificatie);
  },
);
