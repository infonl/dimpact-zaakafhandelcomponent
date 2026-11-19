/*
 * SPDX-FileCopyrightText: 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

import type { TestInfo } from "@playwright/test";

const BLOCKED_TAG = /^@blocked:([A-Z][A-Z0-9]*-\d+)$/;

/** The Jira keys of the `@blocked:<key>` tags, such as `@blocked:PZ-1234`. */
export function blockingIssues(tags: string[]) {
  return tags.flatMap((tag) => BLOCKED_TAG.exec(tag)?.[1] ?? []);
}

/** Skips a scenario that a known issue blocks, with the Jira key as the reason in the report. */
export function skipWhenBlocked(tags: string[], testInfo: TestInfo) {
  const issues = blockingIssues(tags);
  testInfo.skip(issues.length > 0, `Blocked by ${issues.join(", ")}.`);
}
