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
import { OntkoppeldDocument } from "src/app/documenten/model/ontkoppeld-document";
import { TranslateService } from "@ngx-translate/core";
import { UtilService } from "src/app/core/service/util.service";
import {
  KoppelbareZaakListItem,
  ZoekenService,
} from "src/app/zoeken/zoeken.service";

@Component({
  selector: "zac-informatie-object-link",
  templateUrl: "./informatie-object-link.component.html",
  styleUrls: ["./informatie-object-link.component.less"],
})
export class InformatieObjectLinkComponent
  implements OnInit, OnChanges, OnDestroy
{
  @Input() infoObject?:
    | (OntkoppeldDocument & { enkelvoudiginformatieobjectUUID?: string | null })
    | null = null;
  @Input({ required: true }) sideNav!: MatDrawer;

  intro: string | undefined = "";
  caseSearchField?: AbstractFormControlField;
  isValid: boolean = false;
  loading: boolean = false;

  cases = new MatTableDataSource<KoppelbareZaakListItem>();
  totalCases: number = 0;
  caseColumns: string[] = [
    "identificatie",
    "zaaktypeOmschrijving",
    "status",
    "registratiedatum",
    // "status",
    // "datum",
  ];

  private ngDestroy = new Subject<void>();

  constructor(
    private zoekenService: ZoekenService,
    private utilService: UtilService,
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
    if (
      !this.infoObject?.enkelvoudiginformatieobjectUUID &&
      !this.infoObject?.documentUUID
    )
      return;

    this.loading = true;
    this.utilService.setLoading(true);
    console.log("infoObject", this.infoObject);

    this.zoekenService
      .listKoppelbareZaken(
        this.caseSearchField?.formControl.value,
        this.infoObject?.enkelvoudiginformatieobjectUUID ||
          this.infoObject?.documentUUID,
      )
      .subscribe(
        (result) => {
          console.log("result", result);
          this.cases.data = result.resultaten;
          this.totalCases = result.totaal;
          this.loading = false;
          this.utilService.setLoading(false);
        },
        (error) => {
          console.error("Error fetching cases:", error);
          this.loading = false;
          this.utilService.setLoading(false);
        },
      );
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
