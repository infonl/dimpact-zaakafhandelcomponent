/*
 * SPDX-FileCopyrightText: 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

import { expect } from "@playwright/test";
import path from "node:path";
import { Given, Then, When } from "../../fixtures";
import { formioForm } from "../../screens/formio";
import { zaakPage } from "../../screens/zaak";
import type { BpmnProcess } from "../../support/bpmn";
import { dataOf } from "../../support/zac-api";

const FILES_DIRECTORY = path.resolve(__dirname, "../files/bpmn");

const E2E_PROCESS: BpmnProcess = {
  key: "e2eBpmnProces",
  bpmnFile: path.join(FILES_DIRECTORY, "e2eBpmnProces.bpmn"),
  formFiles: [
    path.join(FILES_DIRECTORY, "e2eMultipleFormItemsForm.json"),
    path.join(FILES_DIRECTORY, "e2eMultipleFormItemsSummaryForm.json"),
  ],
};

Given("the E2E process and its forms are deployed", async ({ testData }) => {
  await testData.ensureProcess(E2E_PROCESS);
});

Given(
  "the zaaktype {seeded} uses the E2E process and assigns new zaken to groep {seeded}",
  async ({ testData }, zaaktype: string, groep: string) => {
    await testData.ensureBpmnZaaktype(zaaktype, {
      process: E2E_PROCESS,
      groep,
    });
  },
);

Then("the zaak shows the task {string}", async ({ page }, taak: string) => {
  await expect(zaakPage(page).taak(taak)).toBeVisible();
});

When(
  "I open the task {string} of the zaak",
  async ({ page, testData }, taak: string) => {
    const zaak = zaakPage(page);
    await zaak.open(testData.zaak.identificatie);
    await zaak.taak(taak).getByRole("link", { name: "Taak bekijken" }).click();
    await expect(page.getByRole("heading", { name: taak })).toBeVisible();
  },
);

When(
  "I fill in all required fields of the form, with the resultaat {string} and the status {string}",
  async (
    { page, testData, accountOfRole },
    resultaat: string,
    status: string,
  ) => {
    const form = formioForm(page);
    const { naam } = await testData.groepOf("behandelaar");
    // The users load from the chosen group, so the group goes first.
    await form.choose("Group", naam);
    await form.choose("User", accountOfRole("behandelaar").displayName);
    await form.choose("Select result", resultaat);
    await form.choose("Select status", status);
  },
);

When("I submit the form", async ({ page }) => {
  await formioForm(page).submit("Indienen");
});

Then(
  "the task {string} starts for groep {seeded}",
  async ({ page }, taak: string, groep: string) => {
    await expect(zaakPage(page).taak(taak)).toContainText(groep);
  },
);

Then(
  "the form shows the values that I entered, with the resultaat {string} and the status {string}",
  async (
    { page, testData, accountOfRole },
    resultaat: string,
    status: string,
  ) => {
    const form = formioForm(page);
    const { id } = await testData.groepOf("behandelaar");
    // The form shows the ids of the groep and the user.
    await form.expectValue("Group", id);
    await form.expectValue("User", accountOfRole("behandelaar").username);
    await form.expectValue("Zaak Result", resultaat);
    await form.expectValue("Zaak Status", status);
  },
);

When("I confirm the summary", async ({ page, testData }) => {
  await formioForm(page).submit("Confirm");
  await zaakPage(page).open(testData.zaak.identificatie);
});

// The zaak page does not show the resultaat, so the step reads it from the REST API.
Then(
  "the zaak has the resultaat {string}",
  async ({ apiAs, testData }, resultaat: string) => {
    const api = await apiAs("behandelaar");
    const { identificatie } = testData.zaak;
    const zaak = await dataOf(
      api.GET("/rest/zaken/zaak/id/{identificatie}", {
        params: { path: { identificatie } },
      }),
    );
    expect(zaak.resultaat?.resultaattype?.naam).toBe(resultaat);
  },
);
