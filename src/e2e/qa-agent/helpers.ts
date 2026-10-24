/*
 * SPDX-FileCopyrightText: 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

import { expect, Page } from "@playwright/test";

export type TestUser = "bob" | "oscar";

const credentials: Record<TestUser, { username?: string; password?: string }> =
  {
    bob: {
      username: process.env.E2E_TEST_USER_1_USERNAME,
      password: process.env.E2E_TEST_USER_1_PASSWORD,
    },
    oscar: {
      username: process.env.E2E_TEST_USER_2_USERNAME,
      password: process.env.E2E_TEST_USER_2_PASSWORD,
    },
  };

export async function login(page: Page, user: TestUser = "bob") {
  const { username, password } = credentials[user];
  if (!username || !password) {
    throw new Error(
      `No credentials for test user '${user}' in the environment`,
    );
  }

  await page.goto("/");
  await page.getByLabel("Username or email").fill(username);
  await page.getByLabel("Password", { exact: true }).fill(password);
  await page.getByRole("button", { name: "Sign In" }).click();
  await expect(page.getByText("account_circle").first()).toBeVisible({
    timeout: 30_000,
  });
}

export const TEST_PERSON_BSN = "999993896";

/**
 * Creates a zaak for test person Héndrika Janse, assigned to "Test groep A", and returns its zaaknummer.
 * The omschrijving starts with "QA Agent", so the zaak is recognisable on the shared environment.
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
  await page.getByLabel("BSN").fill(TEST_PERSON_BSN);
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
  await page
    .getByLabel("Toelichting")
    .fill(`CREATED BY: QA Agent run ${process.env.GITHUB_RUN_ID ?? "local"}`);
  await page.getByRole("button", { name: "Aanmaken" }).click();

  await page.waitForURL(/\/zaken\/ZAAK-\d{4}-\d+/, { timeout: 40_000 });
  const [zaaknummer] = page.url().match(/ZAAK-\d{4}-\d+/) ?? [];
  if (!zaaknummer) throw new Error(`No zaaknummer in url ${page.url()}`);
  return zaaknummer;
}
