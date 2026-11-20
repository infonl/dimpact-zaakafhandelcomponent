/*
 * SPDX-FileCopyrightText: 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

import type { APIRequestContext } from "@playwright/test";
import createClient, { type Client } from "openapi-fetch";
import type { components, paths } from "../../generated/zac-openapi-types";

export type ZacApi = Client<paths>;
export type Schemas = components["schemas"];

const STATUSES_WITHOUT_BODY = [204, 205, 304];

/**
 * A client for the REST API of ZAC, typed by the OpenAPI spec of ZAC. It sends its requests through `request`, so
 * they carry the cookies of the session in `request` and show up in the trace of the test. A request that fails
 * throws, so a step never continues with data that ZAC did not return.
 */
export function zacApiClient(request: APIRequestContext, baseUrl: string) {
  const client = createClient<paths>({
    baseUrl,
    fetch: async (input) => {
      const response = await request.fetch(input.url, {
        method: input.method,
        headers: Object.fromEntries(input.headers),
        data: input.body ? Buffer.from(await input.arrayBuffer()) : undefined,
      });
      const body = STATUSES_WITHOUT_BODY.includes(response.status())
        ? null
        : new Uint8Array(await response.body());
      return new Response(body, {
        status: response.status(),
        headers: response.headers(),
      });
    },
  });
  client.use({
    async onResponse({ request: sentRequest, response }) {
      if (response.ok) return;
      const body = await response.text();
      throw new Error(
        `${sentRequest.method} ${new URL(sentRequest.url).pathname} of the ZAC REST API returned HTTP ` +
          `${response.status}: ${body.slice(0, 500)}`,
      );
    },
  });
  return client;
}

export async function dataOf<Data>(call: Promise<{ data?: Data }>) {
  const { data } = await call;
  if (data === undefined) {
    throw new Error("The ZAC REST API returned no data.");
  }
  return data;
}
