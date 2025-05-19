/*
 * SPDX-FileCopyrightText: 2021 Atos, 2024 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

import { ValidatorFn, Validators } from "@angular/forms";
import { Observable, first } from "rxjs";
import { AbstractFormField } from "./abstract-form-field";
import { FormFieldHint } from "./form-field-hint";

export abstract class AbstractFormFieldBuilder<T = unknown> {
  abstract readonly formField: AbstractFormField<T | null | undefined>;

  protected constructor() {}

  id(id: string): this {
    this.formField.id = id;
    return this;
  }

  styleClass(styleClass: string): this {
    this.formField.styleClass = styleClass;
    return this;
  }

  label(label: string): this {
    this.formField.label = label;
    return this;
  }

  readonly(readonly = true): this {
    this.formField.readonly = readonly;
    return this;
  }

  value$(value: Observable<unknown>): this {
    value.pipe(first()).subscribe((firstValue) => {
      this.formField.formControl.setValue(firstValue as T);
    });
    return this;
  }

  validators(...validators: ValidatorFn[]): this {
    this.formField.formControl.setValidators(validators);
    if (
      validators.find((v) => v === Validators.required) ||
      validators.find((v) => v === Validators.requiredTrue)
    ) {
      this.formField.required = true;
    }
    return this;
  }

  hint(hint: string): this {
    this.formField.hint = new FormFieldHint(hint);
    return this;
  }

  build(): this["formField"] {
    this.validate();
    return this.formField;
  }

  disabled(disable: boolean = true) {
    if (disable) {
      this.formField.formControl.disable();
    }
    return this;
  }

  validate() {
    if (!this.formField.id) {
      throw new Error("id is required");
    }
    if (!this.formField.label) {
      throw new Error("label is required");
    }
  }
}
