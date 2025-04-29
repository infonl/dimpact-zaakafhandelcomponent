/*
 * SPDX-FileCopyrightText: 2021 Atos, 2025 Lifely
 * SPDX-License-Identifier: EUPL-1.2+
 */

import { DatumPipe } from "./datum.pipe";

describe("DatumPipe", () => {
  const pipe = new DatumPipe("nl");

  it.each([
    ["2021-06-23T00:00:00Z", "short", "23\u201106\u20112021 02:00"],
    ["2021-06-22T22:00:00Z", "short", "23\u201106\u20112021 00:00"],
    ["2021-06-23T02:00:00Z", "short", "23\u201106\u20112021 04:00"],
    ["2021-06-22T22:00:00Z", "medium", "23 jun. 2021 00:00"],
    ["2021-06-22T22:00:00Z", "long", "23 juni 2021 00:00"],
    ["2021-06-22T22:00:00Z", "full", "woensdag 23 juni 2021 00:00"],
    ["2021-06-22T22:00:00Z", "shortDate", "23\u201106\u20112021"],
    ["2021-06-22T22:00:00Z", "mediumDate", "23 jun. 2021"],
    ["2021-06-22T22:00:00Z", "longDate", "23 juni 2021"],
    ["2021-06-22T22:00:00Z", "fullDate", "woensdag, 23 juni 2021"],
  ])("Transforms '%s' with format '%s' to '%s'", (input, format, expected) => {
    const result = pipe.transform(input, format as "short");
    expect(result).toEqual(expected);
  });

  describe("default format", () => {
    it("picks the default format", () => {
      const result = pipe.transform("2021-06-22T22:00:00Z");
      expect(result).toEqual("23\u201106\u20112021");
    });
  });
});
