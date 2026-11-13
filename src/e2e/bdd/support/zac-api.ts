/*
 * SPDX-FileCopyrightText: 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

import type { APIRequestContext } from "@playwright/test";

function failureOf(method: string, path: string, status: number, body: string) {
  return new Error(
    `${method} ${path} of the ZAC REST API returned HTTP ${status}: ${body.slice(0, 500)}`,
  );
}

export async function getJson<Body>(api: APIRequestContext, path: string) {
  const response = await api.get(path);
  if (!response.ok()) {
    throw failureOf("GET", path, response.status(), await response.text());
  }
  return (await response.json()) as Body;
}

export async function putJson(
  api: APIRequestContext,
  path: string,
  body: unknown,
) {
  const response = await api.put(path, { data: body });
  if (!response.ok()) {
    throw failureOf("PUT", path, response.status(), await response.text());
  }
}
