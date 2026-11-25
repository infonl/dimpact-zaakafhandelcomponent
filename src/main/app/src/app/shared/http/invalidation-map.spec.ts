/*
 * SPDX-FileCopyrightText: 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

import {
  QueryClient,
  type QueryKey,
} from "@tanstack/angular-query-experimental";
import { testQueryClient } from "../../../../setupJest";
import { fromPartial } from "../../../test-helpers";
import { applyRule } from "./invalidation";
import { INVALIDATION_MAP } from "./invalidation-map";

const OTHER_UUID = "fakeOtherUuid";
const UUID = "fakeUuid";

describe("INVALIDATION_MAP", () => {
  function cached(...queryKeys: QueryKey[]) {
    queryKeys.forEach((queryKey) =>
      testQueryClient.setQueryData(queryKey, "fakeData"),
    );
  }

  const isInvalidated = (queryKey: QueryKey) =>
    testQueryClient.getQueryState(queryKey)?.isInvalidated;

  function write(
    endpoint: keyof typeof INVALIDATION_MAP,
    context: { parameters?: unknown; body?: unknown; response?: unknown },
  ) {
    const rule = INVALIDATION_MAP[endpoint] as Parameters<
      typeof applyRule<typeof context>
    >[1];
    applyRule(testQueryClient, rule, context);
  }

  const documentList = (zaakUUID: string) => [
    "/rest/informatieobjecten/informatieobjectenList",
    { zaakUUID, shouldIncludeGekoppeldeZaakDocumenten: false },
  ];
  const historie = (uuid: string) => [
    "/rest/zaken/zaak/{uuid}/historie",
    { path: { uuid } },
  ];

  it("refreshes the documents and historie of only the zaak of a deleted document", () => {
    cached(
      documentList(UUID),
      historie(UUID),
      documentList(OTHER_UUID),
      historie(OTHER_UUID),
    );

    write("DELETE /rest/informatieobjecten/informatieobject/{uuid}", {
      body: { zaakUuid: UUID },
    });

    expect(
      [
        documentList(UUID),
        historie(UUID),
        documentList(OTHER_UUID),
        historie(OTHER_UUID),
      ].map(isInvalidated),
    ).toEqual([true, true, false, false]);
  });

  it("never reads a deleted document again, because that read would fail", () => {
    const document = [
      "/rest/informatieobjecten/informatieobject/{uuid}",
      { path: { uuid: UUID } },
    ];
    cached(document);

    write("DELETE /rest/informatieobjecten/informatieobject/{uuid}", {
      parameters: { path: { uuid: UUID } },
      body: { zaakUuid: OTHER_UUID },
    });

    expect(isInvalidated(document)).toBe(false);
  });

  it("never reads the taak again, because that would overwrite its open form", () => {
    const taak = ["/rest/taken/{taskId}", { path: { taskId: "fakeTaskId" } }];
    cached(taak);

    write("PATCH /rest/taken/toekennen", {
      body: { taakId: "fakeTaskId", zaakUuid: UUID },
    });

    expect(isInvalidated(taak)).toBe(false);
  });

  it("marks searches stale without reading them, because Solr has not committed yet", () => {
    const client = new QueryClient();
    const invalidateQueries = jest.spyOn(client, "invalidateQueries");

    applyRule(
      client,
      INVALIDATION_MAP["PUT /rest/zaken/lijst/verdelen"],
      fromPartial({}),
    );

    expect(invalidateQueries).toHaveBeenCalledWith({
      queryKey: ["/rest/zoeken/list"],
      refetchType: "none",
    });
  });

  it("gives every write a rule that names a read or a reason", () => {
    const empty = Object.entries(INVALIDATION_MAP).filter(
      ([, rule]) => !rule.none && !rule.refresh && !rule.markStale,
    );

    expect(empty).toEqual([]);
  });
});
