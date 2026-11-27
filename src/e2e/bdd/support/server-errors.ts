/*
 * SPDX-FileCopyrightText: 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

const UUID = /[0-9a-f]{8}(-[0-9a-f]{4}){3}-[0-9a-f]{12}/g;
const NUMBER_IN_PATH = /\/(ZAAK-)?\d[\d-]*(?=\/|$)/g;
const UPSTREAM = /^Server response from [^:]+: '?\d{3}/;

export const SERVER_ERROR = "server-error";

export type ServerErrorResponse = {
  method: string;
  url: string;
  status: number;
  body: string;
};

function errorOf(body: string): { message?: string; exception?: string } {
  try {
    return (JSON.parse(body) ?? {}) as { message?: string; exception?: string };
  } catch {
    return {};
  }
}

/**
 * Tells what failed, without ids, so the same failure reads the same in every scenario. It takes only known texts from
 * the answer, because the report of a run can be public.
 */
export function describeServerError({
  method,
  url,
  status,
  body,
}: ServerErrorResponse) {
  const { message, exception } = errorOf(body);
  const upstream = exception?.match(UPSTREAM)?.[0].replace("'", "");
  const code = message?.startsWith("msg.error.") ? message : undefined;
  const path = new URL(url).pathname
    .replace(UUID, "{uuid}")
    .replace(NUMBER_IN_PATH, "/{id}");
  return `${upstream ?? code ?? `HTTP ${status}`} (${method} ${path})`;
}
