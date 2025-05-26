/*
 * SPDX-FileCopyrightText: 2022 Atos, 2025 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

import { AbstractControl, AsyncValidatorFn, ValidatorFn } from "@angular/forms";
import { Observable, of, take } from "rxjs";
import { map } from "rxjs/operators";

export class AutocompleteValidators {
  static asyncOptionInList(options: Observable<unknown[]>): AsyncValidatorFn {
    return (control: AbstractControl) => {
      if (!control.value) {
        return of(null);
      }

      return options.pipe(
        take(1), // Force observable to complete
        map((options) => {
          const find = options.find((option) =>
            AutocompleteValidators.equals(option as string, control.value),
          );

          return find ? null : { match: true };
        }),
      );
    };
  }

  static optionInList(options: unknown[]): ValidatorFn {
    return (control: AbstractControl) => {
      if (!control.value) {
        return null;
      }
      const find = options.find((option) =>
        AutocompleteValidators.equals(option as string, control.value),
      );
      return find ? null : { match: true };
    };
  }

  static equals(
    object1: Record<string, unknown> | string,
    object2: Record<string, unknown> | string,
  ) {
    if (typeof object1 === "string" || typeof object2 === "string") {
      return object1 === object2;
    }
    if (object1 && object2) {
      if ("key" in object1) {
        return object1.key === object2.key;
      } else if ("uuid" in object1) {
        return object1.uuid === object2.uuid;
      } else if ("identificatie" in object1) {
        return object1.identificatie === object2.identificatie;
      } else if ("id" in object1) {
        return object1.id === object2.id;
      } else if ("naam" in object1) {
        return object1.naam === object2.naam;
      } else if ("name" in object1) {
        return object1.name === object2.name;
      }

      throw new Error("Er is geen property aanwezig om te kunnen vergelijken");
    }
    return false;
  }
}
