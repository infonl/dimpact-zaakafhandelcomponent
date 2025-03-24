/*
 * SPDX-FileCopyrightText: 2025 Lifely
 * SPDX-License-Identifier: EUPL-1.2+
 */

import {
  Component,
  Input,
  OnChanges,
  OnDestroy,
  OnInit,
  SimpleChanges,
} from "@angular/core";
import { MatDrawer, MatSidenav } from "@angular/material/sidenav";
import { GeneratedType } from "../../shared/utils/generated-types";
import { InformatieObjectenService } from "../informatie-objecten.service";
import { AbstractFormControlField } from "src/app/shared/material-form-builder/model/abstract-form-control-field";
import { InputFormFieldBuilder } from "src/app/shared/material-form-builder/form-components/input/input-form-field-builder";
import { MatTableDataSource } from "@angular/material/table";
import { Subject, takeUntil } from "rxjs";
import { ZakenService } from "src/app/zaken/zaken.service";
import { OntkoppeldDocument } from "src/app/documenten/model/ontkoppeld-document";

@Component({
  selector: "zac-informatie-object-link",
  templateUrl: "./informatie-object-link.component.html",
  styleUrls: ["./informatie-object-link.component.less"],
})
export class InformatieObjectLinkComponent
  implements OnInit, OnChanges, OnDestroy
{
  // @Input()
  // infoObject?: GeneratedType<"RestEnkelvoudigInformatieObjectVersieGegevens">;
  @Input() infoObject?: OntkoppeldDocument | null = null;
  @Input({ required: true }) sideNav!: MatDrawer;

  caseSearchField?: AbstractFormControlField;
  isValid: boolean = false;
  loading: boolean = false;

  cases = new MatTableDataSource<GeneratedType<"RestZaak">>();
  caseColumns: string[] = [
    "identificatie",
    "zaaktypeOmschrijving",
    "status",
    "registratiedatum",
  ];

  private ngDestroy = new Subject<void>();

  constructor(private zakenService: ZakenService) {}

  ngOnInit() {
    console.log("infoObject", this.infoObject);

    this.caseSearchField = new InputFormFieldBuilder()
      .id("zaak")
      .label("zaak.identificatie")
      .build();

    this.caseSearchField.formControl.valueChanges
      .pipe(takeUntil(this.ngDestroy))
      .subscribe((value) => {
        console.log("valueChanges", value);
        this.isValid = value?.length > 2;
      });
  }

  ngOnChanges(changes: SimpleChanges): void {
    if (changes["infoObject"] && changes["infoObject"].currentValue) {
      // React to the selected document change here
      this.handleDocumentChange(changes["infoObject"].currentValue);
    }
  }

  handleDocumentChange(infoObject: OntkoppeldDocument) {
    // Perform any action after the document has been selected
    console.log("Document selected:", infoObject);
    // Add any logic you need to handle the selected document
  }

  searchCases(): void {
    console.log("searchCases");
    this.loading = true;
    // this.zakenService
    //   .listZaaktypes(this.caseSearchField?.formControl.value)
    //   .subscribe((data) => {
    //     this.cases.data = data;
    //     this.loading = false;
    //   });
  }

  wissen(): void {
    this.caseSearchField?.formControl.reset();
    this.cases.data = [];
    this.loading = false;
  }

  private destroy$ = new Subject<void>();

  ngOnDestroy() {
    this.ngDestroy.next();
    this.ngDestroy.complete();
  }
}
