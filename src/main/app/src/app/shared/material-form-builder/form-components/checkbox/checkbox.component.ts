/*
 * SPDX-FileCopyrightText: 2021 Atos
 * SPDX-License-Identifier: EUPL-1.2+
 */

import { Component } from "@angular/core";
import { TranslateService } from "@ngx-translate/core";
import { FormComponent } from "../../model/form-component";
import { CheckboxFormField } from "./checkbox-form-field";

@Component({
  templateUrl: "./checkbox.component.html",
  styleUrls: ["./checkbox.component.less"],
})
export class CheckboxComponent extends FormComponent {
  data!: CheckboxFormField;

  constructor(public translate: TranslateService) {
    super();
  }
}
