/*
 * SPDX-FileCopyrightText: 2024 Lifely
 * SPDX-License-Identifier: EUPL-1.2+
 */

import { Then, When } from "@cucumber/cucumber";
import { Page, expect } from "@playwright/test";
import { CustomWorld } from "../support/worlds/world";

const ONE_MINUTE_IN_MS = 60_000;
const TWENTY_SECONDS_IN_MS = 20_000;

let smartDocumentsWizardPage: Page;

const documentInput = {
  title: "E2E Test - SmartDocuments Document Title",
  description: "E2E Test - SmartDocuments Document Description",
  author: "E2E Test - SmartDocuments Document Author",
};

When(
  "Employee {string} clicks on Create Document button for the new zaak",
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
    const submitButton = this.page.getByRole("button", {
      name: /Toevoegen/,
    });

    await submitButton.waitFor({ state: "visible" });

    await this.expect(submitButton).toBeDisabled();

    await this.page.getByLabel("Sjabloongroep").click();
    await this.page
      .getByRole("option", { name: "Melding evenement organiseren behandelen" })
      .click();
    await this.page.waitForTimeout(1000);

    await this.page.getByLabel("Sjabloon").last().click();
    await this.page.getByRole("option", { name: "OpenZaakTest" }).click();

    const inputTitle = this.page.getByLabel(/Titel/i);
    await inputTitle.click();
    await inputTitle.fill(documentInput.title);
    await expect(inputTitle).toHaveValue(documentInput.title);

    const inputDescription = this.page.getByLabel(/Beschrijving/i);
    await inputDescription.click();
    await inputDescription.fill(documentInput.description);
    await expect(inputDescription).toHaveValue(documentInput.description);

    const inputAuthor = this.page.getByLabel(/Auteur/i);
    await inputAuthor.click();
    await inputAuthor.fill(documentInput.author);
    await expect(inputAuthor).toHaveValue(documentInput.author);

    await this.expect(submitButton).toBeEnabled();
    await submitButton.click();
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

    const klaarButton = smartDocumentsWizardPage.getByRole("button", {
      name: /Klaar/i,
    });

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

    const wizardResultDiv = smartDocumentsWizardPage.locator(
      '[role="status"][aria-live="polite"]',
    );
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

    const documentTitleText = this.page.locator(`text=${documentInput.title}`);
    // increase the timout because it can take a while for the document to be visible
    await expect(documentTitleText.first()).toBeVisible({
      timeout: TWENTY_SECONDS_IN_MS,
    });

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
      `text=${documentInput.title}`,
    );
    await documentTitleText.waitFor({ state: "attached" });
    await expect(documentTitleText).toBeVisible();

    const documnentDescriptionText = tabPanelLocator.locator(
      `text=${documentInput.description}`,
    );
    await documnentDescriptionText.waitFor({ state: "attached" });
    await expect(documnentDescriptionText).toBeVisible();

    const documnentAuthor = tabPanelLocator.locator(
      `text=${documentInput.author}`,
    );
    await documnentAuthor.waitFor({ state: "attached" });
    await expect(documnentAuthor).toBeVisible();
  },
);
