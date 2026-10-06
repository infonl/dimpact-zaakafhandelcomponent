/*
 * SPDX-FileCopyrightText: 2024 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

import { Given, Then, When } from "@cucumber/cucumber";
import { CustomWorld } from "support/worlds/world";
import { z } from "zod";
import { ONE_MINUTE_IN_MS, TEN_SECONDS_IN_MS } from "../support/time-constants";
import { users } from "../support/worlds/users";
import { worldUsers } from "../utils/schemes";
import { describeTestOriginShort } from "../utils/test-origin";

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
  async function (this: CustomWorld, user: z.infer<typeof worldUsers>) {
    for (let i = 0; i < _noOfZaken; i++) {
      await this.page
        .getByLabel(zaakCheckmarkTitle, { exact: true })
        .first()
        .setChecked(true);
    }
  },
);

When(
  "{string} assigns the zaken to group {string} and user {string}",
  { timeout: TEN_SECONDS_IN_MS },
  async function (
    this: CustomWorld,
    user: z.infer<typeof worldUsers>,
    groupName: string,
    assignee: z.infer<typeof worldUsers>,
  ) {
    await this.page.getByRole("button", { name: /verdelen/i }).click();
    const dialog = this.page.getByRole("dialog");
    await dialog.getByLabel(/groep/i).click();
    await this.page.getByRole("option", { name: groupName }).click();
    await dialog.getByLabel(/medewerker/i).isEnabled();
    await dialog.getByLabel(/medewerker/i).click();
    await this.page
      .getByRole("option", { name: users[worldUsers.parse(assignee)].username })
      .click();
    await dialog
      .getByLabel(/reden/i)
      .fill(describeTestOriginShort(this.browser));
    await dialog.getByRole("button", { name: /verdelen/i }).click();
  },
);

When(
  "{string} releases the zaken",
  { timeout: ONE_MINUTE_IN_MS },
  async function (this: CustomWorld, user: z.infer<typeof worldUsers>) {
    await this.page.getByRole("button", { name: /vrijgeven/i }).click();
    const dialog = this.page.getByRole("dialog");
    await dialog
      .getByLabel(/reden/i)
      .fill(describeTestOriginShort(this.browser));
    await dialog.getByRole("button", { name: /vrijgeven/i }).click();
  },
);

Then(
  "{string} gets a message confirming that the assigning of zaken is starting",
  { timeout: ONE_MINUTE_IN_MS },
  async function (this: CustomWorld, user: z.infer<typeof worldUsers>) {
    await this.page
      .getByText(/\d+ zaken worden verdeeld/)
      .waitFor({ timeout: ONE_MINUTE_IN_MS });
  },
);

Then(
  "{string} gets a message confirming that the releasing of zaken is starting",
  { timeout: ONE_MINUTE_IN_MS },
  async function (this: CustomWorld, user: z.infer<typeof worldUsers>) {
    await this.page
      .getByText(/(\d+ zaken worden vrijgegeven|De zaak wordt vrijgegeven)/)
      .waitFor({ timeout: ONE_MINUTE_IN_MS });
  },
);
