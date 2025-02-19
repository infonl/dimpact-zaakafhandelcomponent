import { AbstractControl } from "@angular/forms";
import { TranslateService } from "@ngx-translate/core";
import { CustomValidators } from "./customValidators";

/*
 * SPDX-FileCopyrightText: 2022 Atos, 2025 Lifely
 * SPDX-License-Identifier: EUPL-1.2+
 */

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

  it("should validate BSN correctly", () => {
    const validBsnControl = createControl("123456782");
    const invalidBsnControl = createControl("123456789");

    expect(CustomValidators.bsn(validBsnControl)).toBeNull();
    expect(CustomValidators.bsn(invalidBsnControl)).toEqual({ bsn: true });
  });

  it("should validate KVK correctly", () => {
    const validKvkControl = createControl("12345678");
    const invalidKvkControl = createControl("1234567");

    expect(CustomValidators.kvk(validKvkControl)).toBeNull();
    expect(CustomValidators.kvk(invalidKvkControl)).toEqual({ kvk: true });
  });

  it("should validate vestigingsnummer correctly", () => {
    const validVestigingsnummerControl = createControl("123456789012");
    const invalidVestigingsnummerControl = createControl("12345678901");

    expect(
      CustomValidators.vestigingsnummer(validVestigingsnummerControl),
    ).toBeNull();
    expect(
      CustomValidators.vestigingsnummer(invalidVestigingsnummerControl),
    ).toEqual({ vestigingsnummer: true });
  });

  it("should validate RSIN correctly", () => {
    const validRsinControl = createControl("123456789");
    const invalidRsinControl = createControl("12345678");

    expect(CustomValidators.rsin(validRsinControl)).toBeNull();
    expect(CustomValidators.rsin(invalidRsinControl)).toEqual({ rsin: true });
  });

  it("should validate postcode correctly", () => {
    const validPostcodeControl = createControl("1234AB");
    const invalidPostcodeControl = createControl("1234A");

    expect(CustomValidators.postcode(validPostcodeControl)).toBeNull();
    expect(CustomValidators.postcode(invalidPostcodeControl)).toEqual({
      postcode: true,
    });
  });

  it("should validate email correctly", () => {
    const validEmailControl = createControl("test@example.com");
    const invalidEmailControl = createControl("test@");

    expect(CustomValidators.email(validEmailControl)).toBeNull();
    expect(CustomValidators.email(invalidEmailControl)).toEqual({
      email: true,
    });
  });

  it("should validate bedrijfsnaam correctly", () => {
    const validBedrijfsnaamControl = createControl("ValidName");
    const invalidBedrijfsnaamControl = createControl("Invalid*Name");

    expect(CustomValidators.bedrijfsnaam(validBedrijfsnaamControl)).toBeNull();
    expect(CustomValidators.bedrijfsnaam(invalidBedrijfsnaamControl)).toEqual({
      bedrijfsnaam: true,
    });
  });

  it("should validate huisnummer correctly", () => {
    const validHuisnummerControl = createControl("123");
    const invalidHuisnummerControl = createControl("123A");

    expect(CustomValidators.huisnummer(validHuisnummerControl)).toBeNull();
    expect(CustomValidators.huisnummer(invalidHuisnummerControl)).toEqual({
      huisnummer: true,
    });
  });
});

describe("CustomValidators error messages", () => {
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

  it("should return required error message", () => {
    const control = createControl("");
    control.setErrors({ required: true });
    const label = "testLabel";

    const errorMessage = CustomValidators.getErrorMessage(
      control,
      label,
      translateService,
    );
    expect(errorMessage).toBe("msg.error.required");
  });
});
