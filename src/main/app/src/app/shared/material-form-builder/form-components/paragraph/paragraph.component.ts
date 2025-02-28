/*
 * SPDX-FileCopyrightText: 2022 Atos
 * SPDX-License-Identifier: EUPL-1.2+
 */

import { Component, OnInit } from "@angular/core";
import { TranslateService } from "@ngx-translate/core";
import { FormComponent } from "../../model/form-component";
import { ParagraphFormField } from "./paragraph-form-field";

@Component({
  templateUrl: "./paragraph.component.html",
  styleUrls: ["./paragraph.component.less"],
})
export class ParagraphComponent extends FormComponent implements OnInit {
  data!: ParagraphFormField;

  constructor(public translate: TranslateService) {
    super();
  }

  ngOnInit(): void {}
}
