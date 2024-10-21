/*
 * SPDX-FileCopyrightText: 2024 Lifely
 * SPDX-License-Identifier: EUPL-1.2+
 */

import { Then, When } from "@cucumber/cucumber";
import { CustomWorld } from "../support/worlds/world";
import { Page } from "@playwright/test";

const ONE_MINUTE_IN_MS = 60_000;

// Reference to smartdocuments page
let smartDocumentsPage: Page;

When(
  "Employee {string} clicks on Create Document for zaak",
  { timeout: ONE_MINUTE_IN_MS },
  async function (this: CustomWorld, user) {
    await this.page.getByText("note_addDocument maken").click();

    const sidebar = this.page.locator("div.sidenav-title");
    await sidebar.waitFor({ state: "visible" });
    await sidebar.getByText("Document maken").click();
  },
);

When(
  "Employee {string} fills in the create document form",
  { timeout: ONE_MINUTE_IN_MS },
  async function (this: CustomWorld, user) {
    const submitButton = this.page.locator("#opslaan_button");
    await submitButton.waitFor({ state: "visible" });

    // Verify that the button is initially disabled
    const isInitiallyDisabled = await submitButton.isDisabled();
    if (!isInitiallyDisabled) {
      throw new Error(
        "The submit button should be disabled initially but it is enabled.",
      );
    }

    // filling the create document form
    await this.page.getByLabel("Sjabloongroep").click();
    await this.page
      .getByRole("option", { name: "Melding evenement organiseren behandelen" })
      .click();
    await this.page.waitForTimeout(1000);

    await this.page.getByLabel("Sjabloon").last().click();
    await this.page.getByRole("option", { name: "OpenZaakTest" }).click();

    const autofillInputTitle = this.page.locator("#title_tekstfield");
    await autofillInputTitle.click();
    await autofillInputTitle.fill("Document Title Text");

    // Now check if the button is enabled after those actions
    const isEnabled = await submitButton.isEnabled();
    if (!isEnabled) {
      throw new Error(
        "The submit button should be enabled after the necessary actions but it is still disabled.",
      );
    }
  },
);

When(
  "Employee {string} submits the form to create the document should see SmartDocuments tab",
  { timeout: ONE_MINUTE_IN_MS },
  async function (this: CustomWorld, user) {
    const submitButton = this.page.locator("#opslaan_button");
    await submitButton.click();

    smartDocumentsPage = await this.page.waitForEvent("popup");
    await this.expect(
      smartDocumentsPage.getByRole("link", { name: "SmartDocuments" }),
    ).toBeVisible();
  },
);

Then(
  "Employee {string} submits the SmartDocuments form",
  { timeout: ONE_MINUTE_IN_MS },
  async function (this: CustomWorld, user) {
    // Use the reference to the smartDocumentsPage
    // Wait if needed for any loading

    // await smartDocumentsPage.waitForTimeout(5000);

    const klaarButton = smartDocumentsPage.locator(
      "#gwt-debug-wizardEngine_panelControls_nextButton",
    );
    await klaarButton.waitFor({ state: "visible" });
    await klaarButton.click();

    // Wait for any subsequent actions if necessary
    await smartDocumentsPage.waitForTimeout(35000); // Adjust the wait time as needed
    await smartDocumentsPage.waitForTimeout(35000); // Adjust the wait time as needed
    await smartDocumentsPage.waitForTimeout(35000); // Adjust the wait time as needed
    await smartDocumentsPage.waitForTimeout(35000); // Adjust the wait time as needed

    // const allPages = this.page.context().pages();
    // await allPages[1].close();
  },
);

Then(
  "Employee {string} should not get an error",
  { timeout: ONE_MINUTE_IN_MS },
  async function (this: CustomWorld, user) {
    const caseNumber = this.testStorage.get("caseNumber");
    await this.expect(this.page.getByText(caseNumber).first()).toBeVisible();
  },
);
