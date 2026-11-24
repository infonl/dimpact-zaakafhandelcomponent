/*
 * SPDX-FileCopyrightText: 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

import type { APIRequestContext, TestInfo } from "@playwright/test";
import { ENV } from "../../config/env";
import type { TestDoubles } from "../../config/targets/target";

type Objecten = TestDoubles["objecten"];

/** The Objecten API of the target. Skips the scenario when the target has none. */
export function objectenOrSkip(testInfo: TestInfo): Objecten {
  testInfo.skip(
    !ENV.testDoubles,
    `The target "${ENV.target}" has no Objecten API for test data.`,
  );
  // skip() ends the test when the target has no test doubles, so they exist from here on.
  return ENV.testDoubles!.objecten;
}

/**
 * Registers a productaanvraag of the type with its aanvraagdocument, as Open Formulieren does. The Objecten API then
 * tells ZAC about it through Open Notificaties.
 */
export async function registerProductaanvraag(
  request: APIRequestContext,
  { apiUrl, token, productaanvraagObjecttype, aanvraagdocument }: Objecten,
  type: string,
) {
  const response = await request.post(`${apiUrl}/objects`, {
    headers: {
      Authorization: `Token ${token}`,
      "Content-Crs": "EPSG:4326",
    },
    data: {
      type: `${apiUrl}/objecttypes/${productaanvraagObjecttype}`,
      record: {
        typeVersion: 1,
        startAt: new Date().toISOString().slice(0, 10),
        data: {
          bron: { naam: "E2E", kenmerk: type },
          type,
          aanvraaggegevens: { aanvraag: { omschrijving: type } },
          taal: "nld",
          pdf: aanvraagdocument.url,
          bijlagen: [],
        },
      },
    },
  });
  if (!response.ok()) {
    throw new Error(
      `The Objecten API returned HTTP ${response.status()}: ${(await response.text()).slice(0, 500)}`,
    );
  }
}
