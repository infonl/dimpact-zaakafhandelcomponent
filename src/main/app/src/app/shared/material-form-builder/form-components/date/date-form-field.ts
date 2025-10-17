/*
 * SPDX-FileCopyrightText: 2021 - 2022 Atos, 2025 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

import { Moment } from "moment/moment";
import { AbstractFormControlField } from "../../model/abstract-form-control-field";
import { FieldType } from "../../model/field-type.enum";

export class DateFormField extends AbstractFormControlField<
  Moment | Date | string | null
> {
  fieldType = FieldType.DATE;
  public minDate: Date;
  public maxDate: Date;
  public showDays: boolean;

  constructor() {
    super();
  }
}
