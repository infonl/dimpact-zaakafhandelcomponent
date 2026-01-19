/*
 * SPDX-FileCopyrightText: 2024 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

import { Given, Then, When } from "@cucumber/cucumber";
import { CustomWorld } from "support/worlds/world";
import { groups } from "../support/worlds/groups";
import { users } from "../support/worlds/users";

const ONE_MINUTE_IN_MS = 60_000;

const zaakCheckmarkTitle = "Selecteren";
let _noOfZaken = 0;

Given(
  "there are at least {int} zaken",
  async function (this: CustomWorld, noOfZaken: number) {
    _noOfZaken = noOfZaken;
    const zaakCount = await this.page
      .getByLabel(zaakCheckmarkTitle, { exact: true })
      .count();
    this.expect(zaakCount).toBeGreaterThanOrEqual(noOfZaken);
  },
);

When(
  "{string} selects that number of zaken",
  async function (this: CustomWorld, s: string) {
    for (let i = 0; i < _noOfZaken; i++) {
      await this.page
        .getByLabel(zaakCheckmarkTitle, { exact: true })
        .first()
        .setChecked(true);
    }
  },
);

When(
  "{string} assigns the zaken to 'Test groep A' and Bob",
  async function (this: CustomWorld, s: string) {
    await this.page.getByRole("button", { name: /verdelen/i }).click();
    await this.page.getByLabel(/groep/i).click();
    await this.page
      .getByRole("option", { name: groups.TestGroupA.name })
      .click();
    await this.page.getByLabel(/medewerker/i).isEnabled();
    await this.page.getByLabel(/medewerker/i).click();
    await this.page.getByRole("option", { name: users.Bob.username }).click();
    await this.page.getByLabel(/reden/i).fill("Fake reason");
    await this.page.getByRole("button", { name: /verdelen/i }).click();
    await this.page.waitForTimeout(10000);
  },
);

When(
  "{string} releases the zaken",
  async function (this: CustomWorld, s: string) {
    await this.page
      .getByRole("button", { name: "Vrijgeven" })
      .locator("span")
      .first()
      .click();

    await this.page.getByLabel("Reden").fill("Fake reason");
    await this.page
      .getByRole("button", { name: /Vrijgeven/ })
      .nth(1)
      .click();
  },
);

Then(
  "{string} gets a message confirming that the assigning of zaken is starting",
  { timeout: ONE_MINUTE_IN_MS },
  async function (this: CustomWorld, s: string) {
    await this.page
      .getByText(/\d+ zaken worden verdeeld/)
      .waitFor({ timeout: ONE_MINUTE_IN_MS });
  },
);

Then(
  "{string} gets a message confirming that the releasing of zaken is starting",
  { timeout: ONE_MINUTE_IN_MS },
  async function (this: CustomWorld, s: string) {
    await this.page
      .getByText(/(\d+ zaken worden vrijgegeven|De zaak wordt vrijgegeven)/)
      .waitFor({ timeout: ONE_MINUTE_IN_MS });
  },
);
