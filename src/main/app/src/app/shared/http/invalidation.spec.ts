/*
 * SPDX-FileCopyrightText: 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

import { type InvalidationMap, read, search } from "./invalidation";

const UUID = "fakeUuid";

describe("the types of a rule", () => {
  it("are checked by the compiler", () => {
    // @ts-expect-error a typo in the path of a read
    read("/rest/zaken/zaakk/{uuid}");
    // @ts-expect-error a write is not a read
    read("/rest/zaken/zaak/koppel");
    // @ts-expect-error a write over PUT is not a search
    search("/rest/zaken/besluit");
    // @ts-expect-error a parameter that the path does not have
    read("/rest/zaken/zaak/{uuid}", { path: { zaakUuid: UUID } });

    const withoutReason: Partial<InvalidationMap> = {
      // @ts-expect-error a rule without reads needs a reason
      "POST /rest/bag": { none: true },
    };
    const withoutTargets: Partial<InvalidationMap> = {
      // @ts-expect-error a rule without a reason needs a read to refresh or mark stale
      "POST /rest/bag": {},
    };
    const unknownEndpoint: Partial<InvalidationMap> = {
      // @ts-expect-error an endpoint that the API does not have
      "POST /rest/unknown": { none: "fakeReason" },
    };
    // @ts-expect-error a map without a rule for every write
    const incomplete: InvalidationMap = {
      "POST /rest/bag": { none: "fakeReason" },
    };

    expect([
      withoutReason,
      withoutTargets,
      unknownEndpoint,
      incomplete,
    ]).toHaveLength(4);
  });
});
