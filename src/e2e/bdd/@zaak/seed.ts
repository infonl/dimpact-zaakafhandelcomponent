/*
 * SPDX-FileCopyrightText: 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

import type { TestInfo } from "@playwright/test";
import { ENV } from "../../config/env";

/** Data that the seed of the kind stack provides, which the zaak journeys use without naming it in a feature. */
export const SEED = {
  cmmnZaaktype: "Test zaaktype 2",
  /** A zaaktype that the CMMN zaaktype allows as the zaaktype of its deelzaken. */
  cmmnDeelzaaktype: "Test zaaktype 3",
  /** A resultaat that both CMMN zaaktypes have, and that needs no besluit and no date. */
  resultaat: "Geweigerd",
  /** A besluittype of the CMMN zaaktype that accepts documents of the informatieobjecttype "besluit". */
  besluittype: "Besluit aansprakelijkstelling",
};

/** The person of the seed of the target. Skips the scenario when the target has no seed. */
export function seededPersoonOrSkip(testInfo: TestInfo) {
  testInfo.skip(!ENV.seed, `The target "${ENV.target}" has no seed.`);
  // skip() ends the test when the target has no seed, so it exists from here on.
  return ENV.seed!.persoon;
}
