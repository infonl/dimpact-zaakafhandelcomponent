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
let addedDocumentDesscription: string;
let addedDocumentAuthor: string;

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

    const inputTitle = this.page.locator("#title_tekstfield");
    addedDocumentTitle = `E2E Test - Document Title Text`;
    await inputTitle.click();
    await inputTitle.fill(addedDocumentTitle);
    await expect(inputTitle).toHaveValue(addedDocumentTitle);

    const inputDescription = this.page.locator("#description_tekstfield");
    addedDocumentDesscription = `E2E Test - Document Description Text`;
    await inputDescription.click();
    await inputDescription.fill(addedDocumentDesscription);
    await expect(inputDescription).toHaveValue(addedDocumentDesscription);

    const inputAuthor = this.page.locator("#auteur_tekstfield");
    addedDocumentAuthor = `E2E Test - Document Author Name`;
    await inputAuthor.click();
    await inputAuthor.fill(addedDocumentAuthor);
    await expect(inputAuthor).toHaveValue(addedDocumentAuthor);

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

When(
  "Employee {string} views the created document",
  { timeout: ONE_MINUTE_IN_MS },
  async function (this: CustomWorld, user) {
    const caseNumber = this.testStorage.get("caseNumber");
    const caseNumberLocator = this.page.locator(`text=${caseNumber}`);

    await expect(caseNumberLocator).toHaveCount(2);

    const documentTitleText = this.page.locator(`text=${addedDocumentTitle}`);
    await expect(documentTitleText.first()).toBeVisible();

    const anchorLocator = this.page.locator('a[title="Document bekijken"]');
    await anchorLocator.click();
  },
);

Then(
  "Employee {string} sees all added details in the created document meta data",
  { timeout: ONE_MINUTE_IN_MS },
  async function (this: CustomWorld, user) {
    const tabPanelLocator = this.page
      .locator('mat-tab-body[role="tabpanel"]')
      .nth(0);
    await tabPanelLocator.waitFor({ state: "visible" });
    await expect(tabPanelLocator).toBeVisible();

    const documentTitleText = tabPanelLocator.locator(
      `text=${addedDocumentTitle}`,
    );
    await documentTitleText.waitFor({ state: "attached" });
    await expect(documentTitleText).toBeVisible();

    const documnentDescriptionText = tabPanelLocator.locator(
      `text=${addedDocumentDesscription}`,
    );
    await documnentDescriptionText.waitFor({ state: "attached" });
    await expect(documnentDescriptionText).toBeVisible();

    const documnentAuthor = tabPanelLocator.locator(
      `text=${addedDocumentAuthor}`,
    );
    await documnentAuthor.waitFor({ state: "attached" });
    await expect(documnentAuthor).toBeVisible();
  },
);
