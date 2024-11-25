/*
 * SPDX-FileCopyrightText: 2022 Atos, 2023 Lifely
 * SPDX-License-Identifier: EUPL-1.2+
 */

import { Component, OnInit } from "@angular/core";
import { TranslateService } from "@ngx-translate/core";
import { FormComponent } from "../../model/form-component";
import { HiddenFormField } from "./hidden-form-field";

@Component({
  templateUrl: "./hidden.component.html",
  styleUrls: ["./hidden.component.less"],
})
export class HiddenComponent extends FormComponent implements OnInit {
  data: HiddenFormField;

  constructor(public translate: TranslateService) {
    super();
  }

  ngOnInit(): void {}
}
