/*
 * SPDX-FileCopyrightText: 2021 Atos
 * SPDX-License-Identifier: EUPL-1.2+
 */

import { Component } from "@angular/core";
import { TranslateService } from "@ngx-translate/core";
import moment from "moment";
import { FormComponent } from "../../model/form-component";
import { DateFormField } from "./date-form-field";

@Component({
  templateUrl: "./date.component.html",
  styleUrls: ["./date.component.less"],
})
export class DateComponent extends FormComponent {
  data!: DateFormField;

  constructor(public translate: TranslateService) {
    super();
  }

  getErrorMessage(): string {
    const formControl = this.data.formControl;
    if (formControl.hasError("matDatepickerParse")) {
      return this.translate.instant("msg.error.invalid.formaat", {
        label: this.translate.instant(this.data.label),
        requiredPattern: this.translate.instant("msg.error.date.formaat"),
      });
    }
    return super.getErrorMessage();
  }

  days() {
    return moment(this.data.formControl.value).diff(
      moment().startOf("day"),
      "days",
    );
  }
}
