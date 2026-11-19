/*
 * SPDX-FileCopyrightText: 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

import { fromTimestamp, isPickedDate } from "./index";

describe("conversion", () => {
  describe(fromTimestamp.name, () => {
    it("gives the picked date of a timestamp in milliseconds", () => {
      const timestamp = new Date(2026, 6, 30, 10, 15).valueOf();

      const pickedDate = fromTimestamp(timestamp);

      expect(isPickedDate(pickedDate)).toBe(true);
      expect(pickedDate.valueOf()).toBe(timestamp);
    });
  });
});
