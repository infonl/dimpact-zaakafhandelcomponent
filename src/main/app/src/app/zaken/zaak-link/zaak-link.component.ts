/*
 * SPDX-FileCopyrightText: 2025 Lifely
 * SPDX-License-Identifier: EUPL-1.2+
 */

import {
  Component,
  EventEmitter,
  Input,
  OnDestroy,
  OnInit,
  Output,
} from "@angular/core";
import { Validators } from "@angular/forms";
import { MatDrawer } from "@angular/material/sidenav";
import { MatTableDataSource } from "@angular/material/table";
import { TranslateService } from "@ngx-translate/core";
import { Subject, takeUntil } from "rxjs";
import { UtilService } from "src/app/core/service/util.service";
import { InputFormFieldBuilder } from "src/app/shared/material-form-builder/form-components/input/input-form-field-builder";
import { SelectFormFieldBuilder } from "src/app/shared/material-form-builder/form-components/select/select-form-field-builder";
import { AbstractFormControlField } from "src/app/shared/material-form-builder/model/abstract-form-control-field";
import { GeneratedType } from "src/app/shared/utils/generated-types";
import { ZoekenService } from "src/app/zoeken/zoeken.service";
import { Zaak } from "../model/zaak";
import { ZaakRelatietype } from "../model/zaak-relatietype";
import { ZakenService } from "../zaken.service";

/*
 enum/type below should come from our GeneratedType 
*/

@Component({
  selector: "zac-zaak-link",
  templateUrl: "./zaak-link.component.html",
  styleUrls: ["./zaak-link.component.less"],
})
export class ZaakLinkComponent implements OnInit, OnDestroy {
  @Input({ required: true }) zaak!: Zaak; // GeneratedType<"RestZaak">;
  @Input({ required: true }) sideNav!: MatDrawer;
  @Output() zaakLinked = new EventEmitter();

  public intro: string = "";
  public selectLinkTypeField?: AbstractFormControlField;
  public caseSearchField?: AbstractFormControlField;
  public isValid = false;
  public loading = false;

  public cases = new MatTableDataSource<
    GeneratedType<"RestZaakKoppelenZoekObject">
  >();
  public totalCases: number = 0;
  public caseColumns: string[] = [
    "identificatie",
    "zaaktypeOmschrijving",
    "statustypeOmschrijving",
    "omschrijving",
    "acties",
  ];

  private caseLinkingOptionsList!: { label: string; value: string }[];
  private ngDestroy = new Subject<void>();

  constructor(
    private zoekenService: ZoekenService,
    private zakenService: ZakenService,
    private translate: TranslateService,
    private utilService: UtilService,
  ) {}

  ngOnInit() {
    this.intro = this.translate.instant("zaak.koppelen.uitleg", {
      zaakID: this.zaak.identificatie,
    });

    this.caseLinkingOptionsList = [
      { label: "HOOFDZAAK", value: ZaakRelatietype.HOOFDZAAK },
      { label: "DEELZAAK", value: ZaakRelatietype.DEELZAAK },
    ];

    this.selectLinkTypeField = new SelectFormFieldBuilder()
      .id("linkType")
      .label("zaak.koppelen.label")
      .optionValue("value")
      .optionLabel("label")
      .options(this.caseLinkingOptionsList)
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
          case ZaakRelatietype.HOOFDZAAK:
            this.caseSearchField.formControl.enable();
            // set hint text here
            break;
          case ZaakRelatietype.DEELZAAK:
            this.caseSearchField.formControl.enable();
            // set hint text here
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
        this.zaak.uuid,
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

  selectCase(row: GeneratedType<"RestZaakKoppelenZoekObject">) {
    console.log("Selected case: ", row);
    if (!row.id || !this.selectLinkTypeField) return;

    const caseLinkDetails: GeneratedType<"RestZaakLinkData"> = {
      zaakUuid: this.zaak.uuid,
      teKoppelenZaakUuid: row.id,
      relatieType: this.selectLinkTypeField.formControl.value,
    };

    this.zakenService.koppelZaak(caseLinkDetails).subscribe({
      next: () => {
        this.utilService.openSnackbar("msg.zaak.gekoppeld", {
          case: row.identificatie,
        });
        this.zaakLinked.emit();
        this.close();
      },
      error: () => {
        this.loading = false;
        this.utilService.setLoading(false);
      },
    });
  }

  rowDisabled(row: GeneratedType<"RestZaakKoppelenZoekObject">): boolean {
    return (
      !row.documentKoppelbaar || row.identificatie === this.zaak.identificatie
    );
  }

  close() {
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
