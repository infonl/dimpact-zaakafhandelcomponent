/*
 * SPDX-FileCopyrightText: 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

import moment from "moment";
import {
  addDays,
  daysBetween,
  endOfDay,
  fromApiDate,
  isAfter,
  isPickedDate,
  isSameOrAfter,
  startOfDay,
  toApiDate,
  toApiDateTime,
  today,
  toPickedDate,
} from "./index";

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

    it("reads an instant with the Amsterdam offset as that date", () => {
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

  describe(toPickedDate.name, () => {
    it("reads 2026-07-30 as local midnight of 30-07-2026", () => {
      expect(toPickedDate("2026-07-30")?.valueOf()).toBe(
        new Date(2026, 6, 30).valueOf(),
      );
    });

    it("reads a date with a time as local midnight of its date in Europe/Amsterdam", () => {
      expect(toPickedDate("2026-07-29T22:00:00.000Z")?.valueOf()).toBe(
        new Date(2026, 6, 30).valueOf(),
      );
    });

    it.each([null, undefined, "", "not a date"])(
      "gives null for %p, not the current time",
      (value) => {
        expect(toPickedDate(value)).toBeNull();
      },
    );
  });

  describe(isPickedDate.name, () => {
    it.each([new Date(2026, 6, 30), moment([2026, 6, 30])])(
      "accepts %p",
      (value) => {
        expect(isPickedDate(value)).toBe(true);
      },
    );

    it.each([null, "2026-07-30", 1785369600000])("rejects %p", (value) => {
      expect(isPickedDate(value)).toBe(false);
    });
  });

  describe("day arithmetic", () => {
    it("gives the start of the day", () => {
      expect(startOfDay(new Date(2026, 6, 30, 15, 45)).valueOf()).toBe(
        new Date(2026, 6, 30).valueOf(),
      );
    });

    it("gives the last millisecond of the day", () => {
      expect(endOfDay(new Date(2026, 6, 30, 15, 45)).valueOf()).toBe(
        new Date(2026, 6, 31).valueOf() - 1,
      );
    });

    it("gives the start of today", () => {
      expect(toApiDate(today())).toBe(toApiDate(new Date()));
      expect(today().valueOf()).toBe(startOfDay(new Date()).valueOf());
    });

    it("adds days across the day the clocks go forward", () => {
      expect(toApiDate(addDays("2026-03-28", 2))).toBe("2026-03-30");
    });

    it("adds days across the day the clocks go back", () => {
      expect(toApiDate(addDays("2026-10-24", 2))).toBe("2026-10-26");
    });

    it("counts the whole days between two dates", () => {
      expect(daysBetween("2026-07-30", new Date(2026, 7, 9))).toBe(10);
    });

    it("compares by the millisecond, or by the day", () => {
      const morning = new Date(2026, 6, 30, 9);
      const evening = new Date(2026, 6, 30, 21);

      expect(isAfter(evening, morning)).toBe(true);
      expect(isAfter(evening, morning, "day")).toBe(false);
      expect(isSameOrAfter(morning, evening)).toBe(false);
      expect(isSameOrAfter(morning, evening, "day")).toBe(true);
    });
  });

  describe(toApiDateTime.name, () => {
    it("sends the local time with its offset", () => {
      expect(toApiDateTime(moment.parseZone("2026-07-30T10:15:00+02:00"))).toBe(
        "2026-07-30T10:15:00+02:00",
      );
    });

    it.each([null, undefined])("gives null for %s", (value) => {
      expect(toApiDateTime(value)).toBeNull();
    });
  });
});
