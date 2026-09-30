/*
 * SPDX-FileCopyrightText: 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

import type { Query, QueryStatus } from "@tanstack/angular-query-experimental";
import { fromPartial } from "src/test-helpers";
import { shouldDehydrateQuery } from "./should-dehydrate-query";

const createQuery = (url: string, status: QueryStatus) =>
  fromPartial<Query>({ queryKey: [url], state: { status } });

describe(shouldDehydrateQuery.name, () => {
  it.each(["/rest/identity/loggedInUser", "/rest/configuratie/file-types"])(
    "persists a successful %s query",
    (url) => {
      expect(shouldDehydrateQuery(createQuery(url, "success"))).toBe(true);
    },
  );

  it.each<QueryStatus>(["pending", "error"])(
    "does not persist a query with status %s",
    (status) => {
      expect(
        shouldDehydrateQuery(
          createQuery("/rest/identity/loggedInUser", status),
        ),
      ).toBe(false);
    },
  );

  it("does not persist a successful query for an endpoint that is not listed", () => {
    expect(
      shouldDehydrateQuery(createQuery("/rest/zaken/zaak/{uuid}", "success")),
    ).toBe(false);
  });
});
