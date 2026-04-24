/*
 * SPDX-FileCopyrightText: 2025 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

import { Given, Then, When } from "@cucumber/cucumber";
import { expect, type Locator, type Page } from "@playwright/test";
import { z } from "zod";
import {
  FORTY_SECONDS_IN_MS,
  ONE_MINUTE_IN_MS,
  TWENTY_SECONDS_IN_MS,
  TWO_MINUTES_IN_MS,
  TWO_SECONDS_IN_MS,
} from "../support/time-constants";
import { CustomWorld } from "../support/worlds/world";
import { worldUsers, zaakResult, zaakStatus } from "../utils/schemes";

function formioForm(page: Page) {
  return page.locator("zac-formio-wrapper");
}

async function waitForFormioReady(page: Page) {
  const form = formioForm(page);
  await expect(form).toBeVisible({ timeout: FORTY_SECONDS_IN_MS });
  await form
    .locator(".formio-component")
    .first()
    .waitFor({ state: "visible", timeout: FORTY_SECONDS_IN_MS });
}

async function isOnBackendErrorPage(page: Page): Promise<boolean> {
  return page
    .getByText(/50[234] (Bad Gateway|Gateway Time-?out|Service Unavailable)/i)
    .first()
    .isVisible()
    .catch(() => false);
}

// Some formio task forms hydrate their fields only after the wrapper has
// already become visible, so waitForFormioReady may pass before the
// interactive element we actually need is in the DOM.
async function waitForFormioContent(page: Page, target: Locator) {
  for (let attempt = 0; attempt < 3; attempt++) {
    if (await isOnBackendErrorPage(page)) {
      await page.waitForTimeout(TWO_SECONDS_IN_MS);
      await page.reload();
      continue;
    }
    const wrapperReady = await formioForm(page)
      .waitFor({ state: "visible", timeout: TWENTY_SECONDS_IN_MS })
      .then(() => true)
      .catch(() => false);
    if (!wrapperReady) {
      await page.reload();
      continue;
    }
    const found = await target
      .waitFor({ state: "visible", timeout: TWENTY_SECONDS_IN_MS })
      .then(() => true)
      .catch(() => false);
    if (found) return;
    await page.reload();
  }
  await waitForFormioReady(page);
  await target.waitFor({ state: "visible", timeout: FORTY_SECONDS_IN_MS });
}

// UUID v4 regex pattern (replacement for deprecated uuidv4 package)
const UUID_V4_REGEX =
  /[0-9a-f]{8}-[0-9a-f]{4}-4[0-9a-f]{3}-[89ab][0-9a-f]{3}-[0-9a-f]{12}/i;

const PAGE_RELOAD_RETRIES = 5;

const beheerdersGroupId = "beheerders_elk_domein";
const beheerdersGroupName = "Beheerders elk domein - new IAM";
const beheerderUserId = "beheerder1newiam";
const beheerderUser = "Beheerder 1 New IAM";

const COMMUNICATION_CHANNEL_KEY = "E-mail";
const COMMUNICATION_CHANNEL_VALUE = "46";
const RESULT_VALUE = "Verleend";
const STATUS_VALUE = "Afgerond";

When(
  "{string} opens the active task",
  { timeout: TWO_MINUTES_IN_MS },
  async function (this: CustomWorld, user: z.infer<typeof worldUsers>) {
    const viewTaskLink = this.page.getByRole("link", { name: "Taak bekijken" });
    for (let attempt = 0; attempt < 3; attempt++) {
      const found = await viewTaskLink
        .waitFor({ state: "visible", timeout: TWENTY_SECONDS_IN_MS })
        .then(() => true)
        .catch(() => false);
      if (found) break;
      await this.page.reload();
    }
    await viewTaskLink.click();
    for (let attempt = 0; attempt < 3; attempt++) {
      if (!(await isOnBackendErrorPage(this.page))) break;
      await this.page.waitForTimeout(TWO_SECONDS_IN_MS);
      await this.page.reload();
    }
  },
);

Then(
  "{string} sees the form associated with the task",
  { timeout: FORTY_SECONDS_IN_MS },
  async function (this: CustomWorld, user: z.infer<typeof worldUsers>) {
    await waitForFormioReady(this.page);
    const form = formioForm(this.page);
    await expect(form.getByLabel("Group")).toBeVisible();
    await expect(form.getByLabel("User")).toBeVisible();
    await expect(form.getByLabel("Template")).toBeVisible();
    await expect(form.getByRole("button", { name: "Create" })).toBeVisible();
    await expect(
      form.getByRole("searchbox", { name: "Select one or more documents" }),
    ).toBeVisible();
    await expect(form.getByLabel("Communication channel")).toBeVisible();
  },
);

Given(
  "{string} creates a SmartDocuments Word file named {string}",
  { timeout: ONE_MINUTE_IN_MS },
  async function (
    this: CustomWorld,
    user: z.infer<typeof worldUsers>,
    fileName: string,
  ) {
    // BPMN form: create a document
    const form = formioForm(this.page);
    await form
      .getByLabel("Template")
      .selectOption("Data Test", { timeout: FORTY_SECONDS_IN_MS });
    await form.getByRole("button", { name: "Create" }).click();

    // ZAC: Create document sidebar
    await this.page.getByRole("textbox", { name: "Titel" }).click();
    await this.page.getByRole("textbox", { name: "Titel" }).fill(fileName);

    // SmartDocuments wizard
    const smartDocumentsWizardPromise = this.page.waitForEvent("popup");
    await this.page
      .getByRole("button", { name: "Toevoegen", exact: true })
      .click();
    const smartDocumentsWizardPage = await smartDocumentsWizardPromise;
    await smartDocumentsWizardPage
      .getByRole("button", {
        name: /Klaar/i,
      })
      .click();
    const wizardResultDiv = smartDocumentsWizardPage.locator(
      '[role="status"][aria-live="polite"]',
    );
    await wizardResultDiv.waitFor({ state: "attached" });
    await expect(wizardResultDiv.getByText("succes")).toBeVisible({
      timeout: FORTY_SECONDS_IN_MS,
    });
    await smartDocumentsWizardPage.close();
  },
);

When(
  "{string} reloads the page",
  { timeout: FORTY_SECONDS_IN_MS },
  async function (this: CustomWorld, user: z.infer<typeof worldUsers>) {
    await this.page.reload();
    if (await this.page.isVisible("text='Bad Request'")) {
      for (let attempt = 1; attempt <= PAGE_RELOAD_RETRIES; attempt++) {
        await this.page.waitForTimeout(attempt * TWO_SECONDS_IN_MS);
        await this.page.goto(this.page.url().split("?")[0]);
        if (!(await this.page.isVisible("text='Bad Request'"))) {
          break;
        }
      }
    }
  },
);

Then(
  "{string} sees document {string} in the documents list",
  { timeout: TWO_MINUTES_IN_MS },
  async function (
    this: CustomWorld,
    user: z.infer<typeof worldUsers>,
    documentName: string,
  ) {
    await waitForFormioReady(this.page);

    const form = formioForm(this.page);
    const searchbox = form.getByRole("searchbox", {
      name: "Select one or more documents",
    });

    await searchbox.click();
    await searchbox.fill(documentName);

    await expect(
      this.page.getByRole("option", { name: documentName, exact: true }),
    ).toBeVisible({ timeout: ONE_MINUTE_IN_MS });

    await this.page
      .getByRole("option", { name: documentName, exact: true })
      .click();
  },
);

Then(
  "{string} sees the desired form fields values",
  { timeout: ONE_MINUTE_IN_MS },
  async function (this: CustomWorld, user: z.infer<typeof worldUsers>) {
    await waitForFormioReady(this.page);
    const form = formioForm(this.page);
    await expect(form.getByLabel("Group")).toContainText(beheerdersGroupName, {
      timeout: FORTY_SECONDS_IN_MS,
    });
    await form.getByLabel("Communication channel").press("ArrowDown");
    await expect(form.getByLabel("Communication channel")).toContainText(
      "E-mail",
      { timeout: FORTY_SECONDS_IN_MS },
    );
  },
);

When(
  "{string} fills all mandatory form fields",
  { timeout: ONE_MINUTE_IN_MS },
  async function (this: CustomWorld, user: z.infer<typeof worldUsers>) {
    const form = formioForm(this.page);
    await form.getByLabel("Group").selectOption(beheerdersGroupName);
    // User options populate dynamically based on Group selection - wait for them
    await form
      .getByLabel("User")
      .locator("option", { hasText: beheerderUser })
      .waitFor({ state: "attached", timeout: FORTY_SECONDS_IN_MS });
    await form.getByLabel("User").selectOption(beheerderUser);
    const documentsSearchbox = form.getByRole("searchbox", {
      name: "Select one or more documents",
    });
    await documentsSearchbox.click();
    await documentsSearchbox.fill("");
    await this.page
      .getByRole("option", { name: "file A", exact: true })
      .waitFor({ state: "visible" });
    await this.page
      .getByRole("option", { name: "file A", exact: true })
      .click();
    await form
      .getByLabel("Communication channel")
      .selectOption(COMMUNICATION_CHANNEL_KEY);
    await form.getByLabel("Select result").selectOption(RESULT_VALUE);
    await form.getByLabel("Select status").selectOption(STATUS_VALUE);
  },
);

When(
  "{string} submits the filled-in form",
  { timeout: FORTY_SECONDS_IN_MS },
  async function (this: CustomWorld, user: z.infer<typeof worldUsers>) {
    await this.page.keyboard.press("Escape");
    await formioForm(this.page)
      .getByRole("button")
      .filter({ hasText: "Indienen" })
      .click();
  },
);

Then(
  "{string} sees that the initial task is completed",
  { timeout: FORTY_SECONDS_IN_MS },
  async function (this: CustomWorld, user: z.infer<typeof worldUsers>) {
    await expect(
      this.page.getByRole("cell", { name: "Test", exact: true }),
    ).not.toBeVisible({ timeout: FORTY_SECONDS_IN_MS });
    await this.page
      .getByRole("switch", { name: "Toon afgeronde taken" })
      .click();
    await expect(
      this.page.getByRole("cell", { name: "Test", exact: true }),
    ).toBeVisible({ timeout: FORTY_SECONDS_IN_MS });
  },
);

Then(
  "{string} sees that the select documents to sign task is started with group {string} and user {string}",
  { timeout: TWO_MINUTES_IN_MS },
  async function (
    this: CustomWorld,
    user: z.infer<typeof worldUsers>,
    groupName: string,
    userName: string,
  ) {
    const taskCell = this.page.getByRole("cell", {
      name: "Select documents to sign",
    });
    // The BPMN engine can lag behind the previous form submission; reload
    // until the new task cell shows up.
    for (let attempt = 0; attempt < 3; attempt++) {
      const found = await taskCell
        .waitFor({ state: "visible", timeout: TWENTY_SECONDS_IN_MS })
        .then(() => true)
        .catch(() => false);
      if (found) break;
      await this.page.reload();
    }
    await expect(taskCell).toBeVisible({ timeout: FORTY_SECONDS_IN_MS });
    await expect(
      this.page.getByRole("cell", { name: "Toegekend" }),
    ).toBeVisible({ timeout: FORTY_SECONDS_IN_MS });
    await expect(
      this.page.getByRole("cell", { name: groupName }),
    ).toBeVisible({ timeout: FORTY_SECONDS_IN_MS });
    await expect(
      this.page.getByRole("cell", { name: userName, exact: true }),
    ).toBeVisible({ timeout: FORTY_SECONDS_IN_MS });
  },
);

Then(
  "{string} sees that the summary form contains all filled-in data",
  { timeout: TWO_MINUTES_IN_MS },
  async function (this: CustomWorld, user: z.infer<typeof worldUsers>) {
    const form = formioForm(this.page);
    await waitForFormioContent(
      this.page,
      form.getByRole("textbox", { name: "Group" }),
    );
    await expect(form.getByRole("textbox", { name: "Group" })).toHaveValue(
      beheerdersGroupId,
      { timeout: FORTY_SECONDS_IN_MS },
    );
    await expect(form.getByRole("textbox", { name: "User" })).toHaveValue(
      beheerderUserId,
      { timeout: FORTY_SECONDS_IN_MS },
    );
    await expect(form.getByRole("option", { name: UUID_V4_REGEX })).toBeVisible(
      { timeout: FORTY_SECONDS_IN_MS },
    );
    await expect(
      form.getByRole("textbox", { name: "Reference table value" }),
    ).toHaveValue(COMMUNICATION_CHANNEL_VALUE, {
      timeout: FORTY_SECONDS_IN_MS,
    });
    await expect(
      form.getByRole("textbox", { name: "Zaak Result" }),
    ).toHaveValue(RESULT_VALUE, { timeout: FORTY_SECONDS_IN_MS });
    await expect(
      form.getByRole("textbox", { name: "Zaak Status" }),
    ).toHaveValue(STATUS_VALUE, { timeout: FORTY_SECONDS_IN_MS });
  },
);

When(
  "{string} confirms the data in the form",
  { timeout: TWO_MINUTES_IN_MS },
  async function (this: CustomWorld, user: z.infer<typeof worldUsers>) {
    const confirmButton = formioForm(this.page).getByRole("button", {
      name: "Confirm",
    });
    await waitForFormioContent(this.page, confirmButton);
    await confirmButton.click({ timeout: FORTY_SECONDS_IN_MS });
  },
);

Then(
  "{string} sees the zaak status changed to {string}",
  { timeout: FORTY_SECONDS_IN_MS },
  async function (
    this: CustomWorld,
    user: z.infer<typeof worldUsers>,
    status: z.infer<typeof zaakStatus>,
  ) {
    const parsedStatus = zaakStatus.parse(status);
    await expect(this.page.locator("zac-zaak-verkort")).toContainText(
      parsedStatus,
      { timeout: FORTY_SECONDS_IN_MS },
    );
  },
);

Then(
  "{string} sees the zaak result is set to {string}",
  { timeout: FORTY_SECONDS_IN_MS },
  async function (
    this: CustomWorld,
    user: z.infer<typeof worldUsers>,
    result: z.infer<typeof zaakStatus>,
  ) {
    const parsedResult = zaakResult.parse(result);
    await this.expect(
      this.page.getByText(`Resultaat ${parsedResult}`),
    ).toBeVisible({ timeout: FORTY_SECONDS_IN_MS });
  },
);

Then(
  "{string} sees group {string} and user {string} in the zaak data",
  { timeout: FORTY_SECONDS_IN_MS },
  async function (
    this: CustomWorld,
    user: z.infer<typeof worldUsers>,
    groupName: string,
    userName: string,
  ) {
    await this.page.getByRole("button", { name: "Zaakdata" }).click();
    await expect(
      this.page.getByRole("textbox", { name: "zaakGroep" }),
    ).toHaveValue(groupName, { timeout: FORTY_SECONDS_IN_MS });
    await expect(
      this.page.getByRole("textbox", { name: "zaakBehandelaar" }),
    ).toHaveValue(userName, { timeout: FORTY_SECONDS_IN_MS });
    await this.page.getByRole("button").filter({ hasText: "close" }).click();
  },
);

Then(
  "{string} sees the select documents to sign form",
  { timeout: ONE_MINUTE_IN_MS },
  async function (this: CustomWorld, user: z.infer<typeof worldUsers>) {
    await waitForFormioReady(this.page);
    await expect(
      formioForm(this.page).getByRole("searchbox", {
        name: "Select one or more documents",
      }),
    ).toBeVisible({ timeout: FORTY_SECONDS_IN_MS });
  },
);

When(
  "{string} selects document {string} for signing",
  { timeout: FORTY_SECONDS_IN_MS },
  async function (
    this: CustomWorld,
    user: z.infer<typeof worldUsers>,
    documentName: string,
  ) {
    const form = formioForm(this.page);
    await form
      .getByRole("searchbox", { name: "Select one or more documents" })
      .click();
    await this.page
      .getByRole("option", { name: documentName, exact: true })
      .click();
  },
);

Then(
  "{string} sees {int} documents in the to be signed list",
  { timeout: TWO_MINUTES_IN_MS },
  async function (
    this: CustomWorld,
    user: z.infer<typeof worldUsers>,
    expectedCount: number,
  ) {
    const options = formioForm(this.page).getByRole("option", {
      name: UUID_V4_REGEX,
    });
    await waitForFormioContent(this.page, options.first());
    await expect(options).toHaveCount(expectedCount, {
      timeout: FORTY_SECONDS_IN_MS,
    });
  },
);

When(
  "{string} confirms the signing of the documents",
  { timeout: FORTY_SECONDS_IN_MS },
  async function (this: CustomWorld, user: z.infer<typeof worldUsers>) {
    await formioForm(this.page).getByRole("button", { name: "Sign" }).click();
  },
);

Then(
  "{string} sees document {string} has been signed",
  { timeout: ONE_MINUTE_IN_MS },
  async function (
    this: CustomWorld,
    _user: z.infer<typeof worldUsers>,
    documentName: string,
  ) {
    const documentRow = this.page.locator("tr").filter({
      has: this.page.locator("td.mat-column-titel").filter({
        hasText: documentName,
      }),
    });
    await expect(
      documentRow.locator("mat-chip-option").filter({
        has: this.page.locator("mat-icon", { hasText: "fact_check" }),
      }),
    ).toBeVisible({ timeout: FORTY_SECONDS_IN_MS });
  },
);
