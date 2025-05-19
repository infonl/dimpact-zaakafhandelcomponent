/*
 * SPDX-FileCopyrightText: 2021 Atos, 2025 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

import { AbstractFormControlField } from "../../model/abstract-form-control-field";
import { FieldType } from "../../model/field-type.enum";

export class GoogleMapsFormField<T extends string = string> extends AbstractFormControlField<T> {
  fieldType: FieldType = FieldType.GOOGLEMAPS;

  constructor() {
    super();
  }
}
