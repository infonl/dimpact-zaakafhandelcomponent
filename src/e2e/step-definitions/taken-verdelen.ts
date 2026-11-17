/*
 * SPDX-FileCopyrightText: 2024 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

import { Given, Then, When } from "@cucumber/cucumber";
import { ONE_MINUTE_IN_MS } from "../support/time-constants";
import { CustomWorld } from "../support/worlds/world";
import { assignInDialog, releaseInDialog } from "../utils/verdelen-dialog";

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
  "{string} assigns the taken to the first group and user available",
  async function (this: CustomWorld, s: string) {
    await assignInDialog(this.page, this.browser);
  },
);

When(
  "{string} releases the taken",
  async function (this: CustomWorld, s: string) {
    await releaseInDialog(this.page, this.browser);
  },
);

Then(
  "{string} gets a message confirming that the assigning of taken is starting",
  { timeout: ONE_MINUTE_IN_MS },
  async function (this: CustomWorld, s: string) {
    await this.page
      .getByText(`${_noOfTaken} taken worden verdeeld...`)
      .waitFor({ timeout: ONE_MINUTE_IN_MS });
  },
);

Then(
  "{string} gets a message confirming that the releasing of taken is starting",
  { timeout: ONE_MINUTE_IN_MS },
  async function (this: CustomWorld, s: string) {
    await this.page
      .getByText(/(\d+ taken worden vrijgegeven|De taak wordt vrijgegeven)/)
      .waitFor({ timeout: ONE_MINUTE_IN_MS });
  },
);
