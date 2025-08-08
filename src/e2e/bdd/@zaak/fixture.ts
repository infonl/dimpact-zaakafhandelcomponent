/*
 * SPDX-FileCopyrightText: 2025 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

import { test as base } from "../@login/fixture";

export const test = base.extend<{
  caseNumber: { value: string };
}>({
  caseNumber: async ({}, use) => {
    await use({ value: "" });
  },
});
