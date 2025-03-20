/*
 * SPDX-FileCopyrightText: 2025 Lifely
 * SPDX-License-Identifier: EUPL-1.2+
 */

import { Component, Input, OnDestroy, OnInit } from "@angular/core";
import { MatDrawer } from "@angular/material/sidenav";
import { GeneratedType } from "../../shared/utils/generated-types";
import { InformatieObjectenService } from "../informatie-objecten.service";
import { AbstractFormControlField } from "src/app/shared/material-form-builder/model/abstract-form-control-field";
import { InputFormFieldBuilder } from "src/app/shared/material-form-builder/form-components/input/input-form-field-builder";
import { MatTableDataSource } from "@angular/material/table";
import { Subject, takeUntil } from "rxjs";

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

  cases = new MatTableDataSource<GeneratedType<"RestZaak">>();
  caseColumns: string[] = [
    "identificatie",
    "zaaktypeOmschrijving",
    "status",
    "registratiedatum",
  ];

  private ngDestroy = new Subject<void>();

  constructor(private informatieObjectenService: InformatieObjectenService) {}

  ngOnInit() {
    this.caseSearchField = new InputFormFieldBuilder()
      .id("zaak")
      .label("Zaaknummer")
      .build();

    this.caseSearchField.formControl.valueChanges
      .pipe(takeUntil(this.ngDestroy))
      .subscribe((value) => {
        console.log("valueChanges", value);
        this.issValid = value?.length > 2;
      });
  }

  searchCases(): void {
    console.log("searchCases");
  }

  wissen(): void {
    this.caseSearchField?.formControl.reset();
    this.cases.data = [];
  }

  private destroy$ = new Subject<void>();

  ngOnDestroy() {
    this.ngDestroy.next();
    this.ngDestroy.complete();
  }
}
