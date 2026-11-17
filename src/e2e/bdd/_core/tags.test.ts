/*
 * SPDX-FileCopyrightText: 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

import { expect, test } from "@playwright/test";
import { blockingIssues } from "./tags";

test("given a scenario with @blocked and a Jira key, when its blocking issues are read, then it has that key", () => {
  expect(blockingIssues(["@behandelaar", "@blocked:PZ-1234"])).toEqual([
    "PZ-1234",
  ]);
});

test("given a scenario with @blocked without a Jira key, when its blocking issues are read, then it has none", () => {
  expect(blockingIssues(["@blocked", "@blocked:soon"])).toEqual([]);
});
