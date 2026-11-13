/*
 * SPDX-FileCopyrightText: 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

import { createBdd } from "playwright-bdd";
import { zaakTest } from "../@zaak/fixtures";

export const test = zaakTest;

export const { Given, When, Then } = createBdd(test);
