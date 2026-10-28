/*
 * SPDX-FileCopyrightText: 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

import { chromium, type FullConfig, type Page } from "@playwright/test";
import fs from "fs";
import { CREATED_ZAKEN_FILE } from "./createZaak";
import { login } from "./globalSetup";

// ZAC can't delete a zaak, so the zaken of the QA agent are aborted ("afgebroken"). They then leave the
// werkvoorraden and the dashboard, but stay findable as afgehandelde zaken.
const OMSCHRIJVING_PREFIX = "QA Agent:";
const INADMISSIBLE_TERMINATION_ID = "ZAAK_NIET_ONTVANKELIJK";

type RestZaak = {
  uuid: string;
  identificatie: string;
  omschrijving: string;
  isOpen: boolean;
  zaaktype: { uuid: string };
};

async function searchOpenQaAgentZaken(page: Page) {
  const response = await page.request.put("/rest/zoeken/list", {
    data: {
      page: 0,
      rows: 200,
      type: "ZAAK",
      zoeken: { ZAAK_OMSCHRIJVING: OMSCHRIJVING_PREFIX },
      alleenOpenstaandeZaken: true,
    },
  });
  if (!response.ok())
    throw new Error(`Searching zaken failed with HTTP ${response.status()}`);
  const { resultaten } = (await response.json()) as {
    resultaten: {
      identificatie: string;
      omschrijving?: string;
      isAfgehandeld?: boolean;
    }[];
  };
  return resultaten
    .filter(
      (zaak) =>
        zaak.omschrijving?.startsWith(OMSCHRIJVING_PREFIX) &&
        !zaak.isAfgehandeld,
    )
    .map(({ identificatie }) => identificatie);
}

function createdZaken() {
  if (!fs.existsSync(CREATED_ZAKEN_FILE)) return [];
  return fs
    .readFileSync(CREATED_ZAKEN_FILE, "utf8")
    .split("\n")
    .map((line) => line.trim())
    .filter((line) => /^ZAAK-\d{4}-\d+$/.test(line));
}

// Returns true when the zaak is closed afterwards. The list of created zaken was written during the agent's
// run, so only open zaken whose omschrijving starts with the QA Agent prefix are touched.
async function abort(page: Page, identificatie: string) {
  const zaakResponse = await page.request.get(
    `/rest/zaken/zaak/id/${identificatie}`,
  );
  if (!zaakResponse.ok()) {
    console.log(`${identificatie}: not found, HTTP ${zaakResponse.status()}`);
    return false;
  }
  const zaak = (await zaakResponse.json()) as RestZaak;
  if (!zaak.omschrijving.startsWith(OMSCHRIJVING_PREFIX)) {
    console.log(`${identificatie}: not a QA Agent zaak, left alone`);
    return true;
  }
  if (!zaak.isOpen) return true;

  const redenen = (await (
    await page.request.get(
      `/rest/zaakafhandelparameters/zaakbeeindigredenen/${zaak.zaaktype.uuid}`,
    )
  ).json()) as { id: string; naam: string }[];
  // Prefer a reason managed for the zaaktype; "niet ontvankelijk" only works when the zaaktype has a resultaattype for it.
  const reden =
    redenen.find(({ id }) => id !== INADMISSIBLE_TERMINATION_ID) ?? redenen[0];
  const response = await page.request.patch(
    `/rest/zaken/zaak/${zaak.uuid}/afbreken`,
    { data: { zaakbeeindigRedenId: reden?.id } },
  );
  console.log(
    `${identificatie}: ${response.ok() ? `aborted (${reden?.naam})` : `not aborted, HTTP ${response.status()}`}`,
  );
  return response.ok();
}

export default async function globalTeardown(config: FullConfig) {
  const { baseURL, locale } = config.projects[0].use;
  const username = process.env.QA_USERNAME;
  const password = process.env.QA_PASSWORD;
  if (!username || !password) throw new Error("No credentials for test user 1");

  const browser = await chromium.launch();
  try {
    const page = await browser.newPage({ baseURL, locale });
    await login(page, username, password);

    // The search index updates asynchronously: a zaak created just before can show up late, and an aborted
    // zaak can still show up as open for a few seconds. So keep searching until nothing new turns up.
    const closed = new Set<string>();
    const failed = new Set<string>();
    for (let attempt = 0; attempt < 12; attempt++) {
      const toAbort = [
        ...new Set([
          ...createdZaken(),
          ...(await searchOpenQaAgentZaken(page)),
        ]),
      ].filter((zaak) => !closed.has(zaak) && !failed.has(zaak));
      if (toAbort.length === 0 && attempt > 0) break;
      for (const zaak of toAbort) {
        if (await abort(page, zaak)) closed.add(zaak);
        else failed.add(zaak);
      }
      await page.waitForTimeout(5_000);
    }

    console.log(
      `Closed ${closed.size} of ${closed.size + failed.size} test zaken, ${failed.size} left.`,
    );
    if (failed.size > 0) {
      throw new Error(`Still open: ${[...failed].join(", ")}`);
    }
  } finally {
    await browser.close();
  }
}
