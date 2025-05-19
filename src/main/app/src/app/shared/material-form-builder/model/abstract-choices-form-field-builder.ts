/*
 * SPDX-FileCopyrightText: 2021 Atos, 2025 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

import { isObservable, Observable, of as observableOf} from "rxjs";
import { AbstractChoicesFormField } from "./abstract-choices-form-field";
import { AbstractFormFieldBuilder } from "./abstract-form-field-builder";

export abstract class AbstractChoicesFormFieldBuilder<T extends Record<string, unknown> = Record<string, unknown>> extends AbstractFormFieldBuilder<T> {
  abstract readonly formField: AbstractChoicesFormField<T>;

  protected constructor() {
    super();
  }

  optionLabel(optionLabel: keyof T): this {
    this.formField.optionLabel = String(optionLabel);
    return this;
  }

  optionSuffix(optionSuffix: string): this {
    this.formField.optionSuffix = optionSuffix;
    return this;
  }

  optionValue(optionValue: string): this {
    this.formField.optionValue = optionValue;
    return this;
  }

  optionsOrder(optionOrderFn: (a: T, b: T) => number): this {
    this.formField.optionOrderFn = optionOrderFn;
    return this;
  }

  options(options: Observable<T[]> | T[] ): this {
    if (isObservable(options)) {
      this.formField.options = options;
    } else {
      this.formField.options = observableOf(options);
    }
    return this;
  }

  settings(
    settings: { translateLabels?: boolean; capitalizeFirstLetter: boolean } = {
      // for backwards compatibility, translateLabels defaults to true
      translateLabels: true,
      // for backwards compatibility, capitalizeFirstLetter defaults to false
      capitalizeFirstLetter: false,
    },
  ): this {
    this.formField.settings = settings;
    return this;
  }
}
