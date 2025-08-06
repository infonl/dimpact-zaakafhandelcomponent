/*
 * SPDX-FileCopyrightText: 2025 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

import { Given, Then, When } from "@cucumber/cucumber";
import { expect } from "@playwright/test";
import playwright from "playwright";
import { z } from "zod";
import { CustomWorld } from "../support/worlds/world";
import { worldUsers, zaakResult, zaakStatus } from "../utils/schemes";

const TWO_MINUTES_IN_MS = 120_000;
const FORTY_SECOND_IN_MS = 40_000;
const FIVE_SECONDS_IN_MS = 5_000;
const TWO_SECONDS_IN_MS = 2_000;
const PAGE_RELOAD_RETRIES = 5;

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
      this.page.getByRole("textbox", { name: "Select one or more documents" }),
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
    await triggerDataLoad(this.page, "Template", { text: "SmartDocuments" });

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

async function triggerDataLoad(
  page: playwright.Page,
  componentLabel: string,
  options?: { text?: string; timeout?: number },
) {
  await expect(page.getByLabel(componentLabel)).toBeVisible({
    timeout: FORTY_SECOND_IN_MS,
  });

  // First click
  await page.getByLabel(componentLabel).click();
  await page.getByLabel(componentLabel).press("ArrowDown");

  if (options?.text) {
    await page.getByText(options?.text).focus();
    await page.getByText(options?.text).click();
  }

  await page.waitForTimeout(options?.timeout || TWO_SECONDS_IN_MS);

  // Press arrow-down on the component again
  await page.getByLabel(componentLabel).press("Escape");
  await page.getByLabel(componentLabel).press("ArrowDown");
}

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
    await triggerDataLoad(this.page, "Select one or more documents", {
      text: "Available Documents",
      timeout: FIVE_SECONDS_IN_MS,
    });
    await expect(
      this.page.getByRole("option", { name: documentName, exact: true }),
    ).toContainText(documentName, { timeout: FORTY_SECOND_IN_MS });
  },
);

Then(
  "{string} sees the desired form fields values",
  { timeout: TWO_MINUTES_IN_MS },
  async function (this: CustomWorld, user: z.infer<typeof worldUsers>) {
    await triggerDataLoad(this.page, "Group", { text: "Approval by:" });

    await expect(this.page.getByLabel("Group")).toContainText(
      "Functioneelbeheerders",
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
    await this.page.getByLabel("Group").selectOption("functioneelbeheerders");

    await this.page.getByLabel("User").click();
    await this.page.getByLabel("User").selectOption("functioneelbeheerder2");
    await this.page
      .getByRole("textbox", { name: "Select one or more documents" })
      .fill("");
    await this.page.getByLabel("Test form").getByText("file A").click();
    await this.page.getByLabel("Test form").getByText("file B").click();
    await this.page.getByLabel("Communication channel").selectOption("E-mail");
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
    await this.page
      .getByRole("switch", { name: "Toon afgeronde taken" })
      .click();
    await expect(
      this.page.getByRole("cell", { name: "Test form" }),
    ).toBeVisible();
    await expect(
      this.page.locator("span").filter({ hasText: "Afgerond" }).nth(1),
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
    await expect(
      this.page.getByRole("cell", { name: "Summary" }),
    ).toBeVisible();
    await expect(
      this.page.locator("span").filter({ hasText: "Niet toegekend" }).nth(1),
    ).toBeVisible();
    await expect(this.page.getByRole("cell", { name: groupName })).toBeVisible();
    await expect(this.page.getByRole("cell", { name: userName })).toBeVisible();
  },
);

Then(
  "{string} sees that the summary form contains all filled-in data",
  { timeout: TWO_MINUTES_IN_MS },
  async function (this: CustomWorld, user: z.infer<typeof worldUsers>) {
    await expect(this.page.getByRole("textbox", { name: "Group" })).toHaveValue(
      "functioneelbeheerders",
    );
    await expect(this.page.getByRole("textbox", { name: "User" })).toHaveValue(
      "functioneelbeheerder2",
    );
    await expect(this.page.getByRole("combobox")).toContainText("file A", {
      timeout: FORTY_SECOND_IN_MS,
    });
    await expect(this.page.getByRole("combobox")).toContainText("file B", {
      timeout: FORTY_SECOND_IN_MS,
    });
    await expect(
      this.page.getByRole("textbox", { name: "Reference table value" }),
    ).toHaveValue("E-mail");
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
    "{string} sees group {string} in the zaak data",
    { timeout: TWO_MINUTES_IN_MS },
    async function (
        this: CustomWorld,
        user: z.infer<typeof worldUsers>,
        groupName: string,
    ) {
        await this.page.getByRole('button', { name: 'Zaakdata' }).click();
        await expect(this.page.getByRole('textbox', { name: 'zaakGroep' })).toHaveValue(groupName);
        await this.page.getByRole('button').filter({ hasText: 'close' }).click();
    }
)

Then(
    "{string} sees group {string} in the zaak data",
    { timeout: TWO_MINUTES_IN_MS },
    async function (
        this: CustomWorld,
        user: z.infer<typeof worldUsers>,
        groupName: string,
    ) {
        await this.page.getByRole('button', { name: 'Zaakdata' }).click();
        await expect(this.page.getByRole('textbox', { name: 'zaakGroep' })).toHaveValue(groupName);
        await this.page.getByRole('button').filter({ hasText: 'close' }).click();
    }
)

Then(
    "{string} sees group {string} and user {string} in the zaak data",
    { timeout: TWO_MINUTES_IN_MS },
    async function (
        this: CustomWorld,
        user: z.infer<typeof worldUsers>,
        groupName: string,
        userName: string,
    ) {
        await this.page.getByRole('button', { name: 'Zaakdata' }).click();
        await expect(this.page.getByRole('textbox', { name: 'zaakGroep' })).toHaveValue(groupName);
        await expect(this.page.getByRole('textbox', { name: 'zaakBehandelaar' })).toHaveValue(userName);
        await this.page.getByRole('button').filter({ hasText: 'close' }).click();
    }
)
