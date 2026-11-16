/*
 * SPDX-FileCopyrightText: 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

import { mergeTests } from "@playwright/test";
import { createBdd } from "playwright-bdd";
import { zaakTest } from "./@zaak/fixtures";
import { coreTest } from "./_core/fixtures";

export const test = mergeTests(coreTest, zaakTest);

export const { Given, When, Then } = createBdd(test);
