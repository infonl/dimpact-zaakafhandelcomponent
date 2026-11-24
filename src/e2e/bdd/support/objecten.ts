/*
 * SPDX-FileCopyrightText: 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

import type { APIRequestContext, TestInfo } from "@playwright/test";
import { ENV } from "../../config/env";
import type { Objecten } from "../../config/targets/target";

/** The Objecten API of the target. Skips the scenario when the target has none. */
export function objectenOrSkip(testInfo: TestInfo): Objecten {
  testInfo.skip(
    !ENV.objecten,
    `The target "${ENV.target}" has no Objecten API for test data.`,
  );
  // skip() ends the test when the target has no Objecten API, so it exists from here on.
  return ENV.objecten!;
}

/**
 * Registers a productaanvraag of the type with its aanvraagdocument, as Open Formulieren does. The Objecten API then
 * tells ZAC about it through Open Notificaties.
 */
export async function registerProductaanvraag(
  request: APIRequestContext,
  {
    apiUrl,
    token,
    productaanvraagObjecttype,
    objecttypeVersion,
    documentenApiUrl,
  }: Objecten,
  { type, documentUuid }: { type: string; documentUuid: string },
) {
  const response = await request.post(`${apiUrl}/objects`, {
    headers: {
      Authorization: `Token ${token}`,
      "Content-Crs": "EPSG:4326",
    },
    data: {
      type: `${apiUrl}/objecttypes/${productaanvraagObjecttype}`,
      record: {
        typeVersion: objecttypeVersion,
        startAt: new Date().toISOString().slice(0, 10),
        data: {
          bron: { naam: "E2E", kenmerk: type },
          type,
          aanvraaggegevens: { aanvraag: { omschrijving: type } },
          taal: "nld",
          pdf: `${documentenApiUrl}/enkelvoudiginformatieobjecten/${documentUuid}`,
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
