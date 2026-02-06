/*
 * SPDX-FileCopyrightText: 2025 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

import { Given, Then, When } from "@cucumber/cucumber";
import { expect } from "@playwright/test";
import { z } from "zod";

// UUID v4 regex pattern (replacement for deprecated uuidv4 package)
const UUID_V4_REGEX =
  /[0-9a-f]{8}-[0-9a-f]{4}-4[0-9a-f]{3}-[89ab][0-9a-f]{3}-[0-9a-f]{12}/i;
import { CustomWorld } from "../support/worlds/world";
import { worldUsers, zaakResult, zaakStatus } from "../utils/schemes";

const TWO_MINUTES_IN_MS = 120_000;
const FORTY_SECOND_IN_MS = 40_000;
const TWO_SECONDS_IN_MS = 2_000;
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
    await viewTaskLink.scrollIntoViewIfNeeded();
    await viewTaskLink.click();
  },
);

Then(
  "{string} sees the form associated with the task",
  { timeout: TWO_MINUTES_IN_MS },
  async function (this: CustomWorld, user: z.infer<typeof worldUsers>) {
    await expect(this.page.getByLabel("Group")).toBeVisible();
    await expect(this.page.getByLabel("User")).toBeVisible();
    await expect(this.page.getByLabel("Template")).toBeVisible();
    await expect(
      this.page.getByRole("button", { name: "Create" }),
    ).toBeVisible();
    await expect(
      this.page.getByRole("searchbox", {
        name: "Select one or more documents",
      }),
    ).toBeVisible();
    await expect(this.page.getByLabel("Communication channel")).toBeVisible();
  },
);

Given(
  "{string} creates a SmartDocuments Word file named {string}",
  { timeout: TWO_MINUTES_IN_MS },
  async function (
    this: CustomWorld,
    user: z.infer<typeof worldUsers>,
    fileName: string,
  ) {
    // BPMN form: create a document
    await this.page
      .getByLabel("Template")
      .selectOption("Data Test", { timeout: FORTY_SECOND_IN_MS });
    await this.page.getByRole("button", { name: "Create" }).click();

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
  { timeout: TWO_MINUTES_IN_MS },
  async function (this: CustomWorld, user: z.infer<typeof worldUsers>) {
    await this.page.reload();
    for (let attempt = 0; attempt < PAGE_RELOAD_RETRIES; attempt++) {
      await this.page.waitForURL(this.page.url());
      if (!(await this.page.isVisible("text='Bad Request'"))) {
        break;
      }
      await this.page.waitForTimeout(attempt * TWO_SECONDS_IN_MS);
      await this.page.goto(this.page.url().split("?")[0]);
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
    await this.page
      .getByRole("searchbox", {
        name: "Select one or more documents",
      })
      .fill(documentName);

    await expect(
      this.page.getByRole("option", { name: documentName, exact: true }),
    ).toContainText(documentName, { timeout: FORTY_SECOND_IN_MS });
  },
);

Then(
  "{string} sees the desired form fields values",
  { timeout: TWO_MINUTES_IN_MS },
  async function (this: CustomWorld, user: z.infer<typeof worldUsers>) {
    await expect(this.page.getByLabel("Group")).toContainText(
      beheerdersGroupName,
      { timeout: FORTY_SECOND_IN_MS },
    );
    await this.page.getByLabel("Communication channel").press("ArrowDown");
    await expect(this.page.getByLabel("Communication channel")).toContainText(
      "E-mail",
      { timeout: FORTY_SECOND_IN_MS },
    );
  },
);

When(
  "{string} fills all mandatory form fields",
  { timeout: TWO_MINUTES_IN_MS },
  async function (this: CustomWorld, user: z.infer<typeof worldUsers>) {
    await this.page.getByLabel("Group").selectOption(beheerdersGroupName);

    await this.page.getByLabel("User").click();
    await this.page.getByLabel("User").selectOption(beheerderUser);
    await this.page
      .getByRole("searchbox", { name: "Select one or more documents" })
      .fill("");
    await this.page
      .getByRole("option", { name: "file A", exact: true })
      .click();
    await this.page
      .getByLabel("Communication channel")
      .selectOption(COMMUNICATION_CHANNEL_KEY);
    await this.page.getByLabel("Select result").click();
    await this.page.getByLabel("Select result").selectOption(RESULT_VALUE);
    await this.page.getByLabel("Select status").click();
    await this.page.getByLabel("Select status").selectOption(STATUS_VALUE);
  },
);

When(
  "{string} submits the filled-in form",
  { timeout: TWO_MINUTES_IN_MS },
  async function (this: CustomWorld, user: z.infer<typeof worldUsers>) {
    await this.page
      .getByRole("button", { name: "submitButtonAriaLabel" })
      .click();
  },
);

Then(
  "{string} sees that the initial task is completed",
  { timeout: TWO_MINUTES_IN_MS },
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
  "{string} sees that the summary task is started with group {string} and user {string}",
  { timeout: TWO_MINUTES_IN_MS },
  async function (
    this: CustomWorld,
    user: z.infer<typeof worldUsers>,
    groupName: string,
    userName: string,
  ) {
    await expect(this.page.getByRole("cell", { name: "Summary" })).toBeVisible({
      timeout: FORTY_SECOND_IN_MS,
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
  { timeout: TWO_MINUTES_IN_MS },
  async function (this: CustomWorld, user: z.infer<typeof worldUsers>) {
    await expect(this.page.getByRole("textbox", { name: "Group" })).toHaveValue(
      beheerdersGroupId,
    );
    await expect(this.page.getByRole("textbox", { name: "User" })).toHaveValue(
      beheerderUserId,
    );
    await expect(
      this.page.getByRole("option", { name: UUID_V4_REGEX }),
    ).toBeVisible({
      timeout: FORTY_SECOND_IN_MS,
    });
    await expect(
      this.page.getByRole("textbox", { name: "Reference table value" }),
    ).toHaveValue(COMMUNICATION_CHANNEL_VALUE);
    await expect(
      this.page.getByRole("textbox", { name: "Zaak Result" }),
    ).toHaveValue(RESULT_VALUE);
    await expect(
      this.page.getByRole("textbox", { name: "Zaak Status" }),
    ).toHaveValue(STATUS_VALUE);
  },
);

When(
  "{string} confirms the data in the form",
  { timeout: TWO_MINUTES_IN_MS },
  async function (this: CustomWorld, user: z.infer<typeof worldUsers>) {
    await this.page.getByRole("button", { name: "Confirm" }).click();
  },
);

Then(
  "{string} sees the zaak status changed to {string}",
  { timeout: TWO_MINUTES_IN_MS },
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
  { timeout: TWO_MINUTES_IN_MS },
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
  { timeout: TWO_MINUTES_IN_MS },
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
