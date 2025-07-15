/*
 * SPDX-FileCopyrightText: 2024 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

import { FormControl } from "@angular/forms";
import moment from "moment";
import { DateConditionals } from "./date-conditionals";

describe("DateConditionals", () => {
  describe("isAfterDate", () => {
    it("should return true if the control value is before the actual date", () => {
      const control = new FormControl(
        moment().subtract(1, "days").toISOString(),
      );
      const actualDate = moment().toISOString();

      const isExceededResult = DateConditionals.isExceeded(
        control.value ?? "",
        actualDate,
      );
      expect(isExceededResult).toBeTruthy();

      const isPreceededResult = DateConditionals.isPreceded(
        control.value ?? "",
        actualDate,
      );
      expect(isPreceededResult).toBeTruthy();
    });

    it("should return false if the control value is after the actual date", () => {
      const control = new FormControl(moment().add(1, "days").toISOString());
      const actualDate = moment().toISOString();

      const isExceededResult = DateConditionals.isExceeded(
        control.value ?? "",
        actualDate,
      );
      expect(isExceededResult).toBeFalsy();

      const isPreceededResult = DateConditionals.isPreceded(
        control.value ?? "",
        actualDate,
      );
      expect(isPreceededResult).toBeFalsy();
    });
  });
});
