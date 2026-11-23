/*
 * SPDX-FileCopyrightText: 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

import { expect, test } from "@playwright/test";
import { createTestData } from "./test-data";
import type { ZacApi } from "./zac-api";

const GROEPEN = [
  { id: "behandelaars-test-1", naam: "Test group behandelaars domein test 1" },
  { id: "behandelaars-test-2", naam: "Test group behandelaars domein test 2" },
];

function testDataOfAccountIn(groupIds: string[]) {
  const responses: Record<string, unknown> = {
    "/rest/identity/loggedInUser": { groupIds },
    "/rest/identity/groups": GROEPEN,
  };
  const api = {
    GET: (path: string) => Promise.resolve({ data: responses[path] }),
  } as unknown as ZacApi;
  return createTestData({
    apiAs: () => Promise.resolve(api),
    locksDirectory: "unused",
  });
}

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

test("given an account in one groep, when a step asks for the groep of its role, then it gets that groep", async () => {
  await expect(
    testDataOfAccountIn(["behandelaars-test-2"]).groepOf("behandelaar"),
  ).resolves.toEqual(GROEPEN[1]);
});

test("given an account in two groepen, when a step asks for the groep of its role, then the step fails and names them", async () => {
  await expect(
    testDataOfAccountIn(["behandelaars-test-1", "behandelaars-test-2"]).groepOf(
      "behandelaar",
    ),
  ).rejects.toThrow(
    "The account of the behandelaar is in 2 groepen (behandelaars-test-1, behandelaars-test-2), not in exactly one.",
  );
});

test("given an account in a groep that ZAC does not know, when a step asks for the groep of its role, then the step fails and names it", async () => {
  await expect(
    testDataOfAccountIn(["unknown-group"]).groepOf("behandelaar"),
  ).rejects.toThrow(
    'ZAC has no groep "unknown-group", which the account of the behandelaar is in.',
  );
});

test("given two zaken that the scenario remembered, when it asks for the zaak with an omschrijving, then it gets the zaak with that omschrijving", () => {
  const scenarioData = testData();
  scenarioData.rememberZaak({
    identificatie: "ZAAK-1",
    omschrijving: "E2E hoofdzaak",
  });
  scenarioData.rememberZaak({
    identificatie: "ZAAK-2",
    omschrijving: "E2E deelzaak",
  });

  expect(scenarioData.zaakDescribed("E2E hoofdzaak").identificatie).toBe(
    "ZAAK-1",
  );
});

test("given no zaak with the omschrijving, when the scenario asks for it, then the step fails and names the omschrijving", () => {
  expect(() => testData().zaakDescribed("E2E hoofdzaak")).toThrow(
    'This scenario has not created a zaak "E2E hoofdzaak".',
  );
});

function testDataOfZaakThatAllows(informatieobjecttypen: string[]) {
  const responses: Record<string, unknown> = {
    "/rest/zaken/zaak/id/{identificatie}": { uuid: "zaak-uuid" },
    "/rest/informatieobjecten/informatieobjecttypes/zaak/{zaakUuid}":
      informatieobjecttypen.map((omschrijving) => ({
        uuid: `${omschrijving}-uuid`,
        omschrijving,
      })),
  };
  const api = {
    GET: (path: string) => Promise.resolve({ data: responses[path] }),
    POST: () => Promise.resolve({ data: {} }),
  } as unknown as ZacApi;
  return createTestData({
    apiAs: () => Promise.resolve(api),
    locksDirectory: "unused",
    scenarioCode: "a1b2c3d4",
  });
}

test("given a document that the scenario added, when it asks for the document by its titel, then it gets the unique titel", async () => {
  const scenarioData = testDataOfZaakThatAllows(["besluit"]);
  await scenarioData.addDocument(
    { identificatie: "ZAAK-1" },
    { titel: "E2E besluitbrief", informatieobjecttype: "besluit" },
  );

  expect(scenarioData.documentTitled("E2E besluitbrief")).toBe(
    "E2E besluitbrief [a1b2c3d4]",
  );
});

test("given a zaak that does not allow the informatieobjecttype, when the scenario adds a document of it, then the step fails and names it", async () => {
  await expect(
    testDataOfZaakThatAllows(["bijlage"]).addDocument(
      { identificatie: "ZAAK-1" },
      { titel: "E2E besluitbrief", informatieobjecttype: "besluit" },
    ),
  ).rejects.toThrow(
    'The zaak ZAAK-1 allows no informatieobjecttype "besluit".',
  );
});

test("given no document with the titel, when the scenario asks for it, then the step fails and names the titel", () => {
  expect(() => testData().documentTitled("E2E besluitbrief")).toThrow(
    'This scenario has not added a document "E2E besluitbrief".',
  );
});
