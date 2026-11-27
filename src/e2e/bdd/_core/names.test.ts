/*
 * SPDX-FileCopyrightText: 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

import { expect, test } from "@playwright/test";
import { nameOf } from "./names";

test("given a name that the target does not translate, when it is looked up, then the target uses the same name", () => {
  expect(nameOf("E-mail", {})).toEqual({ name: "E-mail" });
});

test("given a name that the target translates, when it is looked up, then it is the name in the target", () => {
  expect(nameOf("Toegekend", { Toegekend: "Verleend" })).toEqual({
    name: "Verleend",
  });
});

test("given a name that the target does not have, when it is looked up, then it tells why, so the report shows why the scenario is skipped", () => {
  const missing = { missingBecause: "no zaaktype may be changed" };

  expect(nameOf("Test zaaktype 4", { "Test zaaktype 4": missing })).toEqual(
    missing,
  );
});
