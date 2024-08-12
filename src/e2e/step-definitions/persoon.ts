/*
 * SPDX-FileCopyrightText: 2023 Lifely
 * SPDX-License-Identifier: EUPL-1.2+
 */

import { Then } from "@cucumber/cucumber";
import { z } from "zod";
import { CustomWorld } from "../support/worlds/world";
import { worldUsers } from "../utils/schemes";

const ONE_MINUTE_IN_MS = 60_000;
const FIFTEEN_SECONDS_IN_MS = 15_000;

Then(
  "{string} sees the person",
  { timeout: ONE_MINUTE_IN_MS },
  async function (this: CustomWorld, user: z.infer<typeof worldUsers>) {
    await this.expect(this.page.getByText(`999993896`)).toBeVisible({
      timeout: FIFTEEN_SECONDS_IN_MS,
    });

    await this.expect(this.page.getByText(`0612345678`)).toBeVisible();

    await this.expect(
      this.page.getByText(`e2eTestFirstName of e2eTestLastName`)
    ).toBeVisible();

    await this.expect(
      this.page.getByText(`e2eFirstName in e2eLastName`)
    ).toBeVisible();
  }
);
