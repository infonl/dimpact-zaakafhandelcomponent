/*
 * SPDX-FileCopyrightText: 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

import { readFileSync } from "node:fs";
import { join } from "node:path";
import { isI18nKey } from "../shared/utils/i18n-key";

const LANGUAGES = ["nl", "en"] as const;

// These keys only divide the JSON files into sections; no code looks them up.
const SECTION_HEADERS = new Set(
  [...Array.from({ length: 14 }, (_, index) => `${index + 1}`), "7b"].map(
    (section) => ` ${section} `,
  ),
);

function readKeys(language: (typeof LANGUAGES)[number]) {
  const file = join(__dirname, "../../assets/i18n", `${language}.json`);
  return Object.keys(JSON.parse(readFileSync(file, "utf-8")));
}

describe("the i18n message keys", () => {
  it.each(LANGUAGES)(
    "should write every segment of every key in %s.json in lowercase kebab-case",
    (language) => {
      const keysInOtherCasing = readKeys(language).filter(
        (key) => !SECTION_HEADERS.has(key) && !isI18nKey(key),
      );

      expect(keysInOtherCasing).toEqual([]);
    },
  );

  it("should hold the same keys in nl.json and en.json, so no language misses a translation", () => {
    expect([...readKeys("en")].sort()).toEqual([...readKeys("nl")].sort());
  });
});
