/*
 * SPDX-FileCopyrightText: 2025 Lifely
 * SPDX-License-Identifier: EUPL-1.2+
 */

import { Component, Input, OnDestroy, OnInit } from "@angular/core";
import { Validators } from "@angular/forms";
import { MatDrawer } from "@angular/material/sidenav";
import { MatTableDataSource } from "@angular/material/table";
import { TranslateService } from "@ngx-translate/core";
import { Subject, takeUntil } from "rxjs";
import { UtilService } from "src/app/core/service/util.service";
import { ZaakLinkTypes } from "src/app/informatie-objecten/model/zaak-koppel-type.enum";
import { InputFormFieldBuilder } from "src/app/shared/material-form-builder/form-components/input/input-form-field-builder";
import { SelectFormFieldBuilder } from "src/app/shared/material-form-builder/form-components/select/select-form-field-builder";
import { AbstractFormControlField } from "src/app/shared/material-form-builder/model/abstract-form-control-field";
import { GeneratedType } from "src/app/shared/utils/generated-types";
import { ZoekenService } from "src/app/zoeken/zoeken.service";
import { Zaak } from "../model/zaak";

@Component({
  selector: "zac-zaak-link",
  templateUrl: "./zaak-link.component.html",
  styleUrls: ["./zaak-link.component.less"],
})
export class ZaakLinkComponent implements OnInit, OnDestroy {
  @Input({ required: true }) zaak!: Zaak; // GeneratedType<"RestZaak">;
  @Input({ required: true }) sideNav!: MatDrawer;

  private caseLinkingOptionsList!: { label: string; value: string }[];
  intro: string = "";
  selectLinkTypeField?: AbstractFormControlField;
  caseSearchField?: AbstractFormControlField;
  isValid: boolean = false;
  loading: boolean = false;

  cases = new MatTableDataSource<GeneratedType<"RestZaakKoppelenZoekObject">>();
  totalCases: number = 0;
  caseColumns: string[] = [
    "identificatie",
    "zaaktypeOmschrijving",
    "statustypeOmschrijving",
    "omschrijving",
    "acties",
  ];

  private ngDestroy = new Subject<void>();

  constructor(
    private translate: TranslateService,
    private zoekenService: ZoekenService,
    private utilService: UtilService,
  ) {}

  ngOnInit() {
    this.intro = this.translate.instant("zaak.koppelen.uitleg", {
      zaakID: this.zaak.identificatie,
    });

    this.caseLinkingOptionsList = this.utilService.getEnumAsSelectList(
      "zaak.koppelen.link.type",
      ZaakLinkTypes,
    );

    this.selectLinkTypeField = new SelectFormFieldBuilder()
      .id("linkType")
      .label("zaak.koppelen.label")
      .options(this.caseLinkingOptionsList)
      .optionValue("value")
      .optionLabel("label")
      .validators(Validators.required)
      .build();

    this.caseSearchField = new InputFormFieldBuilder()
      .id("zaak")
      .label("zaak.identificatie")
      .validators(Validators.required)
      .disabled(true)
      .build();

    this.selectLinkTypeField.formControl.valueChanges
      .pipe(takeUntil(this.ngDestroy))
      .subscribe((value) => {
        if (!this.caseSearchField) return;

        this.caseSearchField.formControl.setValue("");
        switch (value) {
          case "hoofdzaak":
            this.caseSearchField.formControl.enable();
            this.caseSearchField.hint.label = this.translate.instant(
              "zaak.link.hoofdzaak.hint",
            );
            break;
          case "deelzaak":
            this.caseSearchField.formControl.enable();
            this.caseSearchField.hint.label = this.translate.instant(
              "zaak.link.deelzaak.hint",
            );
            break;
          default:
            this.caseSearchField.formControl.disable();
            break;
        }
      });

    this.caseSearchField.formControl.valueChanges
      .pipe(takeUntil(this.ngDestroy))
      .subscribe((value) => {
        this.isValid = value?.length >= 2;
      });
  }

  searchCases() {
    this.loading = true;
    this.utilService.setLoading(true);
    this.zoekenService
      .listZaakKoppelbareZaken(
        this.caseSearchField?.formControl.value,
        this.selectLinkTypeField?.formControl.value,
      )
      .subscribe(
        (result) => {
          this.cases.data = result.resultaten;
          this.totalCases = result.totaal;
          this.loading = false;
          this.utilService.setLoading(false);
        },
        () => {
          // error handling
          this.loading = false;
          this.utilService.setLoading(false);
        },
      );
  }

  selectCase() {}

  closeDrawer() {
    this.sideNav.close();
    this.reset();
  }

  private reset() {
    this.selectLinkTypeField?.formControl.setValue("");
    this.caseSearchField?.formControl.setValue("");
    this.caseSearchField?.formControl.disable();
    this.cases.data = [];
    this.totalCases = 0;
    this.isValid = false;
    this.loading = false;
    this.utilService.setLoading(false);
  }

  ngOnDestroy() {
    this.ngDestroy.next();
    this.ngDestroy.complete();
  }
}
