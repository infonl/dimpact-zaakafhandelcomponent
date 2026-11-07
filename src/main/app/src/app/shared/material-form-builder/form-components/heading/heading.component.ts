/*
 * SPDX-FileCopyrightText: 2021 Atos
 * SPDX-License-Identifier: EUPL-1.2+
 */

import { Component } from "@angular/core";
import { TranslateService } from "@ngx-translate/core";
import { FormComponent } from "../../model/form-component";
import { HeadingFormField, HeadingLevel } from "./heading-form-field";

@Component({
  templateUrl: "./heading.component.html",
  styleUrls: ["./heading.component.less"],
})
export class HeadingComponent extends FormComponent {
  readonly headingLevel = HeadingLevel;
  data: HeadingFormField;

  constructor(public translate: TranslateService) {
    super();
  }
}
