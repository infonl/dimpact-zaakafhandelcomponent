/*
 * SPDX-FileCopyrightText: 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

import moment from "moment";
import { fromApiDate, toApiDate } from "./index";

describe("date-time", () => {
  describe(toApiDate.name, () => {
    it("gives 2026-07-30 for 30-07-2026 picked as a Date in Europe/Amsterdam", () => {
      expect(toApiDate(new Date(2026, 6, 30))).toBe("2026-07-30");
    });

    it("gives 2026-07-30 for 30-07-2026 picked through the moment date adapter", () => {
      expect(toApiDate(moment([2026, 6, 30]))).toBe("2026-07-30");
    });

    it("gives the local date, not the UTC date, of a moment late in the day", () => {
      expect(toApiDate(new Date(2026, 6, 30, 23, 30))).toBe("2026-07-30");
    });

    it.each([null, undefined])("gives null for %s", (value) => {
      expect(toApiDate(value)).toBeNull();
    });

    it("gives null for an invalid date", () => {
      expect(toApiDate(new Date(Number.NaN))).toBeNull();
      expect(toApiDate(moment.invalid())).toBeNull();
    });
  });

  describe(fromApiDate.name, () => {
    it("reads 2026-07-30 as 30-07-2026", () => {
      expect(fromApiDate("2026-07-30")).toEqual(new Date(2026, 6, 30));
    });

    it("reads the UTC instant 2026-07-29T22:00:00.000Z of an old saved search as 30-07-2026", () => {
      expect(fromApiDate("2026-07-29T22:00:00.000Z")).toEqual(
        new Date(2026, 6, 30),
      );
    });

    it("reads an instant with the local offset as that local date", () => {
      expect(fromApiDate("2026-03-25T00:00:00.000+01:00")).toEqual(
        new Date(2026, 2, 25),
      );
    });

    it.each([null, undefined, "", "not a date"])(
      "gives null for %p",
      (value) => {
        expect(fromApiDate(value)).toBeNull();
      },
    );
  });
});
