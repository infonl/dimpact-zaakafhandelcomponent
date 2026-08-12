/*
 * SPDX-FileCopyrightText: 2023 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

import { Given, Then, When } from "@cucumber/cucumber";
import fs from "fs";
import { PDFParse } from "pdf-parse";
import { z } from "zod";
import {
  FIFTEEN_SECONDS_IN_MS,
  FORTY_SECONDS_IN_MS,
  ONE_MINUTE_IN_MS,
  TWO_MINUTES_IN_MS,
} from "../support/time-constants";
import { users } from "../support/worlds/users";
import { CustomWorld } from "../support/worlds/world";
import { worldUsers, zaakStatus } from "../utils/schemes";

const ZAAK_NUMBER_REGEX = /ZAAK-\d{4}-\d+/;
const ZAAK_DETAIL_URL_REGEX = /\/zaken\/ZAAK-\d{4}-\d+/;

const TEST_PERSON_HENDRIKA_JANSE_BSN = "999993896";
const TEST_PERSON_HENDRIKA_JANSE_NAME = "Héndrika Janse";
const TEST_PERSON_HENDRIKA_JANSE_PHONE_NUMBER = "0612345678";

async function checkZaakAssignment(
  this: CustomWorld,
  zaakNumber: number,
  userProfile: { group: string; username: string },
) {
  await this.expect(
    this.page
      .getByText(`Aanvullende informatie nodig voor zaak ${zaakNumber}`)
      .first(),
  ).toBeVisible();

  await this.expect(
    this.page
      .getByRole("cell", {
        name: "Aanvullende informatie",
      })
      .first(),
  ).toBeVisible();

  await this.expect(
    this.page.getByRole("cell", { name: "Toegekend" }),
  ).toBeVisible();

  await this.expect(
    this.page.getByRole("cell", { name: userProfile.group }),
  ).toBeVisible();

  await this.expect(
    this.page.getByRole("cell", { name: userProfile.username }),
  ).toBeVisible();
}

async function openZaak(this: CustomWorld, user: z.infer<typeof worldUsers>) {
  worldUsers.parse(user);
  const caseNumber = this.testStorage.get("caseNumber");

  await this.page.goto(`${this.worldParameters.urls.zac}/zaken/${caseNumber}`);
}

Given(
  "Employee {string} is on the newly created zaak with status {string}",
  { timeout: ONE_MINUTE_IN_MS },
  async function (
    this: CustomWorld,
    user: z.infer<typeof worldUsers>,
    status: z.infer<typeof zaakStatus>,
  ) {
    await openZaak.call(this, user);

    const parsedStatus = zaakStatus.parse(status);
    await this.expect(
      this.page.getByText(`Status ${parsedStatus}`),
    ).toBeVisible({ timeout: FORTY_SECONDS_IN_MS });
  },
);

Given(
  "Employee {string} is on the newly created zaak",
  { timeout: ONE_MINUTE_IN_MS },
  async function (this: CustomWorld, user: z.infer<typeof worldUsers>) {
    await openZaak.call(this, user);
  },
);

When(
  "Employee {string} does not have enough information to finish Intake and assigns a task to Employee {string}",
  { timeout: TWO_MINUTES_IN_MS },
  async function (
    this: CustomWorld,
    user1: z.infer<typeof worldUsers>,
    user2: z.infer<typeof worldUsers>,
  ) {
    const zaakNumber = this.testStorage.get("caseNumber");
    const user1Parsed = worldUsers.parse(user1);
    const user1Profile = users[user1Parsed];
    const user2Parsed = worldUsers.parse(user2);
    const user2Profile = users[user2Parsed];

    await this.page.getByText("Aanvullende informatie").first().click();

    await this.page
      .locator("mat-label", { hasText: "E-mailadres" })
      .first()
      .fill("e2e-test@team-dimpact.info.nl");

    const expectedDate = new Date();
    expectedDate.setDate(expectedDate.getDate() + 14);
    const expectedDateString = [
      expectedDate.getFullYear(),
      String(expectedDate.getMonth() + 1).padStart(2, "0"),
      String(expectedDate.getDate()).padStart(2, "0"),
    ].join("-");
    await this.expect(this.page.getByLabel("Fatale datum")).toHaveValue(
      expectedDateString,
    );

    await this.expect(
      this.page.getByLabel("Taak toekennen aan groep").first(),
    ).toHaveValue(user1Profile.group);

    await this.page.getByLabel("Taak toekennen aan groep").first().click();
    await this.page
      .getByRole("option", { name: user2Profile.group })
      .first()
      .click();

    await this.page.getByLabel("Taak toekennen aan medewerker").first().click();
    await this.page
      .getByRole("option", { name: user2Profile.username })
      .first()
      .click();
    await this.page.getByRole("button", { name: "Start" }).first().click();

    await this.expect(
      this.page.getByRole("cell", { name: "Aanvullende informatie" }).nth(0),
    ).toBeVisible({ timeout: FIFTEEN_SECONDS_IN_MS });
    await checkZaakAssignment.call(this, zaakNumber, user2Profile);
  },
);

When(
  "Employee {string} assigns the zaak to group {string} and user {string}",
  { timeout: TWO_MINUTES_IN_MS },
  async function (
    this: CustomWorld,
    user: z.infer<typeof worldUsers>,
    groupName: string,
    userName: string,
  ) {
    await this.page
      .getByRole("tabpanel", { name: "Gegevens" })
      .getByRole("button")
      .click();
    await this.page.getByRole("combobox", { name: "Groep" }).click();
    await this.page
      .getByRole("option", { name: groupName, exact: true })
      .click();

    // The users fetched after a group change overwrite any behandelaar picked meanwhile.
    await this.page.getByRole("combobox", { name: "Behandelaar" }).click();
    const userOption = this.page.getByRole("option", {
      name: userName,
      exact: true,
    });
    await userOption.waitFor({
      state: "visible",
      timeout: FORTY_SECONDS_IN_MS,
    });
    await userOption.click();

    await this.page.getByRole("textbox", { name: "Reden" }).fill("test");

    await this.page.getByRole("button", { name: "Opslaan" }).click();

    // The Gegevens panel is redrawn on a websocket event, not on the save response.
    const gegevensPanel = this.page.getByLabel("topic Gegevens");
    await this.expect(gegevensPanel).toContainText(groupName, {
      timeout: FORTY_SECONDS_IN_MS,
    });
    await this.expect(gegevensPanel).toContainText(userName, {
      timeout: FORTY_SECONDS_IN_MS,
    });
  },
);

When(
  "{string} wants to create a new {string} zaak",
  { timeout: ONE_MINUTE_IN_MS },
  async function (
    this: CustomWorld,
    user: z.infer<typeof worldUsers>,
    zaakType: string,
  ) {
    const bpmnZaakType: boolean = zaakType === "BPMN";
    const zaakTypeName: string = bpmnZaakType
      ? "Zaaktype voor BPMN e2e testen"
      : "Zaaktype voor e2e testen";

    await this.page.getByLabel("Zaak toevoegen").click();
    await this.page.getByLabel("Zaaktype").click();
    await this.page.getByRole("option", { name: zaakTypeName }).click();
    await this.page
      .locator("div")
      .filter({ hasText: /^person$/ })
      .click();
    await this.page.getByLabel("BSN").click();
    await this.page.getByLabel("BSN").fill(TEST_PERSON_HENDRIKA_JANSE_BSN);
    await this.page
      .getByLabel("emoji_people Persoon")
      .getByRole("button", { name: "Zoeken" })
      .click();
    await this.page.getByRole("button", { name: "Select" }).click();
    await this.page
      .locator("div")
      .filter({ hasText: /^gps_fixed$/ })
      .click();
    await this.page
      .getByPlaceholder("Zoeken op adres, postcode of woonplaats")
      .fill("1112gv");
    await this.page
      .getByPlaceholder("Zoeken op adres, postcode of woonplaats")
      .press("Enter");
    await this.page
      .getByRole("row", { name: /1112GV/ })
      .first()
      .getByRole("button", { name: "Koppelen" })
      .click();
    await this.page
      .locator("mat-toolbar button mat-icon", { hasText: "close" })
      .click();

    const group = this.page.getByRole("combobox", {
      name: "Zaak toekennen aan groep",
    });
    await group.fill("Test groep A");
    await this.page
      .getByRole("option", { name: "Test groep A", exact: true })
      .click();

    if (bpmnZaakType) {
      const assignToUser = this.page.getByRole("combobox", {
        name: "Zaak toekennen aan medewerker",
      });
      await assignToUser.fill("E2etest User1");
      await this.page
        .getByRole("option", { name: "E2etest User1", exact: true })
        .click();
    }

    await this.page.getByLabel("Communicatiekanaal").click();
    await this.page.getByRole("option", { name: " E-mail " }).click();
    // Openbaar should be automatically selected on openbaar
    await this.expect(this.page.getByText("Openbaar").first()).toBeVisible();
    // A UTC timestamp with millisecond precision tells this zaak apart from every other one on a shared environment.
    const timestampUtc = new Date().toISOString().replace(/[-:.]/g, "");
    const caseDescription = `E2E-test-${timestampUtc}`;
    await this.page.getByLabel("Omschrijving").fill(caseDescription);
    this.testStorage.set("caseDescription", caseDescription);
    await this.page
      .getByLabel("Toelichting")
      .fill(`This task is created by E2E test scenario: ${this.testName}`);

    await this.page.getByRole("button", { name: "Aanmaken" }).click();

    // Creating a zaak routes to /zaken/<zaaknummer>, which names it exactly.
    await this.page.waitForURL(ZAAK_DETAIL_URL_REGEX, {
      timeout: FORTY_SECONDS_IN_MS,
    });
    const [caseNumber] = this.page.url().match(ZAAK_NUMBER_REGEX) ?? [];
    if (!caseNumber) {
      throw new Error(`No case number in url ${this.page.url()}`);
    }
    this.testStorage.set("caseNumber", caseNumber);
  },
);

Then(
  "Employee {string} sees the task assigned to them by Employee {string} in the newly created zaak tasks list",
  { timeout: ONE_MINUTE_IN_MS },
  async function (this: CustomWorld, user1: string, _user2: string) {
    const user1Parsed = worldUsers.parse(user1);
    const user1Profile = users[user1Parsed];
    const zaakNumber = this.testStorage.get("caseNumber");

    await checkZaakAssignment.call(this, zaakNumber, user1Profile);
  },
);

Then(
  "Employee {string} sees the task assigned to them by Employee {string} in my task list",
  { timeout: TWO_MINUTES_IN_MS },
  async function (this: CustomWorld, user1: string, _user2: string) {
    const user1Parsed = worldUsers.parse(user1);
    const user1Profile = users[user1Parsed];

    const caseNumber = this.testStorage.get("caseNumber");

    await this.page.goto(`${this.worldParameters.urls.zac}/taken/mijn`);

    await this.expect(
      this.page.getByRole("cell", { name: caseNumber, exact: true }).first(),
    ).toBeVisible({ timeout: FIFTEEN_SECONDS_IN_MS });

    await this.expect(
      this.page
        .getByRole("cell", { name: "Aanvullende informatie", exact: true })
        .first(),
    ).toBeVisible();

    await this.expect(
      this.page.getByRole("cell", { name: user1Profile.group }).first(),
    ).toBeVisible();
  },
);

Then(
  "{string} sees the created zaak",
  { timeout: TWO_MINUTES_IN_MS },
  async function (this: CustomWorld, user: z.infer<typeof worldUsers>) {
    const caseNumber = this.testStorage.get("caseNumber");
    const caseDescription = this.testStorage.get("caseDescription");
    const zaak = this.page.getByText(caseNumber).first();

    // A zaak shows up only once it has been indexed, where indexing can take some time
    const maxAttempts = 3;
    for (let attempt = 1; attempt <= maxAttempts; attempt++) {
      await this.page.reload();

      const isLastAttempt = attempt === maxAttempts;
      if (isLastAttempt) {
        await zaak.waitFor({
          state: "visible",
          timeout: FIFTEEN_SECONDS_IN_MS,
        });
        break;
      }

      const isVisible = await zaak
        .waitFor({ state: "visible", timeout: FIFTEEN_SECONDS_IN_MS })
        .then(() => true)
        .catch(() => false);
      if (isVisible) break;
    }

    // Both the detail page and the werkvoorraad show the omschrijving of a zaak.
    await this.expect(this.page.getByText(caseDescription).first()).toBeVisible(
      { timeout: FIFTEEN_SECONDS_IN_MS },
    );
  },
);

Then(
  "{string} sees the zaak initiator",
  { timeout: ONE_MINUTE_IN_MS },
  async function (this: CustomWorld, user: z.infer<typeof worldUsers>) {
    await this.page.getByText(TEST_PERSON_HENDRIKA_JANSE_NAME);
    await this.page.getByText(/initiator/i).click();
    await this.expect(
      this.page.getByText(TEST_PERSON_HENDRIKA_JANSE_BSN),
    ).toBeVisible();
    await this.expect(
      this.page.getByText(TEST_PERSON_HENDRIKA_JANSE_PHONE_NUMBER),
    ).toBeVisible();
  },
);

Then(
  "{string} sees the indication that no acknowledgment has been sent",
  { timeout: ONE_MINUTE_IN_MS },
  async function (this: CustomWorld, user: z.infer<typeof worldUsers>) {
    await this.expect(
      this.page.getByRole("option", { name: "Geen bevestiging verstuurd" }),
    ).toBeVisible();
  },
);

Then(
  "Employee {string} clicks on the first zaak in the zaak-werkvoorraad with delay",
  { timeout: TWO_MINUTES_IN_MS },
  async function (this: CustomWorld, user: z.infer<typeof worldUsers>) {
    // Load duration is necessary in order for the zaak that was submitted in open-forms to be
    // registered in ZAC and for its documents to load into the zaak
    await this.page.waitForTimeout(ONE_MINUTE_IN_MS);
    await this.page.reload();
    await this.expect(this.page.getByText("visibility").first()).toBeVisible();

    // The werkvoorraad contains test zaken with zaaknummers of other years, which would be listed
    // first when sorting descending. Filter on the current year to leave those out.
    const currentYearZaakNumberPrefix = `ZAAK-${new Date().getFullYear()}-`;
    const zaakNumberFilter = this.page.locator(
      "th.mat-column-zaak-identificatie_filter input",
    );
    await zaakNumberFilter.fill(currentYearZaakNumberPrefix);
    await zaakNumberFilter.press("Enter");

    // Sorting cycles through no sorting, ascending and descending, so click the column header
    // until the zaak with the highest zaaknummer, being the zaak created last, is listed first.
    const zaakNumberColumnHeader = this.page.getByRole("columnheader", {
      name: "Zaaknummer",
    });
    for (let attempt = 0; attempt < 3; attempt++) {
      const sorting = await zaakNumberColumnHeader.getAttribute("aria-sort");
      if (sorting === "descending") break;
      await zaakNumberColumnHeader.click();
    }
    await this.expect(zaakNumberColumnHeader).toHaveAttribute(
      "aria-sort",
      "descending",
    );

    const newestZaakRow = this.page
      .getByRole("row")
      .filter({ hasText: currentYearZaakNumberPrefix })
      .first();
    await newestZaakRow.getByText("visibility").click();

    await this.expect(this.page).toHaveURL(ZAAK_DETAIL_URL_REGEX);
  },
);

Then(
  "Employee {string} sees the zaak that {string} created in open-forms",
  { timeout: ONE_MINUTE_IN_MS + 30000 },
  async function (this: CustomWorld, user, profile) {
    const openFormsTestId = this.testStorage.get("open-forms-testid");

    await this.page.getByText("plagiarism").nth(1).click();
    await this.expect(
      this.page.getByAltText("Bijgevoegd document"),
    ).toBeVisible();

    await this.page.getByText("more_vert").first().click();
    const [download] = await Promise.all([
      this.page.waitForEvent("download"),
      this.page.getByText("Document downloaden").first().click(),
    ]);

    const suggestedFileName = download.suggestedFilename();
    const filePath = "ExportData/" + suggestedFileName;
    await download.saveAs(filePath);

    const dataBuffer = fs.readFileSync("./ExportData/" + suggestedFileName);
    const parser = new PDFParse({ data: dataBuffer });
    try {
      const pdfText = await parser.getText();
      let actual_export_values = pdfText.text.replace(/(\r\n|\n|\r)/gm, "");
      this.expect(actual_export_values).toContain(openFormsTestId);
    } finally {
      await parser.destroy();
    }
  },
);
