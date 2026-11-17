/*
 * SPDX-FileCopyrightText: 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

import { createBdd } from "playwright-bdd";
import { coreTest } from "./_core/fixtures";

export const test = coreTest;

export const { Given, When, Then } = createBdd(test);
