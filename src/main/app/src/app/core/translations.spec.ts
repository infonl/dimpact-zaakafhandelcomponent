/*
 * SPDX-FileCopyrightText: 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

import { readFileSync } from "node:fs";
import { join } from "node:path";
import { isCamelCaseSegment } from "../shared/utils/i18n-key";

const LANGUAGES = ["nl", "en"] as const;

function readKeys(language: (typeof LANGUAGES)[number]) {
  const file = join(__dirname, "../../assets/i18n", `${language}.json`);
  return Object.keys(JSON.parse(readFileSync(file, "utf-8")));
}

describe("the i18n message keys", () => {
  it.each(LANGUAGES)(
    "should write the last segment of every key in %s.json in kebab-case, not camelCase",
    (language) => {
      const camelCaseKeys = readKeys(language).filter((key) =>
        isCamelCaseSegment(key.slice(key.lastIndexOf(".") + 1)),
      );

      expect(camelCaseKeys).toEqual([]);
    },
  );

  it("should hold the same keys in nl.json and en.json, so no language misses a translation", () => {
    expect([...readKeys("en")].sort()).toEqual([...readKeys("nl")].sort());
  });
});
