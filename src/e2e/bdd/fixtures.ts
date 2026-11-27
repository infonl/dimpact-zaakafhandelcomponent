/*
 * SPDX-FileCopyrightText: 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

import { createBdd, defineParameterType } from "playwright-bdd";
import { coreTest } from "./_core/fixtures";
import { nameInTarget } from "./_core/names";

export const test = coreTest;

/** A quoted name of data in the kind stack, which the step gets as the name that the target uses for it. */
defineParameterType({
  name: "seeded",
  regexp: /"([^"]*)"/,
  transformer: nameInTarget,
});

export const { Given, When, Then } = createBdd(test);
