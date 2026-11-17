/*
 * SPDX-FileCopyrightText: 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

import { fromTimestamp, isPickedDate, toFormDataDateTime } from "./index";

describe("conversion", () => {
  describe(fromTimestamp.name, () => {
    it("gives the picked date of a timestamp in milliseconds", () => {
      const timestamp = new Date(2026, 6, 30, 10, 15).valueOf();

      const pickedDate = fromTimestamp(timestamp);

      expect(isPickedDate(pickedDate)).toBe(true);
      expect(pickedDate.valueOf()).toBe(timestamp);
    });
  });

  describe(toFormDataDateTime.name, () => {
    it("writes the local date and time with the offset, to the minute", () => {
      expect(toFormDataDateTime("2026-07-30T08:15:59.999Z")).toBe(
        "2026-07-30T10:15+02:00",
      );
    });

    it("writes the hour on the 12-hour clock", () => {
      expect(toFormDataDateTime("2026-07-30T13:15:00.000Z")).toBe(
        "2026-07-30T03:15+02:00",
      );
    });
  });
});
