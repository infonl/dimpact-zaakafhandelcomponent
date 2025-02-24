/*
 * SPDX-FileCopyrightText: 2022 Atos, 2025 Lifely
 * SPDX-License-Identifier: EUPL-1.2+
 */

import { AbstractControl, FormControl } from "@angular/forms";
import { TranslateService } from "@ngx-translate/core";
import { CustomValidators } from "./customValidators";
import { fromPartial } from "@total-typescript/shoehorn";

describe("CustomValidators", () => {
  let translateService: TranslateService;

  beforeEach(() => {
    translateService = {
      instant: jest.fn((key) => key),
    } as unknown as TranslateService;
  });

  const createControl = (value: any): AbstractControl =>
    ({
      value,
      errors: null,
    }) as AbstractControl;

  it.each([
    ["123456782", null],
    ["123456789", { bsn: true }],
  ])("BSN validation: for %s it should return %p", (bsn, expected) => {
    const control = createControl(bsn);
    expect(CustomValidators.bsn(control)).toEqual(expected);
  });

  it.each([
    ["12345678", null],
    ["1234567", { kvk: true }],
  ])("KVK validation: for %s it should return %p", (kvk, expected) => {
    const control = createControl(kvk);
    expect(CustomValidators.kvk(control)).toEqual(expected);
  });

  it.each([
    ["123456789012", null],
    ["12345678901", { vestigingsnummer: true }],
  ])(
    "Vestigingsnummer validation: for %s it should return %p",
    (vestigingsnummer, expected) => {
      const control = createControl(vestigingsnummer);
      expect(CustomValidators.vestigingsnummer(control)).toEqual(expected);
    },
  );

  it.each([
    ["123456789", null],
    ["12345678", { rsin: true }],
  ])("RSIN validation: for %s it should return %p", (rsin, expected) => {
    const control = createControl(rsin);
    expect(CustomValidators.rsin(control)).toEqual(expected);
  });

  it.each([
    ["1234AB", null],
    ["1234A", { postcode: true }],
  ])(
    "Postal code validation: for %s it should return %p",
    (postcode, expected) => {
      const control = createControl(postcode);
      expect(CustomValidators.postcode(control)).toEqual(expected);
    },
  );

  it.each([
    ["test@example.com", null],
    ["test@", { email: true }],
  ])("Email validation: for %s it should return %p", (email, expected) => {
    const control = createControl(email);
    expect(CustomValidators.email(control)).toEqual(expected);
  });

  it.each([
    ["ValidName", null],
    ["Invalid*Name", { bedrijfsnaam: true }],
  ])(
    "Company name validation: for %s it should return %p",
    (bedrijfsnaam, expected) => {
      const control = createControl(bedrijfsnaam);
      expect(CustomValidators.bedrijfsnaam(control)).toEqual(expected);
    },
  );

  it.each([
    ["123", null],
    ["123A", { huisnummer: true }],
  ])(
    "House number validation: for %s it should return %p",
    (huisnummer, expected) => {
      const control = createControl(huisnummer);
      expect(CustomValidators.huisnummer(control)).toEqual(expected);
    },
  );
});

describe("CustomValidators error messages", () => {
  let translateService: TranslateService;

  beforeEach(() => {
    translateService = fromPartial<TranslateService>({
      instant: jest.fn((key: string) => key),
    });
  });

  const createControl = (value: any): AbstractControl => new FormControl(value);

  it.each([
    [{ required: true }, "msg.error.required"],
    [{ min: { min: 5, actual: 3 } }, "msg.error.teklein"],
    [{ max: { max: 10, actual: 12 } }, "msg.error.tegroot"],
    [{ minlength: { requiredLength: 5, actualLength: 3 } }, "msg.error.tekort"],
    [{ maxlength: { requiredLength: 5, actualLength: 7 } }, "msg.error.telang"],
    [{ email: true }, "msg.error.invalid.email"],
    [
      { pattern: { requiredPattern: "^[a-zA-Z]+$", actualValue: "123" } },
      "msg.error.invalid.formaat",
    ],
    [{ bsn: true }, "msg.error.invalid.huisnummer.bsn"],
    [{ kvk: true }, "msg.error.invalid.huisnummer.kvk"],
    [
      { vestigingsnummer: true },
      "msg.error.invalid.huisnummer.vestigingsnummer",
    ],
    [{ rsin: true }, "msg.error.invalid.huisnummer.rsin"],
    [{ postcode: true }, "msg.error.invalid.huisnummer.postcode"],
    [{ huisnummer: true }, "msg.error.invalid.huisnummer.huisnummer"],
    [{ custom: { message: "custom.error.message" } }, "custom.error.message"],
  ])(
    "should return correct error message for %p",
    (errors, expectedMessage) => {
      const control = createControl("");
      control.setErrors(errors);
      const label = "testLabel";

      const errorMessage = CustomValidators.getErrorMessage(
        control,
        label,
        translateService,
      );
      expect(errorMessage).toBe(expectedMessage);
    },
  );
});
