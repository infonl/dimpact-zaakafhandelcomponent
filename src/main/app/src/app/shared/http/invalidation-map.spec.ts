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

  it("lists a document that is linked from an inbox in neither inbox", () => {
    const inbox = ["/rest/inboxdocumenten", { page: 0 }];
    const ontkoppeld = ["/rest/ontkoppeldedocumenten", { page: 0 }];
    cached(inbox, ontkoppeld);

    write("POST /rest/informatieobjecten/informatieobject/verplaats", {});

    expect([inbox, ontkoppeld].map(isInvalidated)).toEqual([true, true]);
  });

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

  it("shows signed documents after a taak is completed", () => {
    cached(documentList(UUID));

    write("PATCH /rest/taken/complete", { body: { zaakUuid: UUID } });

    expect(isInvalidated(documentList(UUID))).toBe(true);
  });

  it("never reads the taak again, because that would overwrite its open form", () => {
    const taak = ["/rest/taken/{taskId}", { path: { taskId: "fakeTaskId" } }];
    cached(taak);

    write("PATCH /rest/taken/toekennen", {
      body: { taakId: "fakeTaskId", zaakUuid: UUID },
    });

    expect(isInvalidated(taak)).toBe(false);
  });

  it.each([
    "PUT /rest/informatieobjecten/informatieobject/{uuid}",
    "POST /rest/informatieobjecten/informatieobject/{uuid}/convert",
    "POST /rest/informatieobjecten/informatieobject/{uuid}/onderteken",
  ] as const)(
    "refreshes the document and the documents and historie of its zaak after %s",
    (endpoint) => {
      const document = [
        "/rest/informatieobjecten/informatieobject/{uuid}",
        { path: { uuid: UUID } },
      ];
      cached(document, documentList(OTHER_UUID), historie(OTHER_UUID));

      write(endpoint, {
        parameters: { path: { uuid: UUID }, query: { zaak: OTHER_UUID } },
      });

      expect(
        [document, documentList(OTHER_UUID), historie(OTHER_UUID)].map(
          isInvalidated,
        ),
      ).toEqual([true, true, true]);
    },
  );

  it("refreshes the documents of every zaak that shows the documents of its related zaken, after a document of a zaak changes", () => {
    const linkedDocumentList = [
      "/rest/informatieobjecten/informatieobjectenList",
      { zaakUUID: OTHER_UUID, shouldIncludeGekoppeldeZaakDocumenten: true },
    ];
    cached(linkedDocumentList, documentList(OTHER_UUID));

    write("DELETE /rest/informatieobjecten/informatieobject/{uuid}", {
      body: { zaakUuid: UUID },
    });

    expect(
      [linkedDocumentList, documentList(OTHER_UUID)].map(isInvalidated),
    ).toEqual([true, false]);
  });

  it.each([
    "PUT /rest/informatieobjecten/informatieobject/{uuid}",
    "POST /rest/informatieobjecten/informatieobject/{uuid}/convert",
    "POST /rest/informatieobjecten/informatieobject/{uuid}/onderteken",
  ] as const)(
    "marks the search stale after %s, because the search shows the changed document",
    (endpoint) => {
      const searchResults = ["/rest/zoeken/list", { type: "DOCUMENT" }];
      cached(searchResults);

      write(endpoint, {
        parameters: { path: { uuid: UUID }, query: { zaak: OTHER_UUID } },
      });

      expect(isInvalidated(searchResults)).toBe(true);
    },
  );

  it("offers a saved zaaktype configuration when a zaak is created", () => {
    const zaaktypen = ["/rest/zaaktypen"];
    const zaaktype = [
      "/rest/zaaktypen/{zaaktypeUuid}",
      { path: { zaaktypeUuid: UUID } },
    ];
    cached(zaaktypen, zaaktype);

    write("PUT /rest/zaakafhandelparameters/{zaaktypeUUID}", {
      parameters: { path: { zaaktypeUUID: UUID } },
    });

    expect([zaaktypen, zaaktype].map(isInvalidated)).toEqual([true, true]);
  });

  it("offers an uploaded BPMN process as a procesmodel", () => {
    const options = ["/rest/zaakafhandelparameters/process-definitions"];
    cached(options);

    write("POST /rest/bpmn-process-definitions", {});

    expect(isInvalidated(options)).toBe(true);
  });

  it("offers a new afzender as reply-to, and new values by code", () => {
    const replyTo = ["/rest/zaakafhandelparameters/replyTo"];
    const byCode = [
      "/rest/referentietabellen/code/{code}",
      { path: { code: "AFZENDER" } },
    ];
    cached(replyTo, byCode);

    write("PUT /rest/referentietabellen/{id}", {
      parameters: { path: { id: 1 } },
      body: { code: "AFZENDER" },
    });

    expect([replyTo, byCode].map(isInvalidated)).toEqual([true, true]);
  });

  it("removes a finished plan item from the menu", () => {
    const planItems = [
      "/rest/planitems/zaak/{uuid}/userEventListenerPlanItems",
      { path: { uuid: UUID } },
    ];
    cached(planItems);

    write("POST /rest/planitems/doUserEventListenerPlanItem", {
      body: { zaakUuid: UUID },
    });

    expect(isInvalidated(planItems)).toBe(true);
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
