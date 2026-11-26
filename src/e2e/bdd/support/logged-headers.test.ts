/*
 * SPDX-FileCopyrightText: 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

import { expect, test } from "@playwright/test";
import { withoutLoggedHeaders } from "./logged-headers";

const NO_SERVER = "http://127.0.0.1:1/rest/zaken";

test("given a call that gets no answer, when it fails, then its error shows the call but no values of its secret headers", async ({
  playwright,
}) => {
  const request = await playwright.request.newContext();

  const { message, stack } = await withoutLoggedHeaders(
    request.get(NO_SERVER, {
      headers: { cookie: "JSESSIONID=fakeSession", authorization: "fakeToken" },
    }),
  ).then(
    () => new Error("The call got an answer."),
    (reason: unknown) => reason as Error,
  );

  expect(message).toContain(`GET ${NO_SERVER}`);
  expect(message).toContain("- cookie: REDACTED");
  expect(`${message}${stack}`).not.toMatch(/fakeSession|fakeToken/);
  await request.dispose();
});

test("given a call that succeeds, when it ends, then its result is unchanged", async () => {
  await expect(
    withoutLoggedHeaders(Promise.resolve("- cookie: fake")),
  ).resolves.toBe("- cookie: fake");
});
