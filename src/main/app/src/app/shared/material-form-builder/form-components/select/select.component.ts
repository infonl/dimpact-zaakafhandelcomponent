/*
 * SPDX-FileCopyrightText: 2021 Atos
 * SPDX-License-Identifier: EUPL-1.2+
 */

import { Component, OnDestroy, OnInit } from "@angular/core";
import { TranslateService } from "@ngx-translate/core";
import { Subscription } from "rxjs";
import { FormComponent } from "../../model/form-component";
import { SelectFormField } from "./select-form-field";

@Component({
  templateUrl: "./select.component.html",
  styleUrls: ["./select.component.less"],
})
export class SelectComponent
  extends FormComponent
  implements OnInit, OnDestroy
{
  data: SelectFormField;
  loading$: Subscription;

  constructor(public translate: TranslateService) {
    super();
  }

  ngOnInit(): void {
    this.loading$ = this.data.loading$.subscribe((loading) => {
      if (loading) {
        this.data.formControl.disable();
      } else {
        this.data.formControl.enable();
      }
    });
  }

  ngOnDestroy(): void {
    this.loading$.unsubscribe();
  }

  getLabel(option: unknown): string {
    return this.data.optionLabel ? option[this.data.optionLabel] : option;
  }

  getFormControlValue() {
    return this.data.optionLabel &&
      this.data.formControl.value &&
      this.data.formControl.value[this.data.optionLabel]
      ? this.data.formControl.value[this.data.optionLabel]
      : this.data.formControl.value;
  }
}
