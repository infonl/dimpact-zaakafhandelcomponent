/*
 * SPDX-FileCopyrightText: 2024 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

import { Then, When } from "@cucumber/cucumber";
import { Page, expect } from "@playwright/test";
import {
  FIVE_SECONDS_IN_MS,
  FORTY_SECONDS_IN_MS,
  ONE_MINUTE_IN_MS,
} from "../support/time-constants";
import { CustomWorld } from "../support/worlds/world";

let smartDocumentsWizardPage: Page;

const templateInput = {
  group: "Dimpact",
  template: "Data Test",
};

const documentInput = {
  title: "E2E Test - SmartDocuments Document Title",
  description: "E2E Test - SmartDocuments Document Description",
  author: "E2E Test - SmartDocuments Document Author",
};

When(
  "Employee {string} clicks on Create Document button for the new zaak",
  { timeout: ONE_MINUTE_IN_MS },
  async function (this: CustomWorld, user) {
    await this.page.getByRole("button", { name: /Document maken/i }).click();

    await this.page
      .getByRole("heading", { name: "Document maken" })
      .waitFor({ state: "visible" });
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

    // Typing filters the autocomplete, so the option clicked below is the only one left.
    // By role, not by label: an open autocomplete panel carries the same label as its input.
    const templateGroupField = this.page.getByRole("combobox", {
      name: "Sjabloongroep",
    });
    await templateGroupField.click();
    await templateGroupField.fill(templateInput.group);
    await this.page
      .getByRole("option", { name: templateInput.group, exact: true })
      .click();

    // Leaving the template to its default sends SmartDocuments to its own selection screen.
    const templateField = this.page.getByRole("combobox", {
      name: "Sjabloon",
      exact: true,
    });
    await templateField.click();
    await templateField.fill(templateInput.template);
    await this.page
      .getByRole("option", { name: templateInput.template, exact: true })
      .click();

    const inputTitle = this.page.getByLabel(/Titel/i);
    await inputTitle.fill(documentInput.title);
    await expect(inputTitle).toHaveValue(documentInput.title);

    const inputDescription = this.page.getByLabel(/Beschrijving/i);
    await inputDescription.fill(documentInput.description);
    await expect(inputDescription).toHaveValue(documentInput.description);

    const inputAuthor = this.page.getByLabel(/Auteur/i);
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

    // This locator selects the status message container of the wizard.
    // The status message container is only visible when the wizard has been completed.
    const wizardResultDiv = smartDocumentsWizardPage.locator(
      '[role="status"][aria-live="polite"]',
    );

    await wizardResultDiv.waitFor({ state: "attached" });
    await expect(wizardResultDiv).toBeVisible();

    await expect(wizardResultDiv).toHaveClass(/wizard-result success/);
    await expect(wizardResultDiv.getByText("succes")).toBeVisible();

    // Give ZAC time to store the document and notify the zaak before the wizard tab disappears.
    await smartDocumentsWizardPage.waitForTimeout(FIVE_SECONDS_IN_MS);
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
      timeout: FORTY_SECONDS_IN_MS,
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
