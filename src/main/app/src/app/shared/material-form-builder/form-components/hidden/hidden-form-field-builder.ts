/*
 * SPDX-FileCopyrightText: 2022 Atos, 2023 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

import { AbstractFormFieldBuilder } from "../../model/abstract-form-field-builder";
import { HiddenFormField } from "./hidden-form-field";

export class HiddenFormFieldBuilder<
  T = unknown,
> extends AbstractFormFieldBuilder<T> {
  readonly formField: HiddenFormField<T>;

  constructor(value?: T | null) {
    super();
    this.formField = new HiddenFormField();
    this.formField.initControl(value);
  }
}
