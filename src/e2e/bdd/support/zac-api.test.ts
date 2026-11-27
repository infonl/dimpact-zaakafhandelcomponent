/*
 * SPDX-FileCopyrightText: 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

import { expect, test, type APIRequestContext } from "@playwright/test";
import { zacApiClient } from "./zac-api";

const NO_DELAYS = [0, 0];

function requestThatAnswers(...statuses: number[]) {
  const sent: string[] = [];
  const request = {
    get: (url: string) => {
      sent.push(`visit ${url}`);
      return Promise.resolve();
    },
    fetch: (url: string, { method }: { method: string }) => {
      const status = statuses.length > 1 ? statuses.shift() : statuses[0];
      sent.push(`${method} ${new URL(url).pathname}`);
      const body = Buffer.from("[]");
      return Promise.resolve({
        status: () => status,
        headers: () => ({ "content-type": "application/json" }),
        body: () => Promise.resolve(body),
        text: () => Promise.resolve(body.toString()),
      });
    },
  } as unknown as APIRequestContext;
  return { request, sent };
}

test("given a read that fails with a server error once, when it is sent, then it is sent again and the error is still reported", async () => {
  const { request, sent } = requestThatAnswers(500, 200);
  const errors: number[] = [];
  const api = zacApiClient(request, "https://zac.example.com", {
    onServerError: ({ status }) => errors.push(status),
    retryDelays: NO_DELAYS,
  });

  await api.GET("/rest/identity/groups");

  expect(sent).toHaveLength(2);
  expect(errors).toEqual([500]);
});

test("given a session that ZAC forgot, when a request is sent, then it visits ZAC for a new session and sends the request again, also a POST, because ZAC did not handle it", async () => {
  const { request, sent } = requestThatAnswers(302, 200);
  const api = zacApiClient(request, "https://zac.example.com");

  await api.POST("/rest/bpmn-process-definitions", {
    body: { filename: "fake.bpmn", content: "fake" },
  });

  expect(sent).toEqual([
    "POST /rest/bpmn-process-definitions",
    "visit https://zac.example.com",
    "POST /rest/bpmn-process-definitions",
  ]);
});

test("given a read that keeps failing, when it is sent, then it fails after the last retry", async () => {
  const { request, sent } = requestThatAnswers(500);
  const api = zacApiClient(request, "https://zac.example.com", {
    retryDelays: NO_DELAYS,
  });

  await expect(api.GET("/rest/identity/groups")).rejects.toThrow("HTTP 500");
  expect(sent).toHaveLength(3);
});

test("given a request that ZAC refuses, when it is sent, then it is not sent again", async () => {
  const { request, sent } = requestThatAnswers(400);
  const api = zacApiClient(request, "https://zac.example.com", {
    retryDelays: NO_DELAYS,
  });

  await expect(api.GET("/rest/identity/groups")).rejects.toThrow("HTTP 400");
  expect(sent).toHaveLength(1);
});

test("given a POST that fails with a server error, when it is sent, then it is not sent again, because it can have changed something", async () => {
  const { request, sent } = requestThatAnswers(500, 200);
  const api = zacApiClient(request, "https://zac.example.com", {
    retryDelays: NO_DELAYS,
  });

  await expect(
    api.POST("/rest/bpmn-process-definitions", {
      body: { filename: "fake.bpmn", content: "fake" },
    }),
  ).rejects.toThrow("HTTP 500");
  expect(sent).toHaveLength(1);
});
