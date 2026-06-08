/*
 * SPDX-FileCopyrightText: 2025 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

import {
  booleanAttribute,
  Component,
  effect,
  input,
  output,
} from "@angular/core";
import { FormGroup } from "@angular/forms";
import { Form, FormConfig, FormField } from "./form-field.types";

@Component({
  selector: "zac-composed-form",
  templateUrl: "./composed-form.component.html",
  standalone: false,
})
export class ZacComposedForm<F extends Form> {
  protected readonly form = input.required<FormGroup<F>>();
  protected readonly fields = input.required<FormField[]>();
  protected readonly config = input<FormConfig>({ hideCancelButton: false });
  protected readonly readonly = input(false, { transform: booleanAttribute });
  protected readonly loading = input(false, { transform: booleanAttribute });

  protected readonly formSubmitted = output<FormGroup<F>>();
  protected readonly formPartiallySubmitted = output<FormGroup<F>>();
  protected readonly formCancelled = output<void>();

  constructor() {
    effect(() => {
      const isReadonly = this.readonly();
      const formGroup = this.form();
      const fields = this.fields();

      if (isReadonly && formGroup.enabled) {
        formGroup.disable({ onlySelf: true });
      } else if (!isReadonly && formGroup.disabled) {
        formGroup.enable({ onlySelf: true });

        for (const field of fields) {
          if (!field.readonly) continue;
          const control = formGroup.controls[field.key];
          control.disable({ emitEvent: false, onlySelf: true });
        }
      }
    });
  }

  protected submitForm() {
    this.formSubmitted.emit(this.form());
  }

  protected partiallySubmitForm() {
    this.formPartiallySubmitted.emit(this.form());
  }

  protected cancelForm() {
    this.form().reset();
    this.formCancelled.emit();
  }
}
