/*
 * SPDX-FileCopyrightText: 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

import { daysFromToday, formatForDisplay } from "./index";

describe("display", () => {
  describe(formatForDisplay.name, () => {
    it.each([
      ["shortDate", "30-07-2026"],
      ["mediumDate", "30 jul. 2026"],
      ["longDate", "30 juli 2026"],
      ["short", "30-07-2026 10:15"],
      ["medium", "30 jul. 2026 10:15"],
      ["long", "30 juli 2026 10:15"],
      ["full", "donderdag 30 juli 2026 10:15"],
      ["fullDate", "donderdag, 30 juli 2026"],
    ] as const)(
      "writes 30-07-2026 10:15 in the %s format of the nl-NL locale as %s",
      (format, expected) => {
        expect(formatForDisplay("2026-07-30T08:15:00Z", format, "nl-NL")).toBe(
          expected,
        );
      },
    );

    it("writes the date in the given locale", () => {
      expect(formatForDisplay(new Date(2026, 6, 30), "longDate", "en")).toBe(
        "July 30, 2026",
      );
    });

    it.each(["not a date", "23-06-2021", "2021-06-32T00:00:00Z"])(
      "gives null for %p, which is not an ISO 8601 date",
      (value) => {
        expect(formatForDisplay(value, "shortDate", "nl-NL")).toBeNull();
      },
    );
  });

  describe(daysFromToday.name, () => {
    beforeEach(() => {
      jest.useFakeTimers().setSystemTime(new Date(2026, 6, 30, 23, 30));
    });

    afterEach(() => {
      jest.useRealTimers();
    });

    it.each([
      ["2026-07-30T00:00:00+02:00", 0],
      ["2026-07-31T00:30:00+02:00", 1],
      ["2026-08-09", 10],
      ["2026-07-28", -2],
    ])("counts the calendar days from today to %s as %d", (value, expected) => {
      expect(daysFromToday(value)).toBe(expected);
    });
  });
});
