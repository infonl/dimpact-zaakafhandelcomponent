/*
 * SPDX-FileCopyrightText: 2023 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

import { Then } from "@cucumber/cucumber";
import { z } from "zod";
import { CustomWorld } from "../support/worlds/world";
import { worldUsers } from "../utils/schemes";

const ONE_MINUTE_IN_MS = 60_000;
const FIFTEEN_SECONDS_IN_MS = 15_000;

const TEST_PERSON_HENDRIKA_JANSE_BSN = "999993896";
const TEST_PERSON_HENDRIKA_JANSE_EMAIL = "hendrika.janse@example.com";
const TEST_PERSON_HENDRIKA_JANSE_PHONE_NUMBER = "0612345678";
const TEST_PERSON_CONTACT_MOMENT_FIRST = "e2eTestFirstName of e2eTestLastName";
const TEST_PERSON_CONTACT_MOMENT_SECOND = "e2eFirstName in e2eLastName";

Then(
  "{string} sees the person",
  { timeout: ONE_MINUTE_IN_MS },
  async function (this: CustomWorld, user: z.infer<typeof worldUsers>) {
    await this.expect(
      this.page.getByText(TEST_PERSON_HENDRIKA_JANSE_BSN),
    ).toBeVisible({
      timeout: FIFTEEN_SECONDS_IN_MS,
    });

    await this.expect(
      this.page.getByText(TEST_PERSON_HENDRIKA_JANSE_PHONE_NUMBER),
    ).toBeVisible();
    await this.expect(
      this.page.getByText(TEST_PERSON_HENDRIKA_JANSE_EMAIL),
    ).toBeVisible();

    await this.expect(
      this.page.getByText(TEST_PERSON_CONTACT_MOMENT_FIRST),
    ).toBeVisible();

    await this.expect(
      this.page.getByText(TEST_PERSON_CONTACT_MOMENT_SECOND),
    ).toBeVisible();
  },
);
