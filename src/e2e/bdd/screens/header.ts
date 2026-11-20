/*
 * SPDX-FileCopyrightText: 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

import type { Page } from "@playwright/test";

export function dashboard(page: Page) {
  return page.getByRole("heading", { name: "Dashboard", level: 1 });
}

export function profileMenuButton(page: Page) {
  return page.getByRole("button", { name: "Gebruikers profiel" });
}
