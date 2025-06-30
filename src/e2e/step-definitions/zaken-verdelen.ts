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
  "{string} distributes the zaken to the first group and user available",
  async function (this: CustomWorld, s: string) {
    await this.page.getByRole("button", { name: "Verdelen" }).click();
    await this.page.getByLabel("Zaak toekennen aan groep").click();
    await this.page.getByRole("option").first().click();
    await this.page.getByLabel("Zaak toekennen aan medewerker").click();
    await this.page.getByRole("option").first().click();
    await this.page.getByLabel("Reden").fill("Fake reason");
    await this.page.getByRole("button", { name: /Verdelen/ }).click();
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
  "{string} gets a message confirming that the distribution of zaken is starting",
  { timeout: ONE_MINUTE_IN_MS },
  async function (this: CustomWorld, s: string) {
    await this.page
      .getByText(`${_noOfZaken} zaken worden verdeeld...`)
      .waitFor({ timeout: ONE_MINUTE_IN_MS });
  },
);

Then(
  "{string} gets a message confirming that the releasement of zaken is starting",
  { timeout: ONE_MINUTE_IN_MS },
  async function (this: CustomWorld, s: string) {
    await this.page
      .getByText(
        _noOfZaken > 1
          ? `${_noOfZaken} zaken worden vrijgegeven...`
          : "De zaak wordt vrijgegeven...",
      )
      .waitFor({ timeout: ONE_MINUTE_IN_MS });
  },
);
