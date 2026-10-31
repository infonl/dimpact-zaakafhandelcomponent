/*
 * SPDX-FileCopyrightText: 2024 Lifely
 * SPDX-License-Identifier: EUPL-1.2+
 */

import { Given, Then, When } from "@cucumber/cucumber";
import { CustomWorld } from "support/worlds/world";

const ONE_MINUTE_IN_MS = 60_000;

let _noOfTaken = 0;

Given(
  "there are at least {int} taken",
  async function (this: CustomWorld, noOfTaken: number) {
    _noOfTaken = noOfTaken;
    const taakCount = await this.page
      .getByLabel("Selecteren", { exact: true })
      .count();
    this.expect(taakCount).toBeGreaterThanOrEqual(noOfTaken);
  },
);

When(
  "{string} selects that number of taken",
  async function (this: CustomWorld, s: string) {
    for (let i = 0; i < _noOfTaken; i++) {
      await this.page
        .getByLabel("Selecteren", { exact: true })
        .first()
        .setChecked(true);
    }
  },
);

When(
  "{string} distributes the taken to the first group available",
  async function (this: CustomWorld, s: string) {
    await this.page.getByTitle("Verdelen").click();
    const expectedLabel = "Taak toekennen aan groep";
    await this.page.getByLabel(expectedLabel).click();
    await this.page.getByRole("option", { name: "test gr" }).first().click();
    await this.page.getByLabel("Reden").fill("Dummy reason");
    await this.page.getByRole("button", { name: /Verdelen/ }).click();
  },
);

When(
  "{string} releases the taken",
  async function (this: CustomWorld, s: string) {
    await this.page.getByTitle("Vrijgeven").click();
    await this.page.getByLabel("Reden").fill("Dummy reason");
    await this.page.getByRole("button", { name: /Vrijgeven/ }).click();
  },
);

Then(
  "{string} gets a message confirming that the distribution of taken is starting",
  { timeout: ONE_MINUTE_IN_MS },
  async function (this: CustomWorld, s: string) {
    await this.page
      .getByText(`${_noOfTaken} taken worden verdeeld...`)
      .waitFor({ timeout: ONE_MINUTE_IN_MS });
  },
);

Then(
  "{string} gets a message confirming that the releasement of taken is starting",
  { timeout: ONE_MINUTE_IN_MS },
  async function (this: CustomWorld, s: string) {
    await this.page
      .getByText(`${_noOfTaken} taken worden vrijgegeven...`)
      .waitFor({ timeout: ONE_MINUTE_IN_MS });
  },
);
