/*
 * SPDX-FileCopyrightText: 2024 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

import { Given, Then, When } from "@cucumber/cucumber";
import { z } from "zod";
import { ONE_MINUTE_IN_MS, TEN_SECONDS_IN_MS } from "../support/time-constants";
import { users } from "../support/worlds/users";
import { CustomWorld } from "../support/worlds/world";
import { worldUsers } from "../utils/schemes";
import { assignInDialog, releaseInDialog } from "../utils/verdelen-dialog";
import {
  e2eZaakRows,
  expectAtLeastRows,
  selectFirstRows,
  selectFirstRowsWithBehandelaar,
} from "../utils/werkvoorraad";

let _noOfZaken = 0;

Given(
  "there are at least {int} zaken",
  async function (this: CustomWorld, noOfZaken: number) {
    _noOfZaken = noOfZaken;
    await expectAtLeastRows(e2eZaakRows(this.page), noOfZaken);
  },
);

When(
  "{string} selects that number of zaken",
  async function (this: CustomWorld, user: z.infer<typeof worldUsers>) {
    await selectFirstRows(e2eZaakRows(this.page), _noOfZaken);
  },
);

When(
  "{string} selects that number of zaken that have a behandelaar",
  { timeout: ONE_MINUTE_IN_MS },
  async function (this: CustomWorld, user: z.infer<typeof worldUsers>) {
    await selectFirstRowsWithBehandelaar(
      this.page,
      e2eZaakRows(this.page),
      _noOfZaken,
    );
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
    await assignInDialog(this.page, this.browser, {
      groupName,
      assigneeName: users[worldUsers.parse(assignee)].username,
    });
  },
);

When(
  "{string} releases the zaken",
  { timeout: ONE_MINUTE_IN_MS },
  async function (this: CustomWorld, user: z.infer<typeof worldUsers>) {
    await releaseInDialog(this.page, this.browser);
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
