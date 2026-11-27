/*
 * SPDX-FileCopyrightText: 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

import { defineConfig } from "@playwright/test";

/** The unit tests of the suite itself, such as of its configuration. They need no browser and no running stack. */
export default defineConfig({
  testDir: ".",
  testMatch: /(bdd|config|reporting)\/.*\.test\.ts$/,
  reporter: "list",
});
