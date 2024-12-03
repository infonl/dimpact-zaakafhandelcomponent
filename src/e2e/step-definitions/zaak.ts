/*
 * SPDX-FileCopyrightText: 2023 Lifely
 * SPDX-License-Identifier: EUPL-1.2+
 */

import { Given, Then, When } from "@cucumber/cucumber";
import fs from "fs";
import { z } from "zod";
import { profiles } from "../support/worlds/userProfiles";
import { CustomWorld } from "../support/worlds/world";
import { worldUsers, zaakStatus } from "../utils/schemes";

const ONE_MINUTE_IN_MS = 60_000;
const TWO_MINUTES_IN_MS = 120_000;
const FIFTEEN_SECONDS_IN_MS = 15_000;

const TEST_PERSON_HENDRIKA_JANSE_BSN = "999993896";
const TEST_PERSON_HENDRIKA_JANSE_NAME = "Héndrika Janse";
const TEST_PERSON_HENDRIKA_JANSE_PHONE_NUMBER = "0612345678";

async function checkZaakAssignment(
  this: CustomWorld,
  zaakNumber: any,
  userProfile: any,
) {
  await this.expect(
    this.page
      .getByText(`Aanvullende informatie nodig voor zaak ${zaakNumber}`)
      .first(),
  ).toBeVisible();

  await this.expect(
    this.page.getByRole("cell", {
      name: "Aanvullende informatie",
      exact: true,
    }),
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

Given(
  "Employee {string} is on the newly created zaak with status {string}",
  { timeout: ONE_MINUTE_IN_MS },
  async function (
    this: CustomWorld,
    user: z.infer<typeof worldUsers>,
    status: z.infer<typeof zaakStatus>,
  ) {
    worldUsers.parse(user);
    const caseNumber = this.testStorage.get("caseNumber");

    const parsedStatus = zaakStatus.parse(status);

    await this.page.waitForTimeout(2000);
    await this.page.goto(
      `${this.worldParameters.urls.zac}/zaken/${caseNumber}`,
    );

    await this.expect(
      this.page.getByText(`Status ${parsedStatus}`),
    ).toBeVisible();
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
    const user2Parsed = worldUsers.parse(user2);
    const user2Profile = profiles[user2Parsed];

    await this.page.getByText("Aanvullende informatie").first().click();

    await this.page.getByText("- Kies een e-mailadres -").first().click();
    await this.page
      .getByText("E2etestuser1@team-dimpact.info.nl")
      .first()
      .click();
    await this.page.getByLabel("E-mailadres").first().click();
    await this.page
      .getByLabel("E-mailadres")
      .first()
      .fill("e2e-test@team-dimpact.info.nl");

    await this.page.getByPlaceholder("- Kies een groep -").first().click();
    await this.page
      .getByRole("option", { name: user2Profile.group })
      .first()
      .click();

    await this.page.getByPlaceholder("- Geen behandelaar -").first().click();
    await this.page
      .getByRole("option", { name: user2Profile.username })
      .first()
      .click();
    await this.page.getByRole("button", { name: "Start" }).first().click();

    await this.expect(
      this.page.getByText(
        `Aanvullende informatie nodig voor zaak ${zaakNumber}`,
      ),
    ).toBeVisible({ timeout: FIFTEEN_SECONDS_IN_MS });
    await checkZaakAssignment.call(this, zaakNumber, user2Profile);
  },
);

When(
  "{string} wants to create a new zaak",
  { timeout: ONE_MINUTE_IN_MS },
  async function (this: CustomWorld, user) {
    await this.page.getByLabel("Zaak toevoegen").click();
    await this.page.getByLabel("Zaaktype").click();
    await this.page
      .getByRole("option", { name: "Zaaktype voor e2e testen" })
      .click();
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
      .click();
    await this.page
      .getByPlaceholder("Zoeken op adres, postcode of woonplaats")
      .fill("1112gv");
    await this.page
      .getByPlaceholder("Zoeken op adres, postcode of woonplaats")
      .press("Enter");
    await this.page
      .getByRole("row", {
        name: "Meelbeskamp 49, 1112GV Diemen",
      })
      .getByTitle("Selecteren")
      .click();
    await this.page.getByText("close").click();

    const group = this.page.getByPlaceholder("kies een groep");
    await group.fill("test gr");
    await group.focus();
    await this.page.getByRole("listbox").first().click();

    await this.page.getByLabel("Communicatiekanaal").click();
    await this.page.getByRole("option", { name: " E-mail " }).click();
    // Openbaar should be automatically selected on openbaar
    await this.expect(this.page.getByText("Openbaar").first()).toBeVisible();
    await this.page.getByLabel("Omschrijving").click();
    await this.page.getByLabel("Omschrijving").fill("E2etest1");
    await this.page.getByRole("button", { name: "Aanmaken" }).click();

    const currentYear = new Date().getFullYear();

    // Construct the regex pattern with the current year
    const regexPattern = new RegExp(`ZAAK-${currentYear}-\\d+`, "g");

    await this.expect(this.page.getByText(regexPattern).first()).toBeVisible();

    // Get the HTML content of the page
    const content = await this.page.content();

    // Find all matches
    const matches = content.match(regexPattern);

    if (matches && matches.length > 0) {
      this.testStorage.set("caseNumber", matches[0]);
    } else {
      throw new Error("No case number found");
    }
  },
);

Then(
  "Employee {string} sees the task assigned to them by Employee {string} in the newly created zaak tasks list",
  { timeout: ONE_MINUTE_IN_MS },
  async function (this: CustomWorld, user1: string, _user2: string) {
    const user1Parsed = worldUsers.parse(user1);
    const user1Profile = profiles[user1Parsed];
    const zaakNumber = this.testStorage.get("caseNumber");

    await checkZaakAssignment.call(this, zaakNumber, user1Profile);
  },
);

Then(
  "Employee {string} sees the task assigned to them by Employee {string} in my task list",
  { timeout: TWO_MINUTES_IN_MS },
  async function (this: CustomWorld, user1: string, _user2: string) {
    const user1Parsed = worldUsers.parse(user1);
    const user1Profile = profiles[user1Parsed];

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
  { timeout: ONE_MINUTE_IN_MS },
  async function (this: CustomWorld, user: z.infer<typeof worldUsers>) {
    const caseNumber = this.testStorage.get("caseNumber");

    await this.page
      .getByText(caseNumber)
      .first()
      .waitFor({ timeout: ONE_MINUTE_IN_MS });
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
  "Employee {string} clicks on the first zaak in the zaak-werkvoorraad with delay",
  { timeout: ONE_MINUTE_IN_MS },
  async function (this: CustomWorld, user) {
    await this.page.waitForTimeout(FIFTEEN_SECONDS_IN_MS);
    await this.page.reload();
    await this.page.getByText("visibility").first().click();
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

    const pdf = require("pdf-parse");
    const dataBuffer = fs.readFileSync("./ExportData/" + suggestedFileName);
    await pdf(dataBuffer).then(function (data: any) {
      fs.writeFileSync("./ExportData/actual.txt", data.text);
    });

    let actual_export_values = fs
      .readFileSync("./ExportData/actual.txt", "utf-8")
      .replace(/(\r\n|\n|\r)/gm, "");
    this.expect(actual_export_values).toContain(openFormsTestId);
  },
);
