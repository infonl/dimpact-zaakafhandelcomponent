/*
 * SPDX-FileCopyrightText: 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

import { I18nKeyPipe } from "./i18n-key.pipe";

describe(I18nKeyPipe.name, () => {
  const pipe = new I18nKeyPipe();

  it("should turn a camelCase column name into its kebab-case i18n key", () => {
    expect(pipe.transform("zaaktypeOmschrijving")).toBe(
      "zaaktype-omschrijving",
    );
  });

  it("should turn a key built from an UPPER_SNAKE enum value into its lowercase kebab-case i18n key", () => {
    expect(pipe.transform("taak.status.NIET_TOEGEKEND")).toBe(
      "taak.status.niet-toegekend",
    );
  });

  it.each([null, undefined, ""])(
    "should turn %p into an empty key, so a missing label translates to nothing",
    (key) => {
      expect(pipe.transform(key)).toBe("");
    },
  );
});
