/*
 * SPDX-FileCopyrightText: 2025 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

import { Given, Then, When } from "@cucumber/cucumber";
import { expect, type Locator, type Page } from "@playwright/test";
import path from "path";
import { z } from "zod";
import {
  FORTY_SECONDS_IN_MS,
  TEN_SECONDS_IN_MS,
  TWENTY_SECONDS_IN_MS,
} from "../support/time-constants";
import { CustomWorld } from "../support/worlds/world";
import { worldUsers, zaakResult, zaakStatus } from "../utils/schemes";

const E2E_PROCESS_DEFINITION_NAME = "E2E Test BPMN Process Definition";
const E2E_PROCESS_DEFINITION_BPMN_FILE = "E2ETestProcessDefinition.bpmn";
const E2E_PROCESS_DEFINITION_FORM_FILES = [
  "E2EStartForm.json",
  "E2ESelectDocumentsForm.json",
  "E2ESignSelectedDocumentsForm.json",
  "E2ESummaryForm.json",
];

function formioForm(page: Page) {
  return page.locator("zac-formio-wrapper");
}

async function waitForFormioReady(page: Page) {
  const form = formioForm(page);
  await expect(form).toBeVisible({ timeout: FORTY_SECONDS_IN_MS });
  await form
    .locator(".formio-component")
    .first()
    .waitFor({ state: "visible", timeout: FORTY_SECONDS_IN_MS });
}

// Like waitForFormioReady, but also waits for a specific target element
async function waitForFormioContent(page: Page, target: Locator) {
  await waitForFormioReady(page);
  await target.waitFor({ state: "visible", timeout: FORTY_SECONDS_IN_MS });
}

// The default submit button ("Indienen") has no formio-component-submit wrapper to match on.
function submitButton(page: Page) {
  return formioForm(page)
    .getByRole("button")
    .filter({ hasText: /^\s*(Indienen|Selecteren|Ondertekenen)\s*$/ });
}

const SELECT_DOCUMENTS_GRID_KEY = "ZAAK_Documenten_Ondertekenen_Selectie";
const SIGN_DOCUMENTS_GRID_KEY = "ZAAK_Documenten_Te_Ondertekenen";

function documentGrid(page: Page, gridKey: string) {
  return formioForm(page).locator(`.formio-component-${gridKey}`);
}

function documentGridRows(page: Page, gridKey: string) {
  return documentGrid(page, gridKey).locator("tbody tr");
}

// The title sits in a textfield, so it needs an input value rather than a text filter.
async function documentGridRow(page: Page, gridKey: string, title: string) {
  const rows = documentGridRows(page, gridKey);
  await expect
    .poll(() => rows.count(), { timeout: FORTY_SECONDS_IN_MS })
    .toBeGreaterThan(0);
  for (let index = 0; index < (await rows.count()); index++) {
    const row = rows.nth(index);
    if ((await row.getByRole("textbox").first().inputValue()) === title) {
      return row;
    }
  }
  throw new Error(`No row for document "${title}" in datagrid "${gridKey}"`);
}

const e2eTestGroupAId = "test-group-a";
const e2eTestGroupAName = "Test groep A";
const testUser1Id = "e2etestuser1";
const testUser1Name = "E2etest User1";

const COMMUNICATION_CHANNEL_KEY = "E-mail";
const COMMUNICATION_CHANNEL_VALUE = "46";
const RESULT_VALUE = "Verleend";
const STATUS_VALUE = "Afgerond";

When(
  "{string} opens the {string} task",
  { timeout: FORTY_SECONDS_IN_MS },
  async function (
    this: CustomWorld,
    user: z.infer<typeof worldUsers>,
    taskName: string,
  ) {
    const taskRow = this.page.getByRole("row").filter({
      has: this.page.getByRole("cell", { name: taskName, exact: true }),
    });
    const viewTaskLink = taskRow.getByRole("link", { name: "Taak bekijken" });
    await viewTaskLink.waitFor({
      state: "visible",
      timeout: FORTY_SECONDS_IN_MS,
    });
    await viewTaskLink.click();
  },
);

Then(
  "{string} sees the form associated with the task",
  { timeout: FORTY_SECONDS_IN_MS },
  async function (this: CustomWorld, user: z.infer<typeof worldUsers>) {
    await waitForFormioReady(this.page);
    const form = formioForm(this.page);
    await expect(form.getByLabel("Group").nth(0)).toBeVisible();
    await expect(form.getByLabel("User")).toBeVisible();
    await expect(
      form.getByLabel("Smart Documents Template Group"),
    ).toBeVisible();
    await expect(
      form.getByLabel("Smart Documents Template").nth(1),
    ).toBeVisible();
    await expect(form.getByRole("button", { name: "Create" })).toBeVisible();
    await expect(
      form.getByRole("searchbox", { name: "Select one or more documents" }),
    ).toBeVisible();
    await expect(form.getByLabel("Communication channel")).toBeVisible();
  },
);

Given(
  "{string} creates a SmartDocuments Word file named {string}",
  { timeout: FORTY_SECONDS_IN_MS },
  async function (
    this: CustomWorld,
    user: z.infer<typeof worldUsers>,
    fileName: string,
  ) {
    // BPMN form: create a document
    const form = formioForm(this.page);
    await form
      .getByLabel("Smart Documents Template Group")
      .selectOption("OpenZaak", { timeout: TEN_SECONDS_IN_MS });
    await form
      .getByLabel("Smart Documents Template")
      .nth(1)
      .selectOption("Data Test", { timeout: TEN_SECONDS_IN_MS });
    await form.getByRole("button", { name: "Create" }).click();

    // ZAC: Create document sidebar
    await this.page.getByRole("textbox", { name: "Titel" }).click();
    await this.page.getByRole("textbox", { name: "Titel" }).fill(fileName);

    // SmartDocuments wizard
    const smartDocumentsWizardPromise = this.page.waitForEvent("popup");
    await this.page
      .getByRole("button", { name: "Toevoegen", exact: true })
      .click();
    const smartDocumentsWizardPage = await smartDocumentsWizardPromise;
    await smartDocumentsWizardPage
      .getByRole("button", {
        name: /Klaar/i,
      })
      .click();
    const wizardResultDiv = smartDocumentsWizardPage.locator(
      '[role="status"][aria-live="polite"]',
    );
    await expect(wizardResultDiv.getByText("succes")).toBeVisible({
      timeout: FORTY_SECONDS_IN_MS,
    });
    await smartDocumentsWizardPage.close();
  },
);

When(
  "{string} reloads the page",
  { timeout: FORTY_SECONDS_IN_MS },
  async function (this: CustomWorld, user: z.infer<typeof worldUsers>) {
    await this.page.reload();
  },
);

Then(
  "{string} sees document {string} in the documents list",
  { timeout: FORTY_SECONDS_IN_MS },
  async function (
    this: CustomWorld,
    user: z.infer<typeof worldUsers>,
    documentName: string,
  ) {
    const option = this.page.getByRole("option", {
      name: documentName,
      exact: true,
    });
    // A freshly-created SmartDocuments file can take a moment to be indexed
    // in the documents list; reload and re-query until it shows up.
    for (let attempt = 0; attempt < 3; attempt++) {
      await waitForFormioReady(this.page);
      const searchbox = formioForm(this.page).getByRole("searchbox", {
        name: "Select one or more documents",
      });
      await searchbox.click();
      await searchbox.fill(documentName);
      const found = await option
        .waitFor({ state: "visible", timeout: TWENTY_SECONDS_IN_MS })
        .then(() => true)
        .catch(() => false);
      if (found) break;
      await this.page.reload();
    }
    await option.click({ timeout: FORTY_SECONDS_IN_MS });
  },
);

Then(
  "{string} sees the desired form fields values",
  { timeout: FORTY_SECONDS_IN_MS },
  async function (this: CustomWorld, user: z.infer<typeof worldUsers>) {
    await waitForFormioReady(this.page);
    const form = formioForm(this.page);
    await expect(form.getByLabel("Group").nth(0)).toContainText(
      e2eTestGroupAName,
      {
        timeout: FORTY_SECONDS_IN_MS,
      },
    );
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
    await form.getByLabel("Group").nth(0).selectOption(e2eTestGroupAName);
    // User options populate from the Group selection via a backend call;
    // wait for it to return before assuming the specific option exists.
    const userSelect = form.getByLabel("User");
    await expect
      .poll(() => userSelect.locator("option").count(), {
        timeout: FORTY_SECONDS_IN_MS,
      })
      .toBeGreaterThan(1);
    await userSelect.selectOption(testUser1Name);
    const documentsSearchbox = form.getByRole("searchbox", {
      name: "Select one or more documents",
    });
    await documentsSearchbox.click();
    const fileAOption = this.page.getByRole("option", {
      name: "file A",
      exact: true,
    });
    await fileAOption.waitFor({ state: "visible" });
    await fileAOption.click();
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
    await submitButton(this.page).click();
  },
);

Then(
  "{string} sees that the initial task is completed",
  { timeout: FORTY_SECONDS_IN_MS },
  async function (this: CustomWorld, user: z.infer<typeof worldUsers>) {
    // An absent task cell also reads as completed on a zaak page that has yet to render.
    const completedTasksSwitch = this.page.getByRole("switch", {
      name: "Toon afgeronde taken",
    });
    await completedTasksSwitch.waitFor({
      state: "visible",
      timeout: FORTY_SECONDS_IN_MS,
    });

    await expect(
      this.page.getByRole("cell", { name: "Start", exact: true }),
    ).not.toBeVisible({ timeout: FORTY_SECONDS_IN_MS });
    await completedTasksSwitch.click();
    await expect(
      this.page.getByRole("cell", { name: "Start", exact: true }),
    ).toBeVisible({ timeout: FORTY_SECONDS_IN_MS });
  },
);

Then(
  "{string} sees that the summary task is started with group {string} and user {string}",
  { timeout: FORTY_SECONDS_IN_MS },
  async function (
    this: CustomWorld,
    user: z.infer<typeof worldUsers>,
    groupName: string,
    userName: string,
  ) {
    const summaryRow = this.page.getByRole("row").filter({
      has: this.page.getByRole("cell", { name: "Summary", exact: true }),
    });
    await expect(summaryRow).toBeVisible({ timeout: FORTY_SECONDS_IN_MS });
    await expect(
      summaryRow.getByRole("cell", { name: "Toegekend" }),
    ).toBeVisible({ timeout: FORTY_SECONDS_IN_MS });
    await expect(summaryRow.getByRole("cell", { name: groupName })).toBeVisible(
      { timeout: FORTY_SECONDS_IN_MS },
    );
    await expect(
      summaryRow.getByRole("cell", { name: userName, exact: true }),
    ).toBeVisible({ timeout: FORTY_SECONDS_IN_MS });
  },
);

Then(
  "{string} sees that the summary form contains all filled-in data",
  { timeout: FORTY_SECONDS_IN_MS },
  async function (this: CustomWorld, user: z.infer<typeof worldUsers>) {
    const form = formioForm(this.page);
    const groupTextbox = form.getByRole("textbox", { name: "Group" });
    await waitForFormioContent(this.page, groupTextbox);
    await expect(groupTextbox).toHaveValue(e2eTestGroupAId, {
      timeout: FORTY_SECONDS_IN_MS,
    });
    await expect(form.getByRole("textbox", { name: "User" })).toHaveValue(
      testUser1Id,
      { timeout: FORTY_SECONDS_IN_MS },
    );
    await expect(form.getByText("file A")).toBeVisible({
      timeout: FORTY_SECONDS_IN_MS,
    });
    await expect(
      form.getByRole("textbox", { name: "Reference table value" }),
    ).toHaveValue(COMMUNICATION_CHANNEL_VALUE, {
      timeout: FORTY_SECONDS_IN_MS,
    });
    await expect(
      form.getByRole("textbox", { name: "Zaak Result" }),
    ).toHaveValue(RESULT_VALUE, { timeout: FORTY_SECONDS_IN_MS });
    await expect(
      form.getByRole("textbox", { name: "Zaak Status" }),
    ).toHaveValue(STATUS_VALUE, { timeout: FORTY_SECONDS_IN_MS });
  },
);

When(
  "{string} confirms the data in the form",
  { timeout: FORTY_SECONDS_IN_MS },
  async function (this: CustomWorld, user: z.infer<typeof worldUsers>) {
    const confirmButton = formioForm(this.page).getByRole("button", {
      name: "Confirm",
    });
    await waitForFormioContent(this.page, confirmButton);
    await confirmButton.click({ timeout: FORTY_SECONDS_IN_MS });
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
      { timeout: FORTY_SECONDS_IN_MS },
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
    ).toBeVisible({ timeout: FORTY_SECONDS_IN_MS });
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
    const behandelaarField = this.page.getByRole("textbox", {
      name: "zaakBehandelaar",
    });

    // The zaakBehandelaar variable is set slightly after zaakGroep, so the
    // panel can be stale if it was opened before ZAC finished writing it.
    // Retry by closing and reloading rather than waiting longer in place.
    const maxAttempts = 3;
    for (let attempt = 1; attempt <= maxAttempts; attempt++) {
      await this.page.getByRole("button", { name: "Zaakdata" }).click();
      await expect(
        this.page.getByRole("textbox", { name: "zaakGroep" }),
      ).toHaveValue(groupName, { timeout: TEN_SECONDS_IN_MS });

      const isLastAttempt = attempt === maxAttempts;
      if (isLastAttempt) {
        await expect(behandelaarField).toHaveValue(userName, {
          timeout: TEN_SECONDS_IN_MS,
        });
        break;
      }

      const hasValue = await expect(behandelaarField)
        .toHaveValue(userName, { timeout: TEN_SECONDS_IN_MS })
        .then(() => true)
        .catch(() => false);
      if (hasValue) break;

      await this.page
        .locator("mat-toolbar button mat-icon", { hasText: "close" })
        .click();
      await this.page.reload();
    }

    await this.page
      .locator("mat-toolbar button mat-icon", { hasText: "close" })
      .click();
  },
);

Then(
  "{string} sees the select documents to sign form",
  { timeout: FORTY_SECONDS_IN_MS },
  async function (this: CustomWorld, user: z.infer<typeof worldUsers>) {
    await waitForFormioContent(
      this.page,
      documentGrid(this.page, SELECT_DOCUMENTS_GRID_KEY),
    );
    await expect
      .poll(
        () => documentGridRows(this.page, SELECT_DOCUMENTS_GRID_KEY).count(),
        {
          timeout: FORTY_SECONDS_IN_MS,
        },
      )
      .toBeGreaterThan(0);
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
    const row = await documentGridRow(
      this.page,
      SELECT_DOCUMENTS_GRID_KEY,
      documentName,
    );
    await row.getByRole("checkbox").check();
  },
);

Then(
  "{string} sees {int} document\\(s) in the to be signed list",
  { timeout: FORTY_SECONDS_IN_MS },
  async function (
    this: CustomWorld,
    _user: z.infer<typeof worldUsers>,
    expectedCount: number,
  ) {
    await waitForFormioContent(
      this.page,
      documentGrid(this.page, SIGN_DOCUMENTS_GRID_KEY),
    );
    await expect
      .poll(
        () => documentGridRows(this.page, SIGN_DOCUMENTS_GRID_KEY).count(),
        {
          timeout: FORTY_SECONDS_IN_MS,
        },
      )
      .toBe(expectedCount);
  },
);

Then(
  "{string} sees document {string} in the to be signed list",
  { timeout: FORTY_SECONDS_IN_MS },
  async function (
    this: CustomWorld,
    user: z.infer<typeof worldUsers>,
    fileName: string,
  ) {
    await waitForFormioContent(
      this.page,
      documentGrid(this.page, SIGN_DOCUMENTS_GRID_KEY),
    );
    await documentGridRow(this.page, SIGN_DOCUMENTS_GRID_KEY, fileName);
  },
);

When(
  "{string} confirms the signing of the documents",
  { timeout: FORTY_SECONDS_IN_MS },
  async function (this: CustomWorld, user: z.infer<typeof worldUsers>) {
    // Rows in the confirmation grid arrive unticked; ticking is what marks a document for signing.
    const rows = documentGridRows(this.page, SIGN_DOCUMENTS_GRID_KEY);
    for (let index = 0; index < (await rows.count()); index++) {
      await rows.nth(index).getByRole("checkbox").check();
    }
    await submitButton(this.page).click();
  },
);

Then(
  "{string} sees document {string} has been signed",
  { timeout: FORTY_SECONDS_IN_MS },
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

function processDefinitionGroupRow(page: Page, name: string) {
  return page.locator("mat-nested-tree-node.group").filter({ hasText: name });
}

When(
  "{string} uploads the E2E test processdefinition",
  { timeout: FORTY_SECONDS_IN_MS },
  async function (this: CustomWorld, user: z.infer<typeof worldUsers>) {
    const fileChooserPromise = this.page.waitForEvent("filechooser");
    await this.page.getByText("BPMN-procesdefinitie toevoegen").click();
    const fileChooser = await fileChooserPromise;
    await fileChooser.setFiles(
      path.join(__dirname, "../testdata", E2E_PROCESS_DEFINITION_BPMN_FILE),
    );
    await expect(
      processDefinitionGroupRow(this.page, E2E_PROCESS_DEFINITION_NAME),
    ).toBeVisible({ timeout: FORTY_SECONDS_IN_MS });
  },
);

When(
  "{string} uploads all of the forms of the E2E test processdefinition",
  { timeout: FORTY_SECONDS_IN_MS },
  async function (this: CustomWorld, user: z.infer<typeof worldUsers>) {
    const processDefinitionGroup = processDefinitionGroupRow(
      this.page,
      E2E_PROCESS_DEFINITION_NAME,
    );
    const fileChooserPromise = this.page.waitForEvent("filechooser");
    await processDefinitionGroup.getByText("Formulieren uploaden").click();
    const fileChooser = await fileChooserPromise;
    await fileChooser.setFiles(
      E2E_PROCESS_DEFINITION_FORM_FILES.map((filename) =>
        path.join(__dirname, "../testdata", filename),
      ),
    );
  },
);

Then(
  "{string} sees that the processdefinition is correctly setup",
  { timeout: FORTY_SECONDS_IN_MS },
  async function (this: CustomWorld, user: z.infer<typeof worldUsers>) {
    const processDefinitionRowHeader = processDefinitionGroupRow(
      this.page,
      E2E_PROCESS_DEFINITION_NAME,
    ).locator(".tree-group-row");
    await expect(
      processDefinitionRowHeader.locator("mat-icon.cursor-default", {
        hasText: "check_circle",
      }),
    ).toBeVisible({ timeout: FORTY_SECONDS_IN_MS });
  },
);

When(
  "{string} adds a document to the zaak",
  { timeout: FORTY_SECONDS_IN_MS },
  async function (this: CustomWorld, user: z.infer<typeof worldUsers>) {
    await this.page.getByText("Document toevoegen").click();
    await this.page.getByText("Bestandsnaam").click();
    await this.page
      .locator('input[type="file"]')
      .setInputFiles(path.join(__dirname, "../testdata/dent.jpg"));
    await this.page.getByRole("combobox", { name: "Documenttype" }).click();
    await this.page
      .locator(".mat-mdc-select-panel")
      .getByRole("option")
      .first()
      .click();
    await this.page.getByRole("combobox", { name: "Status" }).click();
    await this.page
      .locator(".mat-mdc-select-panel")
      .getByRole("option")
      .first()
      .click();
    await this.page
      .getByRole("button", { name: "Toevoegen", exact: true })
      .click();
  },
);
