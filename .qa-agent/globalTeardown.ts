/*
 * SPDX-FileCopyrightText: 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

import { chromium, type FullConfig, type Page } from "@playwright/test";
import { login } from "./globalSetup";

// ZAC can't delete a zaak, so the zaken of the QA agent are aborted ("afgebroken"). They then leave the
// werkvoorraden and the dashboard, but stay findable as afgehandelde zaken.
const OMSCHRIJVING_PREFIX = "QA Agent:";
const INADMISSIBLE_TERMINATION_ID = "ZAAK_NIET_ONTVANKELIJK";

type ZaakZoekObject = {
  id: string;
  identificatie: string;
  omschrijving?: string;
  isAfgehandeld?: boolean;
};

async function findOpenQaAgentZaken(page: Page) {
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
    resultaten: ZaakZoekObject[];
  };
  return resultaten.filter(
    (zaak) =>
      zaak.omschrijving?.startsWith(OMSCHRIJVING_PREFIX) && !zaak.isAfgehandeld,
  );
}

async function abort(page: Page, zaak: ZaakZoekObject) {
  // Search results don't carry the zaaktype UUID, so read it from the zaak itself.
  const { zaaktype } = (await (
    await page.request.get(`/rest/zaken/zaak/${zaak.id}`)
  ).json()) as {
    zaaktype: { uuid: string };
  };
  const redenen = (await (
    await page.request.get(
      `/rest/zaakafhandelparameters/zaakbeeindigredenen/${zaaktype.uuid}`,
    )
  ).json()) as { id: string; naam: string }[];
  // Prefer a reason managed for the zaaktype; "niet ontvankelijk" only works when the zaaktype has a resultaattype for it.
  const reden =
    redenen.find(({ id }) => id !== INADMISSIBLE_TERMINATION_ID) ?? redenen[0];
  const response = await page.request.patch(
    `/rest/zaken/zaak/${zaak.id}/afbreken`,
    {
      data: { zaakbeeindigRedenId: reden?.id },
    },
  );
  console.log(
    `${zaak.identificatie}: ${response.ok() ? `aborted (${reden?.naam})` : `not aborted, HTTP ${response.status()}`}`,
  );
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
    const zaken = await findOpenQaAgentZaken(page);
    for (const zaak of zaken) await abort(page, zaak);

    // The search index updates asynchronously, so an aborted zaak can show up as open for a few seconds.
    let remaining = zaken;
    for (let attempt = 0; attempt < 12 && remaining.length > 0; attempt++) {
      await page.waitForTimeout(5_000);
      remaining = await findOpenQaAgentZaken(page);
    }
    console.log(
      `Closed ${zaken.length - remaining.length} of ${zaken.length} test zaken, ${remaining.length} left.`,
    );
    if (remaining.length > 0) {
      throw new Error(
        `Still open: ${remaining.map(({ identificatie }) => identificatie).join(", ")}`,
      );
    }
  } finally {
    await browser.close();
  }
}
