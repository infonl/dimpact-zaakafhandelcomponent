/*
 * SPDX-FileCopyrightText: 2021 Atos
 * SPDX-License-Identifier: EUPL-1.2+
 */

import { TranslateService } from "@ngx-translate/core";
import { CustomValidators } from "../../validators/customValidators";
import { AbstractFormField } from "./abstract-form-field";

export abstract class FormComponent {
  abstract translate: TranslateService;
  data!: AbstractFormField;

  constructor() {}

  getErrorMessage(): string {
    return CustomValidators.getErrorMessage(
      this.data.formControl,
      this.data.label,
      this.translate,
    );
  }
}
