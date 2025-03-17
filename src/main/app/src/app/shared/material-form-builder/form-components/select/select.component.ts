/*
 * SPDX-FileCopyrightText: 2021 Atos
 * SPDX-License-Identifier: EUPL-1.2+
 */

import { Component } from "@angular/core";
import { TranslateService } from "@ngx-translate/core";
import { FormComponent } from "../../model/form-component";
import { SelectFormField } from "./select-form-field";

@Component({
  templateUrl: "./select.component.html",
  styleUrl: "./select.component.less",
})
export class SelectComponent extends FormComponent {
  data!: SelectFormField;

  constructor(public translate: TranslateService) {
    super();
  }

  getLabel(option: string | Record<string, string>): string {
    switch (typeof option) {
      case "string":
        return option;
      case "object":
        return this.data.optionLabel ? option[this.data.optionLabel] : "";
      default:
        return "";
    }
  }

  getFormControlValue() {
    return this.data.optionLabel &&
      this.data.formControl.value &&
      this.data.formControl.value[this.data.optionLabel]
      ? this.data.formControl.value[this.data.optionLabel]
      : this.data.formControl.value;
  }
}
