/*
 * SPDX-FileCopyrightText: 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

import { isRestZaak } from "./is-rest-zaak";

describe("isRestZaak", () => {
  it("accepts a dialog result that carries a zaak", () => {
    expect(isRestZaak({ uuid: "fakeZaakUuid1" })).toBe(true);
  });

  it.each([true, false, undefined, null, {}])(
    "rejects the confirmation-only dialog result %p",
    (value) => {
      expect(isRestZaak(value)).toBe(false);
    },
  );
});
