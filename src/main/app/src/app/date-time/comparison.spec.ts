/*
 * SPDX-FileCopyrightText: 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

import { daysSince, isEarlierDay, isInDateRange, isLaterDay } from "./index";

describe("comparison", () => {
  describe(isEarlierDay.name, () => {
    it("compares by the day, not by the time", () => {
      expect(
        isEarlierDay("2026-07-30T07:00:00+02:00", "2026-07-30T19:00:00+02:00"),
      ).toBe(false);
      expect(isEarlierDay("2026-07-29T23:00:00+02:00", "2026-07-30")).toBe(
        true,
      );
    });

    it("reads a typed day as DD-MM-YYYY or MM/DD/YYYY too", () => {
      expect(isEarlierDay("29-07-2026", "2026-07-30")).toBe(true);
      expect(isEarlierDay("07/31/2026", "2026-07-30")).toBe(false);
    });

    it("compares a picked date", () => {
      expect(
        isEarlierDay(new Date(2026, 6, 29, 23), new Date(2026, 6, 30)),
      ).toBe(true);
    });
  });

  describe(isLaterDay.name, () => {
    it("compares by the day, not by the time", () => {
      expect(
        isLaterDay("2026-07-30T19:00:00+02:00", "2026-07-30T07:00:00+02:00"),
      ).toBe(false);
      expect(isLaterDay("2026-07-31T00:30:00+02:00", "2026-07-30")).toBe(true);
    });

    it("reads a typed day as DD-MM-YYYY or MM/DD/YYYY too", () => {
      expect(isLaterDay("31-07-2026", "2026-07-30")).toBe(true);
      expect(isLaterDay("07/29/2026", "2026-07-30")).toBe(false);
    });
  });

  describe(isInDateRange.name, () => {
    it.each([
      ["2026-07-01", true],
      ["2026-07-15", true],
      ["2026-07-31", true],
      ["2026-06-30", false],
      ["2026-08-01", false],
    ])(
      "with both ends, includes the first and the last day: %s gives %s",
      (value, expected) => {
        expect(
          isInDateRange(value, { from: "2026-07-01", through: "2026-07-31" }),
        ).toBe(expected);
      },
    );

    it("with only a start, includes every day from the start", () => {
      expect(isInDateRange("2026-07-01", { from: "2026-07-01" })).toBe(true);
      expect(isInDateRange("2026-06-30", { from: "2026-07-01" })).toBe(false);
    });

    it("with only an end, includes every day through the end", () => {
      expect(isInDateRange("2026-07-31", { through: "2026-07-31" })).toBe(true);
      expect(isInDateRange("2026-08-01", { through: "2026-07-31" })).toBe(
        false,
      );
    });

    it("matches nothing without a start or an end", () => {
      expect(isInDateRange("2026-07-15", { from: null, through: null })).toBe(
        false,
      );
    });
  });

  describe(daysSince.name, () => {
    beforeEach(() => {
      jest.useFakeTimers().setSystemTime(new Date(2026, 6, 30, 12));
    });

    afterEach(() => {
      jest.useRealTimers();
    });

    it("counts the whole days since a moment", () => {
      expect(daysSince("2026-07-20T13:00:00+02:00")).toBe(9);
      expect(daysSince("2026-07-20T11:00:00+02:00")).toBe(10);
    });

    it("counts no days when there is no moment", () => {
      expect(daysSince(undefined)).toBe(0);
    });
  });
});
