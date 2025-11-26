/*
 * SPDX-FileCopyrightText: 2022 Atos, 2025 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

import { FormControl } from "@angular/forms";
import { AbstractFormField } from "./abstract-form-field";

export abstract class AbstractFormControlField<
  T = unknown,
> extends AbstractFormField<T> {
  formControl!: FormControl<T | undefined | null>;

  protected constructor() {
    super();
  }

  initControl(value?: T | null) {
    this.formControl = AbstractFormField.formControlInstance<T | null>(
      this.coerce(value) ?? null,
    );
  }

  /**
   * This is a dirty hack to coerce `"null"` or `"undefined"`
   *
   * As we are moving over to the Angular `FormBuilder` API,
   * this class is deprecated and will be removed in future versions.
   */
  private coerce(value: T | null | undefined) {
    if (value === "null") return null;
    if (value === "undefined") return undefined;
    return value;
  }
}
