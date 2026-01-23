/*
 * SPDX-FileCopyrightText: 2021 Atos
 * SPDX-License-Identifier: EUPL-1.2+
 */

import { Component } from "@angular/core";
import { TranslateService } from "@ngx-translate/core";
import { FormComponent } from "../../model/form-component";
import { ReadonlyFormField } from "./readonly-form-field";

@Component({
  templateUrl: "./readonly.component.html",
  styleUrls: ["./readonly.component.less"],
})
export class ReadonlyComponent extends FormComponent {
  data: ReadonlyFormField;

  constructor(public translate: TranslateService) {
    super();
  }
}
