/*
 * SPDX-FileCopyrightText: 2025 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

import { Then, When } from "@cucumber/cucumber";
import { expect } from "@playwright/test";
import { z } from "zod";
import { CustomWorld } from "../support/worlds/world";
import { worldUsers } from "../utils/schemes";

const ONE_MINUTE_IN_MS = 60_000;

When(
  "{string} opens the first task",
  { timeout: ONE_MINUTE_IN_MS },
  async function (this: CustomWorld, user: z.infer<typeof worldUsers>) {
    await this.page.getByRole("link", { name: "View task" }).click();
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

When(
  "{string} creates a SmartDocuments Word file named {string}",
  { timeout: ONE_MINUTE_IN_MS },
  async function (
    this: CustomWorld,
    user: z.infer<typeof worldUsers>,
    fileName: string,
  ) {
    await this.page.getByLabel("Template").selectOption("Data Test");
    await this.page.getByRole("button", { name: "Create" }).click();
    await this.page.getByRole("textbox", { name: "Title" }).click();
    await this.page.getByRole("textbox", { name: "Title" }).fill(fileName);

    const smartDocumentsWizardPromise = this.page.waitForEvent("popup");
    await this.page.getByRole("button", { name: "Add", exact: true }).click();

    const smartDocumentsWizardPage = await smartDocumentsWizardPromise;
    await smartDocumentsWizardPage
      .getByRole("button", { name: "Finish" })
      .click();
  },
);

When(
  "{string} reloads the page",
  { timeout: ONE_MINUTE_IN_MS },
  async function (this: CustomWorld, user: z.infer<typeof worldUsers>) {
    await this.page.reload();
  },
);

When(
  "{string} sees document {string} in the documents list",
  { timeout: ONE_MINUTE_IN_MS },
  async function (
    this: CustomWorld,
    user: z.infer<typeof worldUsers>,
    documentName: string,
  ) {
    await expect(this.page.getByLabel("Documents")).toContainText(documentName);
  },
);

When(
  "{string} fills all mandatory form fields",
  { timeout: ONE_MINUTE_IN_MS },
  async function (this: CustomWorld, user: z.infer<typeof worldUsers>) {
    await this.page.getByLabel("Group").selectOption("test-group-fb");
    await this.page.getByLabel("Group").selectOption("functioneelbeheerder1");
    await this.page.getByLabel("Documents").selectOption({ index: 1 });
    await this.page.getByLabel("Documents").selectOption({ index: 2 });
    await this.page.getByLabel("Documents").selectOption("E-mail");
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
      .getByRole("switch", { name: "Show finished tasks" })
      .click();
    await expect(
      this.page.getByRole("cell", { name: "Test form" }),
    ).toBeVisible();
    await expect(
      this.page.locator("span").filter({ hasText: "Finished" }).nth(1),
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
      this.page.locator("span").filter({ hasText: "Unassigned" }).nth(1),
    ).toBeVisible();
  },
);

When(
  "{string} opens the summary form",
  { timeout: ONE_MINUTE_IN_MS },
  async function (this: CustomWorld, user: z.infer<typeof worldUsers>) {
    await this.page
      .getByRole("switch", { name: "Show finished tasks" })
      .click();
    await this.page.getByRole("link", { name: "View task" }).click();
  },
);

Then(
  "{string} sees that the form contains all filled-in data",
  { timeout: ONE_MINUTE_IN_MS },
  async function (this: CustomWorld, user: z.infer<typeof worldUsers>) {
    await expect(this.page.getByRole("textbox", { name: "Group" })).toHaveValue(
      "test-group-co",
    );
    await expect(this.page.getByRole("textbox", { name: "User" })).toHaveValue(
      "coordinator1",
    );
    await expect(this.page.getByRole("combobox").nth(1)).toContainText(
      /^[a-z,0-9,-]{36}$/,
    );
    await expect(this.page.getByRole("combobox").nth(2)).toContainText(
      /^[a-z,0-9,-]{36}$/,
    );
    await expect(
      this.page.getByRole("textbox", { name: "Reference table value" }),
    ).toHaveValue("Balie");
  },
);
