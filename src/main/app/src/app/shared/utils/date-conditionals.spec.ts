/*
 * SPDX-FileCopyrightText: 2024 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

import moment from "moment";
import { DateConditionals } from "./date-conditionals";

describe(DateConditionals.name, () => {
  describe(DateConditionals.isExceeded.name, () => {
    const now = moment("2024-01-15T12:00:00Z");

    it.each([
      {
        description:
          "should return true if the control value is 1 day before the actual date",
        value: now.clone().subtract(1, "days").toISOString(),
        actualDate: now.toISOString(),
        expected: true,
      },
      {
        description:
          "should return false if the control value is 1 day after the actual date",
        value: now.clone().add(1, "days").toISOString(),
        actualDate: now.toISOString(),
        expected: false,
      },
      {
        description:
          "should return false if the control value is the same day as the actual date",
        value: now.toISOString(),
        actualDate: now.toISOString(),
        expected: false,
      },
      {
        description: "should return false if the control value is null",
        value: null,
        actualDate: now.toISOString(),
        expected: false,
      },
      {
        description: "should return false if the control value is empty string",
        value: "",
        actualDate: now.toISOString(),
        expected: false,
      },
      {
        description:
          "should return true if the control value is 1 week before the actual date",
        value: now.clone().subtract(1, "weeks").toISOString(),
        actualDate: now.toISOString(),
        expected: true,
      },
      {
        description:
          "should return false if the control value is 1 week after the actual date",
        value: now.clone().add(1, "weeks").toISOString(),
        actualDate: now.toISOString(),
        expected: false,
      },
      {
        description:
          "should return true if the control value is 1 month before the actual date",
        value: now.clone().subtract(1, "months").toISOString(),
        actualDate: now.toISOString(),
        expected: true,
      },
      {
        description:
          "should return false if the control value is 1 month after the actual date",
        value: now.clone().add(1, "months").toISOString(),
        actualDate: now.toISOString(),
        expected: false,
      },
      {
        description:
          "should return true if the control value is 1 year before the actual date",
        value: now.clone().subtract(1, "years").toISOString(),
        actualDate: now.toISOString(),
        expected: true,
      },
      {
        description:
          "should return false if the control value is 1 year after the actual date",
        value: now.clone().add(1, "years").toISOString(),
        actualDate: now.toISOString(),
        expected: false,
      },
      {
        description:
          "should return false if the control value is hours before the actual date but same day",
        value: now.clone().subtract(6, "hours").toISOString(),
        actualDate: now.toISOString(),
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
    const now = moment("2024-01-15T12:00:00Z");

    it.each([
      {
        description:
          "should return false if the control value is 1 day before the actual date",
        value: now.clone().subtract(1, "days").toISOString(),
        actualDate: now.toISOString(),
        expected: false,
      },
      {
        description:
          "should return true if the control value is 1 day after the actual date",
        value: now.clone().add(1, "days").toISOString(),
        actualDate: now.toISOString(),
        expected: true,
      },
      {
        description:
          "should return false if the control value is the same day as the actual date",
        value: now.toISOString(),
        actualDate: now.toISOString(),
        expected: false,
      },
      {
        description: "should return false if the control value is null",
        value: null,
        actualDate: now.toISOString(),
        expected: false,
      },
      {
        description: "should return false if the control value is empty string",
        value: "",
        actualDate: now.toISOString(),
        expected: false,
      },
      {
        description:
          "should return false if the control value is 1 week before the actual date",
        value: now.clone().subtract(1, "weeks").toISOString(),
        actualDate: now.toISOString(),
        expected: false,
      },
      {
        description:
          "should return true if the control value is 1 week after the actual date",
        value: now.clone().add(1, "weeks").toISOString(),
        actualDate: now.toISOString(),
        expected: true,
      },
      {
        description:
          "should return false if the control value is 1 month before the actual date",
        value: now.clone().subtract(1, "months").toISOString(),
        actualDate: now.toISOString(),
        expected: false,
      },
      {
        description:
          "should return true if the control value is 1 month after the actual date",
        value: now.clone().add(1, "months").toISOString(),
        actualDate: now.toISOString(),
        expected: true,
      },
      {
        description:
          "should return false if the control value is 1 year before the actual date",
        value: now.clone().subtract(1, "years").toISOString(),
        actualDate: now.toISOString(),
        expected: false,
      },
      {
        description:
          "should return true if the control value is 1 year after the actual date",
        value: now.clone().add(1, "years").toISOString(),
        actualDate: now.toISOString(),
        expected: true,
      },
      {
        description:
          "should return false if the control value is hours after the actual date but same day",
        value: now.clone().add(6, "hours").toISOString(),
        actualDate: now.toISOString(),
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
});
