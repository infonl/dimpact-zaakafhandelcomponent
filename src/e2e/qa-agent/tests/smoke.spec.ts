/*
 * SPDX-FileCopyrightText: 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

import { expect, test } from "@playwright/test";
import { login } from "../helpers";

test("smoke: Bob logs in and sees the dashboard", async ({ page }) => {
  await login(page, "bob");
  await expect(page.getByText("Dashboard").first()).toBeVisible();
});
