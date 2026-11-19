/*
 * SPDX-FileCopyrightText: 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

import { expect } from "@playwright/test";
import { randomBytes } from "node:crypto";
import type { Role } from "../../config/targets/target";
import { withLock } from "./lock";
import { setUpZaaktype } from "./zaakafhandelparameters";
import { dataOf, type ZacApi } from "./zac-api";

const SEARCH_INDEX_TIMEOUT_MILLISECONDS = 60_000;

export type RememberedZaak = { identificatie: string };

/**
 * The data of one scenario. `Given` steps make data exist through it, and later steps read back what the scenario
 * made. It hides the REST API of ZAC, the roles that may change the data, the lock that keeps workers apart while
 * they change shared data, and the wait for the search index.
 */
export type TestData = {
  /**
   * A text that no other scenario uses, in this run or another: "E2E", the text and a code of the scenario.
   * Scenarios run in parallel against the same target, so give every description of new data a unique text.
   */
  unique(text: string): string;
  /** Makes sure the zaaktype routes new zaken to the groep. Changes the zaaktype only when it does not yet. */
  ensureZaaktype(
    omschrijving: string,
    settings: { groep: string },
  ): Promise<void>;
  /** Remembers a zaak that the scenario created, for example through the UI, so later steps can find it. */
  rememberZaak(zaak: RememberedZaak): void;
  /** The zaak that the scenario created last. Fails the step when the scenario created no zaak yet. */
  readonly zaak: RememberedZaak;
  /** Waits until the search index of ZAC has the zaak, so lists and werkvoorraden can show it. */
  untilZaakIsIndexed(identificatie: string): Promise<void>;
};

async function isZaakInSearchIndex(api: ZacApi, identificatie: string) {
  const { resultaten } = await dataOf(
    api.PUT("/rest/zoeken/list", {
      body: {
        type: "ZAAK",
        page: 0,
        rows: 10,
        zoeken: { ZAAK_IDENTIFICATIE: identificatie },
      },
    }),
  );
  return (resultaten ?? []).some(
    (zaak) => zaak.identificatie === identificatie,
  );
}

export function createTestData({
  apiAs,
  locksDirectory,
  scenarioCode = randomBytes(4).toString("hex"),
}: {
  apiAs: (role: Role) => Promise<ZacApi>;
  locksDirectory: string;
  scenarioCode?: string;
}): TestData {
  const zaken: RememberedZaak[] = [];

  return {
    unique: (text) =>
      `${text.startsWith("E2E") ? text : `E2E ${text}`} [${scenarioCode}]`,

    async ensureZaaktype(omschrijving, { groep }) {
      const api = await apiAs("beheerder");
      await withLock(locksDirectory, `zaaktype ${omschrijving}`, () =>
        setUpZaaktype(api, omschrijving, groep),
      );
    },

    rememberZaak(zaak) {
      zaken.push(zaak);
    },

    get zaak() {
      const zaak = zaken.at(-1);
      if (!zaak) throw new Error("This scenario has not created a zaak yet.");
      return zaak;
    },

    async untilZaakIsIndexed(identificatie) {
      const api = await apiAs("behandelaar");
      await expect
        .poll(() => isZaakInSearchIndex(api, identificatie), {
          message: `The search index of ZAC has no zaak ${identificatie}.`,
          timeout: SEARCH_INDEX_TIMEOUT_MILLISECONDS,
        })
        .toBe(true);
    },
  };
}
