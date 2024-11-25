/*
 * SPDX-FileCopyrightText: 2024 Lifely
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
      const result = DateConditionals.isExceeded(control.value, actualDate);
      expect(result).toBeTruthy();
    });

    it("should return false if the control value is after the actual date", () => {
      const control = new FormControl(moment().add(1, "days").toISOString());
      const actualDate = moment().toISOString();
      const result = DateConditionals.isExceeded(control.value, actualDate);
      expect(result).toBeFalsy();
    });
  });
});
