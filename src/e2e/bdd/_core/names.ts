/*
 * SPDX-FileCopyrightText: 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

import { test } from "@playwright/test";
import { ENV } from "../../config/env";
import type { Names } from "../../config/targets/target";

export function nameOf(
  nameInKind: string,
  names: Names,
): { name: string } | { missingBecause: string } {
  if (!Object.hasOwn(names, nameInKind)) return { name: nameInKind };
  const name = names[nameInKind];
  return typeof name === "string" ? { name } : name;
}

/** What the target calls the name of the kind stack. Skips the scenario when the target does not have it. */
export function nameInTarget(nameInKind: string) {
  const found = nameOf(nameInKind, ENV.names);
  if ("name" in found) return found.name;
  test
    .info()
    .skip(
      true,
      `The target "${ENV.target}" has no "${nameInKind}": ${found.missingBecause}`,
    );
  throw new Error("skip() ends the test.");
}
