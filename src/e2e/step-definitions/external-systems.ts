/*
 * SPDX-FileCopyrightText: 2024 Lifely
 * SPDX-License-Identifier: EUPL-1.2+
 */

import { Then, When } from "@cucumber/cucumber";
import { expect, Page } from "@playwright/test";
import { CustomWorld } from "../support/worlds/world";

const ONE_MINUTE_IN_MS = 60_000;

let smartDocumentsWizardPage: Page;
let addedDocumentTitle: string;

When(
  "Employee {string} clicks on Create Document for zaak",
  { timeout: ONE_MINUTE_IN_MS },
  async function (this: CustomWorld, user) {
    await this.page.getByText("note_addDocument maken").click();

    const sidebar = this.page.locator("div.sidenav-title");
    await sidebar.waitFor({ state: "visible" });
    await sidebar.getByText("Document maken");
  },
);

When(
  "Employee {string} enters and submits the form to start the SmartDocuments wizard",
  { timeout: ONE_MINUTE_IN_MS },
  async function (this: CustomWorld, user) {
    const submitButton = this.page.locator("#opslaan_button");
    await submitButton.waitFor({ state: "visible" });

    await this.expect(submitButton).toBeDisabled();

    await this.page.getByLabel("Sjabloongroep").click();
    await this.page
      .getByRole("option", { name: "Melding evenement organiseren behandelen" })
      .click();
    await this.page.waitForTimeout(1000);

    await this.page.getByLabel("Sjabloon").last().click();
    await this.page.getByRole("option", { name: "OpenZaakTest" }).click();

    const autofillInputTitle = this.page.locator("#title_tekstfield");
    addedDocumentTitle = `E2E Document Title Text`;
    await autofillInputTitle.click();
    await autofillInputTitle.fill(addedDocumentTitle);
    await expect(autofillInputTitle).toHaveValue(addedDocumentTitle);

    await this.expect(submitButton).toBeEnabled();
    await this.page.click("#opslaan_button");
  },
);

When(
  "Employee {string} completes the SmartDocuments wizard",
  { timeout: ONE_MINUTE_IN_MS },
  async function (this: CustomWorld, user) {
    smartDocumentsWizardPage = await this.page.waitForEvent("popup");
    await this.expect(
      smartDocumentsWizardPage.getByRole("link", { name: "SmartDocuments" }),
    ).toBeVisible();

    const klaarButton = smartDocumentsWizardPage.locator(
      "#gwt-debug-wizardEngine_panelControls_nextButton",
    );
    await klaarButton.waitFor({ state: "visible" });
    await klaarButton.click();
  },
);

When(
  "Employee {string} closes the wizard result page",
  { timeout: ONE_MINUTE_IN_MS },
  async function (this: CustomWorld, user) {
    const caseNumber = this.testStorage.get("caseNumber");
    const caseNumberLocator = smartDocumentsWizardPage.locator(
      `text=${caseNumber}`,
    );
    await expect(caseNumberLocator).toHaveCount(2);

    const wizardResultDiv = smartDocumentsWizardPage.locator("#wizard-result");
    await wizardResultDiv.waitFor({ state: "attached" });
    await expect(wizardResultDiv).toBeVisible();

    await expect(wizardResultDiv).toHaveClass(/wizard-result success/);
    await expect(wizardResultDiv.getByText("succes")).toBeVisible();

    await smartDocumentsWizardPage.close();
  },
);

Then(
  "Employee {string} sees the newly created document added to the zaak",
  { timeout: ONE_MINUTE_IN_MS },
  async function (this: CustomWorld, user) {
    const caseNumber = this.testStorage.get("caseNumber");
    const caseNumberLocator = this.page.locator(`text=${caseNumber}`);
    await expect(caseNumberLocator).toHaveCount(2);

    const documnentTitleText = this.page.locator(`text=${addedDocumentTitle}`);
    await documnentTitleText.waitFor({ state: "attached" });
    await expect(documnentTitleText.first()).toBeVisible();
  },
);
