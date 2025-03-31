/*
 * SPDX-FileCopyrightText: 2025 Lifely
 * SPDX-License-Identifier: EUPL-1.2+
 */

import {
  Component,
  EventEmitter,
  Input,
  OnChanges,
  OnDestroy,
  OnInit,
  Output,
  SimpleChanges,
} from "@angular/core";
import { MatDialog } from "@angular/material/dialog";
import { MatDrawer } from "@angular/material/sidenav";
import { MatTableDataSource } from "@angular/material/table";
import { TranslateService } from "@ngx-translate/core";
import { Subject, takeUntil } from "rxjs";
import { UtilService } from "src/app/core/service/util.service";
import {
  ConfirmDialogComponent,
  ConfirmDialogData,
} from "src/app/shared/confirm-dialog/confirm-dialog.component";
import { InputFormFieldBuilder } from "src/app/shared/material-form-builder/form-components/input/input-form-field-builder";
import { AbstractFormControlField } from "src/app/shared/material-form-builder/model/abstract-form-control-field";
import { GeneratedType } from "src/app/shared/utils/generated-types";
import {
  DocumentKoppelbaarAanZaakListItem,
  ZoekenService,
} from "src/app/zoeken/zoeken.service";
import { InformatieObjectenService } from "../informatie-objecten.service";

@Component({
  selector: "zac-informatie-object-link",
  templateUrl: "./informatie-object-link.component.html",
  styleUrls: ["./informatie-object-link.component.less"],
})
export class InformatieObjectLinkComponent
  implements OnInit, OnChanges, OnDestroy
{
  @Input() infoObject!:
    | GeneratedType<"RESTOntkoppeldDocument">
    | GeneratedType<"RESTInboxDocument">
    | GeneratedType<"RestEnkelvoudigInformatieobject">;
  @Input({ required: true }) sideNav!: MatDrawer;
  @Input({ required: true }) source!: string;
  @Input({ required: true }) action!: string;
  @Output() informationObjectLinked = new EventEmitter<void>();

  intro: string = "";
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
    private zoekenService: ZoekenService,
    private informatieObjectService: InformatieObjectenService,
    public dialog: MatDialog,
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
        this.isValid = value?.length >= 2;
      });
  }

  ngOnChanges(changes: SimpleChanges) {
    if (changes.infoObject && changes.infoObject.currentValue) {
      this.reset();
      this.intro = this.translate.instant("informatieobject.koppelen.uitleg", {
        documentName:
          changes.infoObject.currentValue?.identificatie ||
          changes.infoObject.currentValue?.documentID ||
          changes.infoObject.currentValue?.enkelvoudiginformatieobjectID,
      });
    }
  }

  searchCases() {
    this.loading = true;
    this.utilService.setLoading(true);
    this.zoekenService
      .listDocumentKoppelbareZaken(
        this.caseSearchField?.formControl.value,
        this.infoObject.informatieobjectTypeUUID,
      )
      .subscribe(
        (result) => {
          this.cases.data = result.resultaten;
          this.totalCases = result.totaal;
          this.loading = false;
          this.utilService.setLoading(false);
        },
        (error) => {
          this.loading = false;
          this.utilService.setLoading(false);
        },
      );
  }

  selectCase(row: any) {
    const msgKey =
      this.action === "actie.document.koppelen"
        ? "msg.document.koppelen.bevestigen"
        : "msg.document.verplaatsen.bevestigen";
    const msgSnackbarKey =
      this.action === "actie.document.koppelen"
        ? "msg.document.koppelen.uitgevoerd"
        : "msg.document.verplaatsen.uitgevoerd";

    const linkDocumentDetails = {
      documentUUID:
        "uuid" in this.infoObject
          ? this.infoObject.uuid
          : "documentUUID" in this.infoObject
            ? this.infoObject.documentUUID
            : "enkelvoudiginformatieobjectUUID" in this.infoObject
              ? this.infoObject.enkelvoudiginformatieobjectUUID
              : "",
      documentTitel: this.infoObject.titel,
      bron: this.source,
      nieuweZaakID: row.identificatie,
    };

    this.dialog
      .open(ConfirmDialogComponent, {
        data: new ConfirmDialogData(
          {
            key: msgKey,
            args: {
              document: linkDocumentDetails.documentTitel,
              case: row.identificatie,
            },
          },
          this.informatieObjectService.linkDocumentToCase(linkDocumentDetails),
        ),
      })
      .afterClosed()
      .subscribe((result) => {
        if (result) {
          this.utilService.openSnackbar(msgSnackbarKey);
          this.closeDrawer();
          this.informationObjectLinked.emit();
        } else {
          this.loading = false;
          this.utilService.setLoading(false);
        }
      });
  }

  closeDrawer() {
    this.sideNav.close();
    this.reset();
  }

  rowDisabled(row: GeneratedType<"RestZaakKoppelenZoekObject">): boolean {
    return !row.documentKoppelbaar || row.identificatie == this.source;
  }

  private reset() {
    this.caseSearchField?.formControl.reset();
    this.cases.data = [];
    this.totalCases = 0;
    this.loading = false;
  }

  ngOnDestroy() {
    this.ngDestroy.next();
    this.ngDestroy.complete();
  }
}
