/*
 * SPDX-FileCopyrightText: 2021 Atos, 2025 INFO.nl
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

  describe("invalid local dates", () => {
    it.each([
      ["invalid-date-string", "invalid-date-string"],
      ["2021-13-45T25:70:99Z", "2021-13-45T25:70:99Z"],
      ["not-a-date", "not-a-date"],
      ["2021/06/23", "2021/06/23"],
      ["23-06-2021", "23-06-2021"],
      ["", ""],
      ["null", "null"],
      ["undefined", "undefined"],
    ])("Returns original value for invalid date '%s'", (input, expected) => {
      const result = pipe.transform(input);
      expect(result).toEqual(expected);
    });

    it("handles null input", () => {
      const result = pipe.transform(null);
      expect(result).toBeNull();
    });

    it("handles undefined input", () => {
      const result = pipe.transform(undefined);
      expect(result).toBeUndefined();
    });

    it("handles invalid date with format parameter", () => {
      const result = pipe.transform("invalid-date", "short");
      expect(result).toEqual("invalid-date");
    });

    it("handles malformed ISO date string", () => {
      const result = pipe.transform("2021-06-23T25:00:00Z");
      expect(result).toEqual("2021-06-23T25:00:00Z");
    });

    it("handles date with invalid month", () => {
      const result = pipe.transform("2021-13-23T00:00:00Z");
      expect(result).toEqual("2021-13-23T00:00:00Z");
    });

    it("handles date with invalid day", () => {
      const result = pipe.transform("2021-06-32T00:00:00Z");
      expect(result).toEqual("2021-06-32T00:00:00Z");
    });
  });
});
