/*
 * SPDX-FileCopyrightText: 2022 Atos, 2025 Lifely
 * SPDX-License-Identifier: EUPL-1.2+
 */

import {
  AbstractControl,
  AsyncValidatorFn,
  ValidationErrors,
  ValidatorFn,
} from "@angular/forms";
import { Observable, of, take } from "rxjs";
import { map } from "rxjs/operators";

export class AutocompleteValidators {
  static asyncOptionInList(options: Observable<any[]>): AsyncValidatorFn {
    return (control: AbstractControl): Observable<ValidationErrors> => {
      if (!control.value) {
        return of(null);
      }

      return options.pipe(
        take(1), // Force observable to complete
        map((options) => {
          const find = options.find((option) =>
            AutocompleteValidators.equals(option, control.value),
          );

          return find ? null : { match: true };
        }),
      );
    };
  }

  static optionInList(options: any[]): ValidatorFn {
    return (control: AbstractControl): ValidationErrors => {
      if (!control.value) {
        return null;
      }
      const find: any = options.find((option) =>
        AutocompleteValidators.equals(option, control.value),
      );
      return find ? null : { match: true };
    };
  }

  static equals(object1: any, object2: any): boolean {
    if (typeof object1 === "string") {
      return object1 === object2;
    }
    if (object1 && object2) {
      if ('key' in object1) {
        return object1.key === object2.key;
      } else if ('uuid' in object1) {
        return object1.uuid === object2.uuid;
      } else if ('identificatie' in object1) {
        return object1.identificatie === object2.identificatie;
      } else if ('id' in object1) {
        return object1.id === object2.id;
      } else if ('naam' in object1) {
        return object1.naam === object2.naam;
      } else if ('name' in object1) {
        return object1.name === object2.name;
      }

      throw new Error("Er is geen property aanwezig om te kunnen vergelijken");
    }
    return false;
  }
}
