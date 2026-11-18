/*
 * SPDX-FileCopyrightText: 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

import { isEarlierDay, isLaterDay } from "./index";

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
});
