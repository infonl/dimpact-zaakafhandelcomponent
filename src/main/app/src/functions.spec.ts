/*
 * SPDX-FileCopyrightText: 2025 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

import moment from "moment";
import { alterMoment } from "./functions";

describe(moment.name, () => {
  describe("default behavior", () => {
    it("should format date to JSON without the timezone", () => {
      const date = moment("2021-06-17T13:43:56.111Z");
      const formattedDate = date.toJSON();
      expect(formattedDate).toBe("2021-06-17T13:43:56.111Z");
    });

    it("should format date to ISO without the timezone", () => {
      const date = moment("2021-06-17T13:43:56.111Z");
      const formattedDate = date.toISOString();
      expect(formattedDate).toBe("2021-06-17T13:43:56.111Z");
    });
  });

  describe(alterMoment.name, () => {
    beforeAll(() => {
      alterMoment();
    });

    it("should format date to JSON with the current timezone", () => {
      const date = moment("2021-06-17T13:43:56.111Z");
      const formattedDate = date.toJSON();
      expect(formattedDate).toBe("2021-06-17T15:43:56+02:00");
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
