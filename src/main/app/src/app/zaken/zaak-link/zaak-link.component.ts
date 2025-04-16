/*
 * SPDX-FileCopyrightText: 2025 Lifely
 * SPDX-License-Identifier: EUPL-1.2+
 */

import { Component, Input, OnDestroy, OnInit } from "@angular/core";
import { MatDrawer } from "@angular/material/sidenav";
import { Subject, takeUntil } from "rxjs";
import { InputFormFieldBuilder } from "src/app/shared/material-form-builder/form-components/input/input-form-field-builder";
import { AbstractFormControlField } from "src/app/shared/material-form-builder/model/abstract-form-control-field";
import { Zaak } from "../model/zaak";

@Component({
  selector: "zac-zaak-link",
  templateUrl: "./zaak-link.component.html",
  styleUrls: ["./zaak-link.component.less"],
})
export class ZaakLinkComponent implements OnInit, OnDestroy {
  @Input({ required: true }) zaak!: Zaak; // GeneratedType<"RestZaak">;
  @Input({ required: true }) sideNav!: MatDrawer;

  intro: string = "";
  caseSearchField?: AbstractFormControlField;
  isValid: boolean = false;
  loading: boolean = false;

  private ngDestroy = new Subject<void>();

  constructor() {}

  ngOnInit() {
    this.caseSearchField = new InputFormFieldBuilder()
      .id("zaak")
      .label("zaak.identificatie")
      .build();

    this.caseSearchField.formControl.valueChanges
      .pipe(takeUntil(this.ngDestroy))
      .subscribe((value) => {
        this.isValid = value?.length >= 2;
      });
  }

  searchCases() {}

  selectCase() {}

  closeDrawer() {
    this.sideNav.close();
    this.reset();
  }

  private reset() {}

  ngOnDestroy() {
    this.ngDestroy.next();
    this.ngDestroy.complete();
  }
}
