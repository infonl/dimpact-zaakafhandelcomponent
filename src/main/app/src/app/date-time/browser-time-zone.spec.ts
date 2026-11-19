/**
 * SPDX-FileCopyrightText: 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 *
 * @jest-environment <rootDir>/jest-environment-browser-time-zone.js
 * @browser-time-zone America/New_York
 */

import { fromApiDate, toApiDate, toPickedDate } from "./index";

describe("date-time in a browser outside Europe/Amsterdam", () => {
  it("runs in America/New_York", () => {
    expect(new Date(2026, 0, 30).getTimezoneOffset()).toBe(5 * 60);
  });

  it.each([
    ["2026-01-29T23:00:00.000Z", new Date(2026, 0, 30)],
    ["2026-01-30T00:00:00.000+01:00", new Date(2026, 0, 30)],
    ["2026-07-30T03:30:00.000+05:00", new Date(2026, 6, 30)],
  ])(
    "reads the instant %s as its date in Europe/Amsterdam, not in the browser",
    (value, date) => {
      expect(fromApiDate(value)).toEqual(date);
    },
  );

  it("sends a date with a time that nobody changed back as its date in Europe/Amsterdam", () => {
    expect(toApiDate(toPickedDate("2026-01-29T23:00:00.000Z"))).toBe(
      "2026-01-30",
    );
  });

  it("keeps a date without a time", () => {
    expect(toApiDate(toPickedDate("2026-01-30"))).toBe("2026-01-30");
  });
});
