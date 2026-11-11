/*
 * SPDX-FileCopyrightText: 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

import type { Page } from "@playwright/test";

const MAX_NAVIGATION_ATTEMPTS = 3;
const NAVIGATION_ABORTED_ERROR = "net::ERR_ABORTED";

// ZAC keeps navigating on its own after a user action (for example when a websocket event reloads the
// zaak), and the browser then aborts a `goto` that starts in the middle of it. Loading a page with a GET is
// safe to repeat, so retry that error only; every other navigation error is a real failure.
export async function gotoWithRetry(
  page: Page,
  url: string,
  options?: Parameters<Page["goto"]>[1],
) {
  for (let attempt = 1; ; attempt++) {
    try {
      return await page.goto(url, options);
    } catch (error) {
      const isAborted =
        error instanceof Error &&
        error.message.includes(NAVIGATION_ABORTED_ERROR);
      if (!isAborted || attempt >= MAX_NAVIGATION_ATTEMPTS) {
        throw error;
      }
      await page.waitForLoadState("load");
    }
  }
}
