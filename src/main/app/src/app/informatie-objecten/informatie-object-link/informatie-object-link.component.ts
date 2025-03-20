/*
 * SPDX-FileCopyrightText: 2025 Lifely
 * SPDX-License-Identifier: EUPL-1.2+
 */

import { Component, Input, OnDestroy, OnInit } from "@angular/core";
import { FormBuilder, FormGroup } from "@angular/forms";
import { MatDrawer } from "@angular/material/sidenav";
import { GeneratedType } from "../../shared/utils/generated-types";
import { InformatieObjectenService } from "../informatie-objecten.service";
import { AbstractFormControlField } from "src/app/shared/material-form-builder/model/abstract-form-control-field";
import { InputFormFieldBuilder } from "src/app/shared/material-form-builder/form-components/input/input-form-field-builder";
import { Validators } from "ngx-editor";

@Component({
  selector: "zac-informatie-object-link",
  templateUrl: "./informatie-object-link.component.html",
  styleUrls: ["./informatie-object-link.component.less"],
})
export class InformatieObjectLinkComponent implements OnInit, OnDestroy {
  @Input({ required: true })
  infoObject!: GeneratedType<"RestEnkelvoudigInformatieObjectVersieGegevens">;
  @Input({ required: true }) sideNav!: MatDrawer;

  caseSearchField?: AbstractFormControlField;
  issValid: boolean = false;
  loading: boolean = false;

  constructor(private informatieObjectenService: InformatieObjectenService) {}

  ngOnInit() {
    this.caseSearchField = new InputFormFieldBuilder()
      .id("zaak")
      .label("Zaaknummer")
      .build();

    this.caseSearchField.formControl.valueChanges.subscribe((value) => {
      console.log("valueChanges", value);
      this.issValid = value?.length > 2;
    });
  }

  zoekZaken(): void {
    console.log("zoekZaken");
  }

  wissen(): void {
    console.log("wissen");
    this.caseSearchField?.formControl.reset();
  }

  ngOnDestroy() {}

  onFormSubmit(formGroup: FormGroup): void {}
}
