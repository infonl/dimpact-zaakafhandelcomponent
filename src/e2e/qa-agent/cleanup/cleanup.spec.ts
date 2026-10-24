/*
 * SPDX-FileCopyrightText: 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

import { expect, test } from "@playwright/test";
import { login } from "../helpers";

// ZAC cannot delete a zaak, so the zaken of the QA agent are aborted ("afgebroken"). They then leave the
// werkvoorraden and the dashboard, but stay findable as afgehandelde zaken.
const OMSCHRIJVING_PREFIX = "QA Agent:";
const INADMISSIBLE_TERMINATION_ID = "ZAAK_NIET_ONTVANKELIJK";

type ZaakZoekObject = {
  id: string;
  identificatie: string;
  omschrijving?: string;
  isAfgehandeld?: boolean;
};

test("abort the open zaken created by the QA agent", async ({ page }) => {
  test.setTimeout(10 * 60_000);
  await login(page, "bob");

  const findOpenQaAgentZaken = async () => {
    const response = await page.request.put("/rest/zoeken/list", {
      data: {
        page: 0,
        rows: 200,
        type: "ZAAK",
        zoeken: { ZAAK_OMSCHRIJVING: OMSCHRIJVING_PREFIX },
        alleenOpenstaandeZaken: true,
      },
    });
    expect(response.ok()).toBeTruthy();
    const { resultaten } = (await response.json()) as {
      resultaten: ZaakZoekObject[];
    };
    return resultaten.filter(
      (zaak) =>
        zaak.omschrijving?.startsWith(OMSCHRIJVING_PREFIX) &&
        !zaak.isAfgehandeld,
    );
  };

  const zaken = await findOpenQaAgentZaken();
  console.log(`Found ${zaken.length} open QA Agent zaken`);

  for (const zaak of zaken) {
    // Search results don't carry the zaaktype UUID, so read it from the zaak itself.
    const { zaaktype } = (await (
      await page.request.get(`/rest/zaken/zaak/${zaak.id}`)
    ).json()) as { zaaktype: { uuid: string } };
    const redenen = (await (
      await page.request.get(
        `/rest/zaakafhandelparameters/zaakbeeindigredenen/${zaaktype.uuid}`,
      )
    ).json()) as { id: string; naam: string }[];
    // Prefer a reason managed for the zaaktype; "niet ontvankelijk" only works when the zaaktype has a resultaattype for it.
    const reden =
      redenen.find(({ id }) => id !== INADMISSIBLE_TERMINATION_ID) ??
      redenen[0];
    const response = await page.request.patch(
      `/rest/zaken/zaak/${zaak.id}/afbreken`,
      { data: { zaakbeeindigRedenId: reden?.id } },
    );
    console.log(
      `${zaak.identificatie}: ${response.ok() ? `aborted (${reden?.naam})` : `not aborted, HTTP ${response.status()}`}`,
    );
  }

  // The search index is updated asynchronously, so an aborted zaak can show up as open for a few seconds.
  await expect
    .poll(
      async () =>
        (await findOpenQaAgentZaken()).map(
          ({ identificatie }) => identificatie,
        ),
      { timeout: 60_000, intervals: [5_000] },
    )
    .toEqual([]);
});
