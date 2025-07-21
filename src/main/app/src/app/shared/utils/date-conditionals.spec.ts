/*
 * SPDX-FileCopyrightText: 2024 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

import { FormControl } from "@angular/forms";
import moment from "moment";
import { DateConditionals } from "./date-conditionals";

describe(DateConditionals.name, () => {
  describe(DateConditionals.isExceeded.name, () => {
    it("should return true if the control value is before the actual date", () => {
      const control = new FormControl(
        moment().subtract(1, "days").toISOString(),
      );
      const actualDate = moment().toISOString();
      const result = DateConditionals.isExceeded(
        control.value ?? "",
        actualDate,
      );
      expect(result).toBeTruthy();
    });

    it("should return false if the control value is after the actual date", () => {
      const control = new FormControl(moment().add(1, "days").toISOString());
      const actualDate = moment().toISOString();
      const result = DateConditionals.isExceeded(
        control.value ?? "",
        actualDate,
      );
      expect(result).toBeFalsy();
    });
  });
});

describe(DateConditionals.name, () => {
  describe(DateConditionals.isPreceded.name, () => {
    it("should return false if the control value is before the actual date", () => {
      const control = new FormControl(
        moment().subtract(1, "days").toISOString(),
      );
      const actualDate = moment().toISOString();
      const result = DateConditionals.isPreceded(
        control.value ?? "",
        actualDate,
      );
      expect(result).toBeFalsy();
    });

    it("should return true if the control value is after the actual date", () => {
      const control = new FormControl(moment().add(1, "days").toISOString());
      const actualDate = moment().toISOString();
      const result = DateConditionals.isPreceded(
        control.value ?? "",
        actualDate,
      );
      expect(result).toBeTruthy();
    });
  });
});
