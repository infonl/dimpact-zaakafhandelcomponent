/*
 * SPDX-FileCopyrightText: 2021 - 2022 Atos, 2025 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

import { AbstractFormFieldBuilder } from "../../model/abstract-form-field-builder";
import { DateFormField } from "./date-form-field";
import {Moment} from "moment";

export class DateFormFieldBuilder extends AbstractFormFieldBuilder<Moment | Date | string | null> {
  readonly formField: DateFormField;

  constructor(value?: Moment | Date | string | null) {
    super();
    this.formField = new DateFormField();
    this.formField.initControl(value);
    const maxDate = new Date();
    maxDate.setDate(maxDate.getDate() + 36525); // default maxDate (100 jaar)
    this.maxDate(maxDate);
  }

  minDate(date: Date) {
    this.formField.minDate = date;
    return this;
  }

  maxDate(date: Date) {
    this.formField.maxDate = date;
    return this;
  }

  showDays() {
    this.formField.showDays = true;
    return this;
  }
}
