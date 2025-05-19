/*
 * SPDX-FileCopyrightText: 2021 Atos, 2025 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

import { AbstractFormFieldBuilder } from "../../model/abstract-form-field-builder";
import { GoogleMapsFormField } from "./google-maps-form-field";

export class GoogleMapsFormFieldBuilder<
  T extends string = string,
> extends AbstractFormFieldBuilder<T> {
  readonly formField: GoogleMapsFormField<T>;

  constructor(value?: T | null) {
    super();
    this.formField = new GoogleMapsFormField();
    this.formField.initControl(value);
  }
}
