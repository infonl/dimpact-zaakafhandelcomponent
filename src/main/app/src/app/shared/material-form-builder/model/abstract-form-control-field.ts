/*
 * SPDX-FileCopyrightText: 2022 Atos, 2025 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

import { FormControl } from "@angular/forms";
import { AbstractFormField } from "./abstract-form-field";

export abstract class AbstractFormControlField<T = unknown> extends AbstractFormField<T> {
  formControl: FormControl<T | undefined | null>;

  protected constructor() {
    super();
  }

  initControl(value?: T | null) {
    this.formControl = AbstractFormField.formControlInstance<T | null>(value);
  }
}
