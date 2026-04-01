/*
 * SPDX-FileCopyrightText: 2025 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

import { Given, Then, When } from "@cucumber/cucumber";
import { expect, type Page } from "@playwright/test";
import { z } from "zod";
import {
  FORTY_SECONDS_IN_MS,
  ONE_MINUTE_IN_MS,
  ONE_SECOND_IN_MS,
} from "../support/time-constants";
import { CustomWorld } from "../support/worlds/world";
import { worldUsers, zaakResult, zaakStatus } from "../utils/schemes";

function formioForm(page: Page) {
  return page.locator("zac-formio-wrapper");
}

async function waitForFormioReady(page: Page) {
  const form = formioForm(page);
  await expect(form).toBeVisible();
  await form.locator(".formio-component").first().waitFor({ state: "visible" });
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
  { timeout: FORTY_SECONDS_IN_MS },
  async function (this: CustomWorld, user: z.infer<typeof worldUsers>) {
    const viewTaskLink = this.page.getByRole("link", { name: "Taak bekijken" });
    await viewTaskLink.scrollIntoViewIfNeeded();
    await viewTaskLink.click();
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
    await this.page
      .getByRole("button", { name: "Toevoegen", exact: true })
      .click();

    // SmartDocuments wizard
    const smartDocumentsWizardPromise = this.page.waitForEvent("popup");
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
    await expect(wizardResultDiv.getByText("succes")).toBeVisible();
    await smartDocumentsWizardPage.close();
  },
);

When(
  "{string} reloads the page",
  { timeout: FORTY_SECONDS_IN_MS },
  async function (this: CustomWorld, user: z.infer<typeof worldUsers>) {
    await this.page.waitForLoadState("networkidle");
    await this.page.reload();
    for (let attempt = 0; attempt < PAGE_RELOAD_RETRIES; attempt++) {
      await this.page.waitForURL(this.page.url());
      if (!(await this.page.isVisible("text='Bad Request'"))) {
        break;
      }
      await this.page.waitForTimeout(ONE_SECOND_IN_MS);
      await this.page.goto(this.page.url().split("?")[0]);
    }
  },
);

Then(
  "{string} sees document {string} in the documents list",
  { timeout: ONE_MINUTE_IN_MS },
  async function (
    this: CustomWorld,
    user: z.infer<typeof worldUsers>,
    documentName: string,
  ) {
    const form = formioForm(this.page);
    await form
      .getByRole("searchbox", { name: "Select one or more documents" })
      .fill(documentName);

    await expect(
      this.page.getByRole("option", { name: documentName, exact: true }),
    ).toContainText(documentName, { timeout: FORTY_SECONDS_IN_MS });
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
  { timeout: FORTY_SECONDS_IN_MS },
  async function (this: CustomWorld, user: z.infer<typeof worldUsers>) {
    const form = formioForm(this.page);
    await form.getByLabel("Group").selectOption(beheerdersGroupName);
    await form.getByLabel("User").selectOption(beheerderUser);
    await form
      .getByRole("searchbox", { name: "Select one or more documents" })
      .fill("");
    await this.page.getByRole("option", { name: "file A", exact: true }).click();
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
    ).not.toBeVisible();
    await this.page
      .getByRole("switch", { name: "Toon afgeronde taken" })
      .click();
    await expect(
      this.page.getByRole("cell", { name: "Test", exact: true }),
    ).toBeVisible();
  },
);

Then(
  "{string} sees that the select documents to sign task is started with group {string} and user {string}",
  { timeout: ONE_MINUTE_IN_MS },
  async function (
    this: CustomWorld,
    user: z.infer<typeof worldUsers>,
    groupName: string,
    userName: string,
  ) {
    await expect(
      this.page.getByRole("cell", { name: "Select documents to sign" }),
    ).toBeVisible({
      timeout: FORTY_SECONDS_IN_MS,
    });
    await expect(
      this.page.getByRole("cell", { name: "Toegekend" }),
    ).toBeVisible();
    await expect(
      this.page.getByRole("cell", { name: groupName }),
    ).toBeVisible();
    await expect(
      this.page.getByRole("cell", { name: userName, exact: true }),
    ).toBeVisible();
  },
);

Then(
  "{string} sees that the summary form contains all filled-in data",
  { timeout: ONE_MINUTE_IN_MS },
  async function (this: CustomWorld, user: z.infer<typeof worldUsers>) {
    await waitForFormioReady(this.page);
    const form = formioForm(this.page);
    await expect(form.getByRole("textbox", { name: "Group" })).toHaveValue(
      beheerdersGroupId,
    );
    await expect(form.getByRole("textbox", { name: "User" })).toHaveValue(
      beheerderUserId,
    );
    await expect(
      form.getByRole("option", { name: UUID_V4_REGEX }),
    ).toBeVisible({ timeout: FORTY_SECONDS_IN_MS });
    await expect(
      form.getByRole("textbox", { name: "Reference table value" }),
    ).toHaveValue(COMMUNICATION_CHANNEL_VALUE);
    await expect(
      form.getByRole("textbox", { name: "Zaak Result" }),
    ).toHaveValue(RESULT_VALUE);
    await expect(
      form.getByRole("textbox", { name: "Zaak Status" }),
    ).toHaveValue(STATUS_VALUE);
  },
);

When(
  "{string} confirms the data in the form",
  { timeout: FORTY_SECONDS_IN_MS },
  async function (this: CustomWorld, user: z.infer<typeof worldUsers>) {
    await formioForm(this.page)
      .getByRole("button", { name: "Confirm" })
      .click();
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
    ).toBeVisible();
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
    ).toHaveValue(groupName);
    await expect(
      this.page.getByRole("textbox", { name: "zaakBehandelaar" }),
    ).toHaveValue(userName);
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
  { timeout: ONE_MINUTE_IN_MS },
  async function (
    this: CustomWorld,
    user: z.infer<typeof worldUsers>,
    expectedCount: number,
  ) {
    await waitForFormioReady(this.page);
    await expect(
      formioForm(this.page).getByRole("option", { name: UUID_V4_REGEX }),
    ).toHaveCount(expectedCount, {
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
