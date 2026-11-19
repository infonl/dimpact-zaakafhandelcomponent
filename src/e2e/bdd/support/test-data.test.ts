/*
 * SPDX-FileCopyrightText: 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

import { expect, test } from "@playwright/test";
import { createTestData } from "./test-data";

function testData(scenarioCode = "a1b2c3d4") {
  return createTestData({
    apiAs: () => Promise.reject(new Error("no REST API in a unit test")),
    locksDirectory: "unused",
    scenarioCode,
  });
}

test("given a text that starts with E2E, when it is made unique, then it ends with the code of the scenario", () => {
  expect(testData().unique("E2E zaak met persoon")).toBe(
    "E2E zaak met persoon [a1b2c3d4]",
  );
});

test("given a text without E2E, when it is made unique, then it starts with E2E so a cleanup can find it", () => {
  expect(testData().unique("zaak in intake")).toBe(
    "E2E zaak in intake [a1b2c3d4]",
  );
});

test("given two scenarios, when they make the same text unique, then the texts differ", () => {
  expect(
    createTestData({
      apiAs: () => Promise.reject(new Error()),
      locksDirectory: "unused",
    }).unique("E2E zaak"),
  ).not.toBe(
    createTestData({
      apiAs: () => Promise.reject(new Error()),
      locksDirectory: "unused",
    }).unique("E2E zaak"),
  );
});

test("given a scenario that created no zaak, when a step asks for its zaak, then the step fails with a clear message", () => {
  expect(() => testData().zaak).toThrow(
    "This scenario has not created a zaak yet.",
  );
});

test("given a scenario that created two zaken, when a step asks for its zaak, then it gets the last one", () => {
  const data = testData();
  data.rememberZaak({ identificatie: "ZAAK-2026-0000000001" });
  data.rememberZaak({ identificatie: "ZAAK-2026-0000000002" });

  expect(data.zaak.identificatie).toBe("ZAAK-2026-0000000002");
});
