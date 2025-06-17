/*
 * SPDX-FileCopyrightText: 2025 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

import { Given, Then, When } from "@cucumber/cucumber";
import { expect } from "@playwright/test";
import { z } from "zod";
import { CustomWorld } from "../support/worlds/world";
import { worldUsers, zaakResult, zaakStatus } from "../utils/schemes";

const ONE_MINUTE_IN_MS = 60_000;
const TWENTY_SECOND_IN_MS = 20_000;

When(
  "{string} opens the active task",
  { timeout: ONE_MINUTE_IN_MS },
  async function (this: CustomWorld, user: z.infer<typeof worldUsers>) {
    const viewTaskLink = this.page.getByRole("link", { name: "Taak bekijken" });
    await viewTaskLink.scrollIntoViewIfNeeded();
    await viewTaskLink.click();
  },
);

Then(
  "{string} sees the form associated with the task",
  { timeout: ONE_MINUTE_IN_MS },
  async function (this: CustomWorld, user: z.infer<typeof worldUsers>) {
    await expect(this.page.getByLabel("Group")).toBeVisible();
    await expect(this.page.getByLabel("User")).toBeVisible();
    await expect(this.page.getByLabel("Template")).toBeVisible();
    await expect(
      this.page.getByRole("button", { name: "Create" }),
    ).toBeVisible();
    await expect(this.page.getByLabel("Documents")).toBeVisible();
    await expect(this.page.getByLabel("Communication channel")).toBeVisible();
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
    await this.page.getByLabel("Template").selectOption("Data Test");
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
  { timeout: ONE_MINUTE_IN_MS },
  async function (this: CustomWorld, user: z.infer<typeof worldUsers>) {
    await this.page.reload();
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
    await expect(this.page.getByLabel("Documents")).toContainText(
      documentName,
      { timeout: ONE_MINUTE_IN_MS },
    );
  },
);

Then(
  "{string} sees the desired form fields values",
  { timeout: ONE_MINUTE_IN_MS },
  async function (this: CustomWorld, user: z.infer<typeof worldUsers>) {
    await expect(this.page.getByLabel("Group")).toContainText(
      "Functioneelbeheerders",
      { timeout: TWENTY_SECOND_IN_MS },
    );
    await expect(this.page.getByLabel("Communication channel")).toContainText(
      "E-mail",
      { timeout: TWENTY_SECOND_IN_MS },
    );
  },
);

When(
  "{string} fills all mandatory form fields",
  { timeout: ONE_MINUTE_IN_MS },
  async function (this: CustomWorld, user: z.infer<typeof worldUsers>) {
    await this.page.getByLabel("Group").selectOption("functioneelbeheerders");
    await this.page.getByLabel("User").selectOption("functioneelbeheerder2");
    await this.page.getByLabel("Documents").selectOption(["file A", "file B"]);
    await this.page.getByLabel("Communication channel").selectOption("E-mail");
  },
);

When(
  "{string} submits the filled-in form",
  { timeout: ONE_MINUTE_IN_MS },
  async function (this: CustomWorld, user: z.infer<typeof worldUsers>) {
    await this.page
      .getByRole("button", { name: "submitButtonAriaLabel" })
      .click();
  },
);

Then(
  "{string} sees that the initial task is completed",
  { timeout: ONE_MINUTE_IN_MS },
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
  "{string} sees that the summary task is started",
  { timeout: ONE_MINUTE_IN_MS },
  async function (this: CustomWorld, user: z.infer<typeof worldUsers>) {
    await expect(
      this.page.getByRole("cell", { name: "Summary" }),
    ).toBeVisible();
    await expect(
      this.page.locator("span").filter({ hasText: "Niet toegekend" }).nth(1),
    ).toBeVisible();
  },
);

Then(
  "{string} sees that the summary form contains all filled-in data",
  { timeout: ONE_MINUTE_IN_MS },
  async function (this: CustomWorld, user: z.infer<typeof worldUsers>) {
    await expect(this.page.getByRole("textbox", { name: "Group" })).toHaveValue(
      "functioneelbeheerders",
    );
    await expect(this.page.getByRole("textbox", { name: "User" })).toHaveValue(
      "functioneelbeheerder2",
    );
    await expect(this.page.getByRole("combobox").nth(0)).toContainText(
      /^[a-z,0-9-]{36}/,
    );
    await expect(
      this.page.getByRole("textbox", { name: "Reference table value" }),
    ).toHaveValue("E-mail");
  },
);

When(
  "{string} confirms the data in the form",
  { timeout: ONE_MINUTE_IN_MS },
  async function (this: CustomWorld, user: z.infer<typeof worldUsers>) {
    await this.page.getByRole("button", { name: "Confirm" }).click();
  },
);

Then(
  "{string} sees the zaak status changed to {string}",
  { timeout: ONE_MINUTE_IN_MS },
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
  { timeout: ONE_MINUTE_IN_MS },
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
