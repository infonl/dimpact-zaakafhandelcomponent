/*
 * SPDX-FileCopyrightText: 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

import { test as setup } from "@playwright/test";
import { saveSessionOf } from "../bdd/_core/sessions";
import { requireZacUrl } from "../config/env";
import { ROLES } from "../config/targets/target";

setup.beforeAll(() => {
  requireZacUrl();
});

for (const role of ROLES) {
  setup(`sign in once as ${role}`, async ({ page }, testInfo) => {
    await saveSessionOf(role, page, testInfo);
  });
}
