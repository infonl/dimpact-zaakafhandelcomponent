/*
 * SPDX-FileCopyrightText: 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

import type { APIRequestContext } from "@playwright/test";
import { setTimeout as wait } from "node:timers/promises";
import createClient, { type Client } from "openapi-fetch";
import type { components, paths } from "../../generated/zac-openapi-types";
import { withoutLoggedHeaders } from "./logged-headers";
import type { ServerErrorResponse } from "./server-errors";

export type ZacApi = Client<paths>;
export type Schemas = components["schemas"];

const STATUSES_WITHOUT_BODY = [204, 205, 304];
// A POST or PATCH that failed can have changed something, so only these are safe to send again.
const REPEATABLE_METHODS = ["GET", "PUT"];
const RETRY_DELAYS_MILLISECONDS = [1_000, 2_000, 4_000, 8_000];

/**
 * A client for the REST API of ZAC, typed by the OpenAPI spec of ZAC. It sends its requests through `request`, so
 * they carry the cookies of the session in `request` and show up in the trace of the test. A request that fails
 * throws, so a step never continues with data that ZAC did not return. This client only sets up data and waits for it,
 * so it sends a repeatable request again after a server error. Each server error still goes to `onServerError`.
 */
export function zacApiClient(
  request: APIRequestContext,
  baseUrl: string,
  {
    onServerError = () => undefined,
    retryDelays = RETRY_DELAYS_MILLISECONDS,
  }: {
    onServerError?: (response: ServerErrorResponse) => void;
    retryDelays?: number[];
  } = {},
) {
  const client = createClient<paths>({
    baseUrl,
    fetch: async (input) => {
      const data = input.body
        ? Buffer.from(await input.arrayBuffer())
        : undefined;
      const send = async () => {
        const response = await withoutLoggedHeaders(
          request.fetch(input.url, {
            method: input.method,
            headers: Object.fromEntries(input.headers),
            data,
            maxRedirects: 0,
          }),
        );
        if (response.status() >= 500) {
          onServerError({
            method: input.method,
            url: input.url,
            status: response.status(),
            body: await response.text(),
          });
        }
        return response;
      };

      let response = await send();
      // ZAC forgets its sessions when a new version is deployed, and then redirects to Keycloak before it handles the
      // request. Keycloak still knows the user, so a visit to ZAC gives a new session without a sign in.
      if (response.status() === 302) {
        await withoutLoggedHeaders(request.get(baseUrl));
        response = await send();
      }
      if (REPEATABLE_METHODS.includes(input.method)) {
        for (const delay of retryDelays) {
          if (response.status() < 500) break;
          await wait(delay);
          response = await send();
        }
      }
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
