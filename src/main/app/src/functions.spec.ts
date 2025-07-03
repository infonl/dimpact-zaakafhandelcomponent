/*
 * SPDX-FileCopyrightText: 2025 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

import moment from "moment";
import { alterMoment } from "./functions";

describe(moment.fn.toJSON.name, () => {
  it("should format date to JSON without the timezone", () => {
    const date = moment("2021-06-17T13:43:56.111Z");
    const formattedDate = date.toJSON();
    expect(formattedDate).toBe("2021-06-17T13:43:56.111Z");
  });

  describe(alterMoment.name, () => {
    beforeEach(() => {
      alterMoment();
    });

    it("should format date to JSON with the current timezone", () => {
      const date = moment("2021-06-17T13:43:56.111Z");
      const formattedDate = date.toJSON();
      expect(formattedDate).toBe("2021-06-17T15:43:56+02:00");
    });
  });
});

describe(moment.fn.toISOString.name, () => {
  it("should format date to ISO without the timezone", () => {
    const date = moment("2021-06-17T13:43:56.111Z");
    const formattedDate = date.toISOString();
    expect(formattedDate).toBe("2021-06-17T13:43:56.111Z");
  });

  describe(alterMoment.name, () => {
    beforeEach(() => {
      alterMoment();
    });

    it.each([
      ["2021-06-17T00:00:00.000", "2021-06-17T00:00:00.000+02:00"],
      ["2021-06-17T00:00:00.000Z", "2021-06-17T02:00:00.000+02:00"],
      ["2021-06-17T13:43:56.111Z", "2021-06-17T15:43:56.111+02:00"],
      ["2021-06-17T00:00:00.000+02:00", "2021-06-17T00:00:00.000+02:00"],
      ["2021-06-16T22:00:00.000+02:00", "2021-06-16T22:00:00.000+02:00"],
    ])("should format %s to %s", (input, expected) => {
      const date = moment(input);
      const formattedDate = date.toISOString();
      expect(formattedDate).toBe(expected);
    });
  });
});

describe(moment.fn.diff.name, () => {
  it("calculate the difference", () => {
    const date = moment("2021-06-18T00:00:00.000Z");
    const otherDate = moment("2021-06-17T00:00:00.000Z");
    const diff = date.diff(otherDate, "days");
    expect(diff).toBe(1);
  });

  describe(alterMoment.name, () => {
    beforeEach(() => {
      alterMoment();
    });

    it("should use the UTC for calculating difference", () => {
      const date = moment("2021-06-18T00:00:00.000Z");
      const otherDate = moment("2021-06-17T00:00:00.000Z");
      const diff = date.diff(otherDate, "days");
      expect(diff).toBe(1);
    });
  });
});
