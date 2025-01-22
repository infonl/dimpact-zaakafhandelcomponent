/*
 * SPDX-FileCopyrightText: 2022 Atos, 2024 Lifely
 * SPDX-License-Identifier: EUPL-1.2+
 */

import { Component, OnInit } from "@angular/core";
import { TranslateService } from "@ngx-translate/core";
import { FormComponent } from "../../model/form-component";
import { RadioFormField } from "./radio-form-field";

@Component({
  templateUrl: "./radio.component.html",
  styleUrls: ["./radio.component.less"],
})
export class RadioComponent extends FormComponent implements OnInit {
  data: RadioFormField;
  selectedValue: string;

  constructor(public translate: TranslateService) {
    super();
  }

  ngOnInit(): void {
    this.selectedValue = this.data.formControl.value;
  }
}
