/*
 * SPDX-FileCopyrightText: 2025 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

import moment from "moment";
import { alterMomentToISO, alterMomentToJSON } from "./functions";

describe(moment.fn.toJSON.name, () => {
  it("should format date to JSON withouth the timezone", () => {
    const date = moment("2021-06-17T13:43:56.111Z");
    const formattedDate = date.toJSON();
    expect(formattedDate).toBe("2021-06-17T13:43:56.111Z");
  });

  describe(alterMomentToJSON.name, () => {
    beforeEach(() => {
      alterMomentToJSON();
    });

    it("should format date to JSON with the current timezone", () => {
      const date = moment("2021-06-17T13:43:56.111Z");
      const formattedDate = date.toJSON();
      expect(formattedDate).toBe("2021-06-17T15:43:56.111000+02:00");
    });
  });
});

describe(moment.fn.toISOString.name, () => {
  it("should format date to ISO withouth the timezone", () => {
    const date = moment("2021-06-17T13:43:56.111Z");
    const formattedDate = date.toISOString();
    expect(formattedDate).toBe("2021-06-17T13:43:56.111Z");
  });

  describe(alterMomentToISO.name, () => {
    beforeEach(() => {
      alterMomentToISO();
    });

    it("should format date to ISO with the current timezone", () => {
      const date = moment("2021-06-17T13:43:56.111Z");
      const formattedDate = date.toISOString();
      expect(formattedDate).toBe("2021-06-17T15:43:56.111+02:00");
    });
  });
});
