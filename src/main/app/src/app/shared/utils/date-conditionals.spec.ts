/*
 * SPDX-FileCopyrightText: 2024 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

import { DateConditionals } from "./date-conditionals";

describe(DateConditionals.name, () => {
  describe(DateConditionals.isExceeded.name, () => {
    it.each([
      {
        description:
          "should return true if the control value is 1 day before the actual date",
        value: "2024-01-14T13:00:00.000+01:00",
        actualDate: "2024-01-15T13:00:00.000+01:00",
        expected: true,
      },
      {
        description:
          "should return false if the control value is 1 day after the actual date",
        value: "2024-01-16T13:00:00.000+01:00",
        actualDate: "2024-01-15T13:00:00.000+01:00",
        expected: false,
      },
      {
        description:
          "should return false if the control value is the same day as the actual date",
        value: "2024-01-15T13:00:00.000+01:00",
        actualDate: "2024-01-15T13:00:00.000+01:00",
        expected: false,
      },
      {
        description: "should return false if the control value is null",
        value: null,
        actualDate: "2024-01-15T13:00:00.000+01:00",
        expected: false,
      },
      {
        description: "should return false if the control value is empty string",
        value: "",
        actualDate: "2024-01-15T13:00:00.000+01:00",
        expected: false,
      },
      {
        description:
          "should return true if the control value is 1 week before the actual date",
        value: "2024-01-08T13:00:00.000+01:00",
        actualDate: "2024-01-15T13:00:00.000+01:00",
        expected: true,
      },
      {
        description:
          "should return false if the control value is 1 week after the actual date",
        value: "2024-01-22T13:00:00.000+01:00",
        actualDate: "2024-01-15T13:00:00.000+01:00",
        expected: false,
      },
      {
        description:
          "should return true if the control value is 1 month before the actual date",
        value: "2023-12-15T13:00:00.000+01:00",
        actualDate: "2024-01-15T13:00:00.000+01:00",
        expected: true,
      },
      {
        description:
          "should return false if the control value is 1 month after the actual date",
        value: "2024-02-15T13:00:00.000+01:00",
        actualDate: "2024-01-15T13:00:00.000+01:00",
        expected: false,
      },
      {
        description:
          "should return true if the control value is 1 year before the actual date",
        value: "2023-01-15T13:00:00.000+01:00",
        actualDate: "2024-01-15T13:00:00.000+01:00",
        expected: true,
      },
      {
        description:
          "should return false if the control value is 1 year after the actual date",
        value: "2025-01-15T13:00:00.000+01:00",
        actualDate: "2024-01-15T13:00:00.000+01:00",
        expected: false,
      },
      {
        description:
          "should return false if the control value is hours before the actual date but same day",
        value: "2024-01-15T07:00:00.000+01:00",
        actualDate: "2024-01-15T13:00:00.000+01:00",
        expected: false,
      },
      {
        description:
          "should return true when actual date is not provided (uses current date) and value is in the past",
        value: "2020-01-01T00:00:00Z", // Use a fixed date far in the past
        actualDate: undefined,
        expected: true,
      },
      {
        description:
          "should return false when actual date is not provided and value is in the future",
        value: "2030-01-01T00:00:00Z", // Use a fixed date far in the future
        actualDate: undefined,
        expected: false,
      },
    ])("$description", ({ value, actualDate, expected }) => {
      const result = DateConditionals.isExceeded(value ?? "", actualDate);
      expect(result).toBe(expected);
    });
  });

  describe(DateConditionals.isPreceded.name, () => {
    it.each([
      {
        description:
          "should return false if the control value is 1 day before the actual date",
        value: "2024-01-14T13:00:00.000+01:00",
        actualDate: "2024-01-15T13:00:00.000+01:00",
        expected: false,
      },
      {
        description:
          "should return true if the control value is 1 day after the actual date",
        value: "2024-01-16T13:00:00.000+01:00",
        actualDate: "2024-01-15T13:00:00.000+01:00",
        expected: true,
      },
      {
        description:
          "should return false if the control value is the same day as the actual date",
        value: "2024-01-15T13:00:00.000+01:00",
        actualDate: "2024-01-15T13:00:00.000+01:00",
        expected: false,
      },
      {
        description: "should return false if the control value is null",
        value: null,
        actualDate: "2024-01-15T13:00:00.000+01:00",
        expected: false,
      },
      {
        description: "should return false if the control value is empty string",
        value: "",
        actualDate: "2024-01-15T13:00:00.000+01:00",
        expected: false,
      },
      {
        description:
          "should return false if the control value is 1 week before the actual date",
        value: "2024-01-08T13:00:00.000+01:00",
        actualDate: "2024-01-15T13:00:00.000+01:00",
        expected: false,
      },
      {
        description:
          "should return true if the control value is 1 week after the actual date",
        value: "2024-01-22T13:00:00.000+01:00",
        actualDate: "2024-01-15T13:00:00.000+01:00",
        expected: true,
      },
      {
        description:
          "should return false if the control value is 1 month before the actual date",
        value: "2023-12-15T13:00:00.000+01:00",
        actualDate: "2024-01-15T13:00:00.000+01:00",
        expected: false,
      },
      {
        description:
          "should return true if the control value is 1 month after the actual date",
        value: "2024-02-15T13:00:00.000+01:00",
        actualDate: "2024-01-15T13:00:00.000+01:00",
        expected: true,
      },
      {
        description:
          "should return false if the control value is 1 year before the actual date",
        value: "2023-01-15T13:00:00.000+01:00",
        actualDate: "2024-01-15T13:00:00.000+01:00",
        expected: false,
      },
      {
        description:
          "should return true if the control value is 1 year after the actual date",
        value: "2025-01-15T13:00:00.000+01:00",
        actualDate: "2024-01-15T13:00:00.000+01:00",
        expected: true,
      },
      {
        description:
          "should return false if the control value is hours after the actual date but same day",
        value: "2024-01-15T19:00:00.000+01:00",
        actualDate: "2024-01-15T13:00:00.000+01:00",
        expected: false,
      },
      {
        description:
          "should return false when actual date is not provided (uses current date) and value is in the past",
        value: "2020-01-01T00:00:00Z", // Use a fixed date far in the past
        actualDate: undefined,
        expected: false,
      },
      {
        description:
          "should return true when actual date is not provided and value is in the future",
        value: "2030-01-01T00:00:00Z", // Use a fixed date far in the future
        actualDate: undefined,
        expected: true,
      },
    ])("$description", ({ value, actualDate, expected }) => {
      const result = DateConditionals.isPreceded(value ?? "", actualDate);
      expect(result).toBe(expected);
    });
  });

  describe(DateConditionals.always.name, () => {
    it("meets the condition for any date", () => {
      jest.spyOn(console, "warn").mockImplementation();

      expect(DateConditionals.always("2026-01-30")).toBe(true);
    });
  });
});
