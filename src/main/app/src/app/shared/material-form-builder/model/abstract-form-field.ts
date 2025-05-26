/*
 * SPDX-FileCopyrightText: 2022 Atos, 2025 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

import {
  AbstractControl,
  FormControl,
  FormControlOptions,
} from "@angular/forms";
import { FieldType } from "./field-type.enum";
import { FormFieldHint } from "./form-field-hint";

export abstract class AbstractFormField<T = unknown> {
  static formControlOptions: FormControlOptions = { nonNullable: true };

  id: string;
  styleClass: string;
  label: string;
  required: boolean;
  readonly: boolean;
  abstract formControl: AbstractControl<T>;
  hint: FormFieldHint;

  abstract fieldType: FieldType;

  protected constructor() {}

  hasReadonlyView() {
    return false;
  }

  value(value: T) {
    this.formControl.setValue(value);
    this.formControl.markAsDirty();
  }

  reset(): void {
    this.formControl.reset();
  }

  abstract initControl(value?: T): void;

  static formControlInstance<T>(value?: T) {
    return new FormControl<T | null>(value ?? null, this.formControlOptions);
  }

  hasFormControl(): boolean {
    return this.formControl != null;
  }
}
