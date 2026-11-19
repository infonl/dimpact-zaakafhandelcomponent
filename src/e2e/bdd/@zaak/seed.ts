/*
 * SPDX-FileCopyrightText: 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

import type { TestInfo } from "@playwright/test";
import { ENV } from "../../config/env";

/** Data that the seed of the kind stack provides, which the zaak journeys use without naming it in a feature. */
export const SEED = {
  cmmnZaaktype: "Test zaaktype 2",
};

/** The person of the seed of the target. Skips the scenario when the target has no seed. */
export function seededPersoonOrSkip(testInfo: TestInfo) {
  testInfo.skip(!ENV.seed, `The target "${ENV.target}" has no seed.`);
  // skip() ends the test when the target has no seed, so it exists from here on.
  return ENV.seed!.persoon;
}
