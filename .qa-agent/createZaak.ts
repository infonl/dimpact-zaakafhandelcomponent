/*
 * SPDX-FileCopyrightText: 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

import { expect, type Page } from "@playwright/test";
import fs from "fs";
import path from "path";

// globalTeardown.ts aborts the zaken listed here, also when the search index doesn't show them yet.
export const CREATED_ZAKEN_FILE = path.join("output", "created-zaken.txt");

/**
 * Creates a zaak for test person Héndrika Janse, assigned to "Test groep A" (for BPMN also to E2etest User1),
 * and returns its zaaknummer. The omschrijving starts with "QA Agent:", so globalTeardown.ts aborts it after
 * the run. Takes up to a minute.
 */
export async function createZaak(
  page: Page,
  {
    type = "CMMN",
    omschrijving,
  }: { type?: "CMMN" | "BPMN"; omschrijving: string },
) {
  const zaaktypeName =
    type === "BPMN"
      ? "Zaaktype voor BPMN e2e testen"
      : "Zaaktype voor e2e testen";

  await page.goto("/");
  await page.getByLabel("Zaak toevoegen").click();
  // The panel stays closed when it is clicked before the zaaktypes have loaded.
  await expect(async () => {
    await page.getByRole("combobox", { name: "Zaaktype" }).click();
    await page
      .getByRole("option", { name: zaaktypeName })
      .click({ timeout: 5_000 });
  }).toPass({ timeout: 40_000 });

  await page
    .locator("div")
    .filter({ hasText: /^person$/ })
    .click();
  await page.getByLabel("BSN").fill("999993896");
  await page
    .getByLabel("emoji_people Persoon")
    .getByRole("button", { name: "Zoeken" })
    .click();
  await page.getByRole("button", { name: "Select" }).click();

  await page
    .locator("div")
    .filter({ hasText: /^gps_fixed$/ })
    .click();
  const addressSearch = page.getByPlaceholder(
    "Zoeken op adres, postcode of woonplaats",
  );
  await addressSearch.fill("1112gv");
  await addressSearch.press("Enter");
  await page
    .getByRole("row", { name: /1112GV/ })
    .first()
    .getByRole("button", { name: "Koppelen" })
    .click();
  await page
    .locator("mat-toolbar button mat-icon", { hasText: "close" })
    .click();

  await page
    .getByRole("combobox", { name: "Zaak toekennen aan groep" })
    .fill("Test groep A");
  await page.getByRole("option", { name: "Test groep A", exact: true }).click();
  if (type === "BPMN") {
    await page
      .getByRole("combobox", { name: "Zaak toekennen aan medewerker" })
      .fill("E2etest User1");
    await page
      .getByRole("option", { name: "E2etest User1", exact: true })
      .click();
  }

  await page.getByLabel("Communicatiekanaal").click();
  await page.getByRole("option", { name: " E-mail " }).click();
  await page.getByLabel("Omschrijving").fill(`QA Agent: ${omschrijving}`);
  await page.getByRole("button", { name: "Aanmaken" }).click();

  await page.waitForURL(/\/zaken\/ZAAK-\d{4}-\d+/, { timeout: 40_000 });
  const [zaaknummer] = page.url().match(/ZAAK-\d{4}-\d+/) ?? [];
  if (!zaaknummer) throw new Error(`No zaaknummer in url ${page.url()}`);
  fs.mkdirSync(path.dirname(CREATED_ZAKEN_FILE), { recursive: true });
  fs.appendFileSync(CREATED_ZAKEN_FILE, `${zaaknummer}\n`);
  return zaaknummer;
}
