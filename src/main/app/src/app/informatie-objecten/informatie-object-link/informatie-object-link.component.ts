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
import { MatDrawer } from "@angular/material/sidenav";
import { GeneratedType } from "../../shared/utils/generated-types";
import { AbstractFormControlField } from "src/app/shared/material-form-builder/model/abstract-form-control-field";
import { InputFormFieldBuilder } from "src/app/shared/material-form-builder/form-components/input/input-form-field-builder";
import { MatTableDataSource } from "@angular/material/table";
import { Subject, takeUntil } from "rxjs";
import { ZakenService } from "src/app/zaken/zaken.service";
import { OntkoppeldDocument } from "src/app/documenten/model/ontkoppeld-document";
import { TranslateService } from "@ngx-translate/core";

@Component({
  selector: "zac-informatie-object-link",
  templateUrl: "./informatie-object-link.component.html",
  styleUrls: ["./informatie-object-link.component.less"],
})
export class InformatieObjectLinkComponent
  implements OnInit, OnChanges, OnDestroy
{
  @Input() infoObject?: OntkoppeldDocument | null = null;
  @Input({ required: true }) sideNav!: MatDrawer;

  intro: string | undefined = "";
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

  constructor(
    private zakenService: ZakenService,
    private translate: TranslateService,
  ) {}

  ngOnInit() {
    this.caseSearchField = new InputFormFieldBuilder()
      .id("zaak")
      .label("zaak.identificatie")
      .build();

    this.caseSearchField.formControl.valueChanges
      .pipe(takeUntil(this.ngDestroy))
      .subscribe((value) => {
        this.isValid = value?.length > 2;
      });
  }

  ngOnChanges(changes: SimpleChanges): void {
    if (changes.infoObject && changes.infoObject.currentValue) {
      this.wissen();
      this.intro = this.translate.instant("informatieobject.koppelen.uitleg", {
        documentName:
          changes.infoObject.currentValue?.documentID ||
          changes.infoObject.currentValue?.enkelvoudiginformatieobjectID,
      });

      changes.infoObject.currentValue.documentID;
    }
  }

  searchCases(): void {
    this.loading = true;
    console.log("infoObject", this.infoObject);
    // this.zakenService
    //   .listZaaktypes(this.caseSearchField?.formControl.value)
    //   .subscribe((data) => {
    //     this.cases.data = data;
    //     this.loading = false;
    //   });
  }

  private wissen(): void {
    this.caseSearchField?.formControl.reset();
    this.cases.data = [];
    this.loading = false;
  }

  ngOnDestroy() {
    this.ngDestroy.next();
    this.ngDestroy.complete();
  }
}
