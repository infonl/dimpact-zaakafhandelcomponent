/*
 * SPDX-FileCopyrightText: 2025 Lifely
 * SPDX-License-Identifier: EUPL-1.2+
 *
 */

import { FormControl } from "@angular/forms";
import { from, of } from "rxjs";
import { AutocompleteValidators } from "./autocomplete-validators";

describe(AutocompleteValidators.name, () => {
  describe(AutocompleteValidators.asyncOptionInList.name, () => {
    it("should return null if control value is null", (done) => {
      const options$ = of([
        { key: "1", name: "Option 1" },
        { key: "2", name: "Option 2" },
      ]);
      const validatorFn = AutocompleteValidators.asyncOptionInList(options$);
      const control = new FormControl(null);

      from(validatorFn(control)).subscribe((result) => {
        expect(result).toBeNull();
        done();
      });
    });

    describe("object input", () => {
      it("should return null if control value matches an option in the list", (done) => {
        const options$ = of([
          { key: "1", name: "Option 1" },
          { key: "2", name: "Option 2" },
        ]);
        const validatorFn = AutocompleteValidators.asyncOptionInList(options$);
        const control = new FormControl({ key: "1", name: "Option 1" });

        from(validatorFn(control)).subscribe((result) => {
          expect(result).toBeNull();
          done();
        });
      });

      it("should return { match: true } if control value does not match any option in the list", (done) => {
        const options$ = of([
          { key: "1", name: "Option 1" },
          { key: "2", name: "Option 2" },
        ]);
        const validatorFn = AutocompleteValidators.asyncOptionInList(options$);
        const control = new FormControl({ key: "3", name: "Option 3" });

        from(validatorFn(control)).subscribe((result) => {
          expect(result).toEqual({ match: true });
          done();
        });
      });
    });

    describe("string input", () => {
      it("should return null if control value matches an option in the list", (done) => {
        const options$ = of(["Option 1", "Option 2"]);
        const validatorFn = AutocompleteValidators.asyncOptionInList(options$);
        const control = new FormControl("Option 1");

        from(validatorFn(control)).subscribe((result) => {
          expect(result).toBeNull();
          done();
        });
      });

      it("should return { match: true } if control value does not match any option in the list", (done) => {
        const options$ = of(["Option 1", "Option 2"]);
        const validatorFn = AutocompleteValidators.asyncOptionInList(options$);
        const control = new FormControl("Option 3");

        from(validatorFn(control)).subscribe((result) => {
          expect(result).toEqual({ match: true });
          done();
        });
      });
    });
  });
});
