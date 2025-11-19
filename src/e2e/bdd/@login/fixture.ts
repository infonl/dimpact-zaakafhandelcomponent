/*
 * SPDX-FileCopyrightText: 2025 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

import { createBdd } from "playwright-bdd";
import { test as base } from "../fixture";

export const test = base;
export const { Given, When, Then } = createBdd(test);
