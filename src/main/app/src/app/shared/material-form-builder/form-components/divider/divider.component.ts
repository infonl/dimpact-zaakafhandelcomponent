/*
 * SPDX-FileCopyrightText: 2022 Atos
 * SPDX-License-Identifier: EUPL-1.2+
 */

import { Component } from "@angular/core";
import { TranslateService } from "@ngx-translate/core";
import { FormComponent } from "../../model/form-component";
import { DividerFormField } from "./divider-form-field";

@Component({
  templateUrl: "./divider.component.html",
  styleUrls: ["./divider.component.less"],
})
export class DividerComponent extends FormComponent {
  data!: DividerFormField;

  constructor(public translate: TranslateService) {
    super();
  }
}
