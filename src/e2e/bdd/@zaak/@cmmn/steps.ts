/*
 * SPDX-FileCopyrightText: 2025 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

import { createBdd } from "playwright-bdd";
import { test } from "../fixture";

const { Given } = createBdd(test);

Given("a valid CMMN case exists", async ({}) => {
  console.log("TODO: ensure a CMMN case exists, else make it");
});
