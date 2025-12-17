/*
 * SPDX-FileCopyrightText: 2024 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

import { Given, Then, When } from "@cucumber/cucumber";
import { CustomWorld } from "support/worlds/world";

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
  "{string} assigns the zaken to 'Test groep A' and the first user in this group",
  async function (this: CustomWorld, s: string) {
    await this.page.getByRole("button", { name: /verdelen/i }).click();
    const group = this.page.getByRole("combobox", {
      name: " Groep ",
    });
    // TODO: do not select the first group because that group is not authorised for the e2e test zaaktype
    // instead select 'Test groep A' directly
    await group.fill("Test groep A");
    await group.focus();
    await this.page.getByRole("listbox").first().click();
    //await this.page.getByLabel(/groep/i).click();
    //await this.page.getByRole("option").first().click();
    await this.page.getByLabel(/medewerker/i).isEnabled();
    await this.page.getByLabel(/medewerker/i).click();
    await this.page.getByRole("option").first().click();
    await this.page.getByLabel(/reden/i).fill("Fake reason");
    await this.page.getByRole("button", { name: /verdelen/i }).click();
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

    await this.page.getByRole("button", { name: "Vrijgeven" }).click();

    await this.page.waitForTimeout(3000);
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
