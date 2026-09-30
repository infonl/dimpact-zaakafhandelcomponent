/*
 * SPDX-FileCopyrightText: 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

import type { Query } from "@tanstack/angular-query-experimental";
import type { Paths } from "./http-client";

const SESSION_STORAGE_PERSISTED_ENDPOINTS: (keyof Paths)[] = [
  "/rest/identity/loggedInUser",
  "/rest/configuratie/file-types",
];

export const shouldDehydrateQuery = ({ queryKey, state }: Query) => {
  const [url] = queryKey;
  if (!url || state.status !== "success") return false;

  return SESSION_STORAGE_PERSISTED_ENDPOINTS.includes(
    String(url) as keyof Paths,
  );
};
