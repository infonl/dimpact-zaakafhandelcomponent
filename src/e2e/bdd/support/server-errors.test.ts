/*
 * SPDX-FileCopyrightText: 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

import { expect, test } from "@playwright/test";
import { describeServerError } from "./server-errors";

const response = {
  method: "GET",
  url: "https://zac.example.com/rest/zaken/zaak/15a8fc34-351e-4347-b0e0-d19968d26aca/historie",
  status: 500,
  body: "",
};

test("given a server error of a system behind ZAC, when it is described, then it names that system and leaves out the ids", () => {
  expect(
    describeServerError({
      ...response,
      body: '{"message":"msg.error.server.generic","exception":"Server response from Klanten: 500 (Internal Server Error)"}',
    }),
  ).toBe(
    "Server response from Klanten: 500 (GET /rest/zaken/zaak/{uuid}/historie)",
  );
});

test("given a server error with only a code of ZAC, when it is described, then it names the code", () => {
  expect(
    describeServerError({
      ...response,
      url: "https://zac.example.com/rest/zaken/zaak/id/ZAAK-2026-0000006024",
      body: '{"message":"msg.error.zrc.client.exception"}',
    }),
  ).toBe("msg.error.zrc.client.exception (GET /rest/zaken/zaak/id/{id})");
});

test("given a server error with an unknown text, when it is described, then the text is left out, because the report can be public", () => {
  for (const body of [
    "<html>Bad Gateway</html>",
    '{"message":"fakeName","exception":"fakeSecret"}',
    "null",
  ]) {
    expect(describeServerError({ ...response, status: 502, body })).toBe(
      "HTTP 502 (GET /rest/zaken/zaak/{uuid}/historie)",
    );
  }
});
