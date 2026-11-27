/*
 * SPDX-FileCopyrightText: 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

import { expect } from "@playwright/test";
import { Given, Then, When } from "../../fixtures";
import { documentPage } from "../../screens/document";
import { chooseFromAutocomplete } from "../../screens/select";
import { zaakPage } from "../../screens/zaak";
import { SEED } from "../seed";

const SJABLOONGROEP = "Dimpact";
const SJABLOON = "Data Test";
const BESCHRIJVING = "E2E beschrijving";
const AUTEUR = "E2E auteur";
const DOCUMENT_TIMEOUT_MILLISECONDS = 90_000;

Given("a zaak in intake", async ({ testData }) => {
  const { naam } = await testData.groepOf("behandelaar");
  await testData.createZaak({
    zaaktype: SEED.cmmnZaaktype,
    groep: naam,
    omschrijving: "zaak in intake",
  });
});

When(
  "I start the SmartDocuments wizard for the document {string}",
  async ({ page, testData }, titel: string) => {
    await zaakPage(page).chooseFromMenu("Document maken");
    await chooseFromAutocomplete(page, "Sjabloongroep", SJABLOONGROEP);
    await chooseFromAutocomplete(page, "Sjabloon", SJABLOON);
    const uniqueTitel = testData.unique(titel);
    await page.getByRole("textbox", { name: "Titel" }).fill(uniqueTitel);
    await page
      .getByRole("textbox", { name: "Beschrijving" })
      .fill(BESCHRIJVING);
    await page.getByRole("textbox", { name: "Auteur" }).fill(AUTEUR);
    testData.rememberDocument(titel, uniqueTitel);
  },
);

When("I complete the SmartDocuments wizard", async ({ page }) => {
  const [wizard] = await Promise.all([
    page.waitForEvent("popup"),
    page.getByRole("button", { name: "Toevoegen", exact: true }).click(),
  ]);
  await wizard.getByRole("button", { name: /Klaar/i }).click();
  await expect(
    wizard.getByRole("status").filter({ hasText: "succes" }),
  ).toBeVisible();
});

Then(
  "the zaak lists the document {string}",
  async ({ page, testData }, titel: string) => {
    await expect(
      zaakPage(page).document(testData.documentTitled(titel)),
    ).toBeVisible({ timeout: DOCUMENT_TIMEOUT_MILLISECONDS });
  },
);

Then(
  "the document {string} shows the titel, the beschrijving and the auteur that I entered",
  async ({ page, testData }, titel: string) => {
    const uniqueTitel = testData.documentTitled(titel);
    await zaakPage(page)
      .document(uniqueTitel)
      .getByRole("link", { name: "Document bekijken" })
      .click();
    const document = documentPage(page);
    await document.expectOpened(uniqueTitel);
    await document.expectGegeven("Titel", uniqueTitel);
    await document.expectGegeven("Beschrijving", BESCHRIJVING);
    await document.expectGegeven("Auteur", AUTEUR);
  },
);
