/*
 * SPDX-FileCopyrightText: 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

import type { TestInfo } from "@playwright/test";
import { ENV } from "../../config/env";
import type { Account, Role } from "../../config/targets/target";

export function accountOrSkip(role: Role, testInfo: TestInfo) {
  const account = ENV.accounts[role];
  testInfo.skip(
    !account,
    `The target "${ENV.target}" has no account for the role ${role}.`,
  );
  // skip() ends the test when the target has no account, so there is one from here on.
  return account as Account;
}
