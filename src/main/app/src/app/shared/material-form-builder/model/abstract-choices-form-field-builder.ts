/*
 * SPDX-FileCopyrightText: 2021 Atos
 * SPDX-License-Identifier: EUPL-1.2+
 */

import { isObservable, Observable, of as observableOf } from "rxjs";
import { AbstractChoicesFormField } from "./abstract-choices-form-field";
import { AbstractFormFieldBuilder } from "./abstract-form-field-builder";

export abstract class AbstractChoicesFormFieldBuilder extends AbstractFormFieldBuilder {
  abstract readonly formField: AbstractChoicesFormField;

  constructor() {
    super();
  }

  optionLabel(optionLabel: string): this {
    this.formField.optionLabel = optionLabel;
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

  optionsOrder(optionOrderFn: (a: any, b: any) => number): this {
    this.formField.optionOrderFn = optionOrderFn;
    return this;
  }

  options(options: Observable<unknown[]> | unknown[]): this {
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
