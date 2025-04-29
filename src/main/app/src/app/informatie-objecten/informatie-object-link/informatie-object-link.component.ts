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
import { MatDrawer } from "@angular/material/sidenav";
import { MatTableDataSource } from "@angular/material/table";
import { TranslateService } from "@ngx-translate/core";
import { Subject, takeUntil } from "rxjs";
import { UtilService } from "src/app/core/service/util.service";
import { InputFormFieldBuilder } from "src/app/shared/material-form-builder/form-components/input/input-form-field-builder";
import { AbstractFormControlField } from "src/app/shared/material-form-builder/model/abstract-form-control-field";
import { GeneratedType } from "src/app/shared/utils/generated-types";
import { ZoekenService } from "src/app/zoeken/zoeken.service";
import { InformatieObjectenService } from "../informatie-objecten.service";

enum DocumentAction {
  LINK = "actie.document.koppelen",
  MOVE = "actie.document.verplaatsen",
}

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
  @Input({ required: true }) actionLabel!: string;
  @Output() informationObjectLinked = new EventEmitter<void>();

  public intro: string = "";
  public caseSearchField?: AbstractFormControlField;
  public isValid = false;
  public loading = false;

  public documentAction!: DocumentAction;
  public actionIcon!: string;

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

  private ngDestroy = new Subject<void>();

  constructor(
    private zoekenService: ZoekenService,
    private informatieObjectService: InformatieObjectenService,
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

    this.documentAction =
      this.actionLabel === DocumentAction.LINK
        ? DocumentAction.LINK
        : DocumentAction.MOVE;

    this.actionIcon =
      this.documentAction === DocumentAction.LINK ? "link" : "move_item";
  }

  ngOnChanges(changes: SimpleChanges) {
    if (changes.infoObject && changes.infoObject.currentValue) {
      this.reset();
      this.intro = this.translate.instant("informatieobject.koppelen.uitleg", {
        documentID:
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
        () => {
          // error handling
          this.loading = false;
          this.utilService.setLoading(false);
        },
      );
  }

  selectCase(row: any) {
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

    const msgSnackbarKey =
      this.documentAction === DocumentAction.LINK
        ? "msg.document.koppelen.uitgevoerd"
        : "msg.document.verplaatsen.uitgevoerd";

    this.informatieObjectService
      .linkDocumentToCase(linkDocumentDetails)
      .pipe(takeUntil(this.ngDestroy))
      .subscribe({
        next: () => {
          this.utilService.openSnackbar(msgSnackbarKey, {
            document: this.infoObject.titel,
            case: linkDocumentDetails.nieuweZaakID,
          });
          this.closeDrawer();
          this.informationObjectLinked.emit();
        },
        error: () => {
          this.loading = false;
          this.utilService.setLoading(false);
        },
      });
  }

  closeDrawer() {
    this.sideNav.close();
    this.reset();
  }

  rowDisabled(row: GeneratedType<"RestZaakKoppelenZoekObject">): boolean {
    return !row.isKoppelbaar || row.identificatie == this.source;
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
