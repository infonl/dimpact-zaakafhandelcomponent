/*
 * SPDX-FileCopyrightText: 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

import { coreTest } from "../_core/core-fixtures";

export const zaakTest = coreTest.extend<{
  zaakOfScenario: { identificatie?: string; bagAdres?: string };
}>({
  zaakOfScenario: async ({}, use) => {
    await use({});
  },
});
