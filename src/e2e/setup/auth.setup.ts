/*
 * SPDX-FileCopyrightText: 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

import { expect, test as setup } from "@playwright/test";
import { accountOrSkip } from "../bdd/_core/accounts";
import { sessionFileOfRole } from "../bdd/_core/roles";
import { signIn } from "../bdd/support/keycloak";
import { dashboard } from "../bdd/support/zac";
import { ROLES } from "../config/targets/target";

for (const role of ROLES) {
  setup(`sign in once as ${role}`, async ({ page }, testInfo) => {
    const account = accountOrSkip(role, testInfo);

    await page.goto("/");
    await signIn(page, account);
    await expect(dashboard(page)).toBeVisible();

    await page.context().storageState({ path: sessionFileOfRole(role) });
  });
}
