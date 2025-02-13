/*
 * SPDX-FileCopyrightText: 2022 Atos
 * SPDX-License-Identifier: EUPL-1.2+
 */

import { AbstractControl, ValidatorFn } from "@angular/forms";
import { TranslateService } from "@ngx-translate/core";

export class CustomValidators {
  static postcode = CustomValidators.postcodeVFn();
  static bsn = CustomValidators.bsnVFn();
  static kvk = CustomValidators.kvkVFn();
  static vestigingsnummer = CustomValidators.vestigingsnummerVFn();
  static rsin = CustomValidators.rsinVFn();
  static email = CustomValidators.emailVFn(false);
  static emails = CustomValidators.emailVFn(true);
  static bedrijfssnaam = CustomValidators.bedrijfsnaamVFn();
  static huisnummer = CustomValidators.huisnummerVFn();

  private static postcodeRegex = /^[1-9][0-9]{3}(?!sa|sd|ss)[a-z]{2}$/i;

  private static ID = "A-Za-z\\d";
  private static LCL = "[" + CustomValidators.ID + "!#$%&'*+\\-/=?^_`{|}~]+";
  private static LBL =
    "[" +
    CustomValidators.ID +
    "]([" +
    CustomValidators.ID +
    "\\-]*[" +
    CustomValidators.ID +
    "])?";
  private static EMAIL =
    CustomValidators.LCL +
    "(\\." +
    CustomValidators.LCL +
    ")*@" +
    CustomValidators.LBL +
    "(\\." +
    CustomValidators.LBL +
    ")+";
  private static emailRegex = new RegExp("^" + CustomValidators.EMAIL + "$");
  private static emailsRegex = new RegExp(
    "^(" + CustomValidators.EMAIL + ")(;//s*" + CustomValidators.EMAIL + ")*$",
  );
  private static bedrijfsnaamRegex = new RegExp("[*()]+");
  private static nummerRegex = new RegExp("^[0-9]*$");

  private static bsnVFn(): ValidatorFn {
    return (control: AbstractControl): { [key: string]: boolean } | null => {
      if (!control.value) {
        return null;
      }
      const val = control.value;
      if (!this.isValidBSN(val)) {
        return { bsn: true };
      }
    };
  }

  private static isValidBSN(bsn: string): boolean {
    if (!CustomValidators.nummerRegex.test(bsn) || bsn.length !== 9) {
      return false;
    }
    let checksum = 0;
    for (let i = 0; i < 8; i++) {
      checksum += Number(bsn.charAt(i)) * (9 - i);
    }
    checksum -= Number(bsn.charAt(8));
    return checksum % 11 === 0;
  }

  private static kvkVFn(): ValidatorFn {
    return (control: AbstractControl): { [key: string]: boolean } | null => {
      if (!control.value) {
        return null;
      }
      const val = control.value;
      if (!CustomValidators.nummerRegex.test(val) || val.length !== 8) {
        return { kvk: true };
      }
    };
  }

  static vestigingsnummerVFn(): ValidatorFn {
    return (control: AbstractControl): { [key: string]: boolean } | null => {
      if (!control.value) {
        return null;
      }
      const val = control.value;
      if (!CustomValidators.nummerRegex.test(val) || val.length !== 12) {
        return { vestigingsnummer: true };
      }
    };
  }

  static rsinVFn(): ValidatorFn {
    return (control: AbstractControl): { [key: string]: boolean } | null => {
      if (!control.value) {
        return null;
      }
      const val = control.value;
      if (!CustomValidators.nummerRegex.test(val) || val.length !== 9) {
        return { rsin: true };
      }
    };
  }

  private static postcodeVFn(): ValidatorFn {
    return (control: AbstractControl): { [key: string]: boolean } | null => {
      if (!control.value) {
        return null;
      }
      const val = control.value;
      if (!CustomValidators.postcodeRegex.test(val)) {
        return { postcode: true };
      }
    };
  }

  private static emailVFn(multi: boolean): ValidatorFn {
    return (control: AbstractControl): { [key: string]: boolean } | null => {
      if (!control.value) {
        return null;
      }
      const val = control.value;
      if (
        multi
          ? !CustomValidators.emailsRegex.test(val)
          : !CustomValidators.emailRegex.test(val)
      ) {
        return { email: true };
      }
    };
  }

  private static bedrijfsnaamVFn(): ValidatorFn {
    return (control: AbstractControl): { [key: string]: boolean } | null => {
      if (!control.value) {
        return null;
      }
      const val = control.value;
      if (CustomValidators.bedrijfsnaamRegex.test(val)) {
        return { bedrijfsnaam: true };
      }
    };
  }

  private static huisnummerVFn(): ValidatorFn {
    return (control: AbstractControl): { [key: string]: boolean } | null => {
      if (!control.value) {
        return null;
      }
      if (!CustomValidators.nummerRegex.test(control.value)) {
        return { huisnummer: true };
      }
    };
  }

  public static getErrorMessage(
    formControl: AbstractControl,
    label: string,
    translate: TranslateService,
  ): string {
    const params = { label: translate.instant(label) };
    const errorKey = Object.keys(formControl.errors || {})[0];

    switch (errorKey) {
      case "required":
        return translate.instant("msg.error.required", params);
      case "min":
        return translate.instant("msg.error.teklein", {
          label: label,
          min: formControl.errors.min.min,
          actual: formControl.errors.min.actual,
        });
      case "max":
        return translate.instant("msg.error.tegroot", {
          label: label,
          max: formControl.errors.max.max,
          actual: formControl.errors.max.actual,
        });
      case "minlength":
        return translate.instant("msg.error.tekort", {
          label: label,
          requiredLength: formControl.errors.minlength.requiredLength,
          actualLength: formControl.errors.minlength.actualLength,
        });
      case "maxlength":
        return translate.instant("msg.error.telang", {
          label: label,
          requiredLength: formControl.errors.maxlength.requiredLength,
          actualLength: formControl.errors.maxlength.actualLength,
        });
      case "email":
        return translate.instant("msg.error.invalid.email", params);
      case "pattern":
        return translate.instant("msg.error.invalid.formaat", {
          label: label,
          requiredPattern: formControl.errors.pattern.requiredPattern,
          actualValue: formControl.errors.pattern.actualValue,
        });
      case "bsn":
        return translate.instant("msg.error.invalid.bsn", params);
      case "kvk":
        return translate.instant("msg.error.invalid.kvk", params);
      case "vestigingsnummer":
        return translate.instant("msg.error.invalid.vestigingsnummer", params);
      case "rsin":
        return translate.instant("msg.error.invalid.rsin", params);
      case "postcode":
        return translate.instant("msg.error.invalid.postcode", params);
      case "huisnummer":
        return translate.instant("msg.error.invalid.huisnummer", params);
      default:
        return "";
    }
  }
}
