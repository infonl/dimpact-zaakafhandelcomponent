/*
 * SPDX-FileCopyrightText: 2025 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

import {
  Component,
  EventEmitter,
  Input,
  OnChanges,
  OnInit,
  Output,
  SimpleChanges,
} from "@angular/core";
import { FormBuilder, Validators } from "@angular/forms";
import { MatDrawer } from "@angular/material/sidenav";
import { MatTableDataSource } from "@angular/material/table";
import { TranslateService } from "@ngx-translate/core";
import { UtilService } from "src/app/core/service/util.service";
import { GeneratedType } from "src/app/shared/utils/generated-types";
import { ZoekenService } from "src/app/zoeken/zoeken.service";
import { InformatieObjectenService } from "../informatie-objecten.service";

type DocumentAction = "actie.document.koppelen" | "actie.document.verplaatsen";

@Component({
  selector: "zac-informatie-object-link",
  templateUrl: "./informatie-object-link.component.html",
  styleUrls: ["./informatie-object-link.component.less"],
})
export class InformatieObjectLinkComponent implements OnInit, OnChanges {
  @Input() infoObject?: GeneratedType<
    | "RESTOntkoppeldDocument"
    | "RESTInboxDocument"
    | "RestEnkelvoudigInformatieobject"
  > | null = null;
  @Input({ required: true }) sideNav!: MatDrawer;
  @Input({ required: true }) source!: string;
  @Input({ required: true })
  actionLabel!: DocumentAction;
  @Output() informationObjectLinked = new EventEmitter<void>();

  protected intro = "";
  protected loading = false;

  protected actionIcon!: string;

  protected cases = new MatTableDataSource<
    GeneratedType<"RestZaakKoppelenZoekObject">
  >();
  protected totalCases = 0;
  protected caseColumns = [
    "identificatie",
    "zaaktypeOmschrijving",
    "statustypeOmschrijving",
    "omschrijving",
    "acties",
  ] as const;

  protected form = this.formBuilder.group({
    caseSearch: this.formBuilder.control<string | null>(null, [
      Validators.minLength(2),
    ]),
  });

  constructor(
    private readonly zoekenService: ZoekenService,
    private readonly informatieObjectService: InformatieObjectenService,
    private readonly utilService: UtilService,
    private readonly translate: TranslateService,
    private readonly formBuilder: FormBuilder,
  ) {}

  ngOnInit() {
    this.actionIcon =
      this.actionLabel === "actie.document.koppelen" ? "link" : "move_item";
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

  protected searchCases() {
    this.loading = true;
    this.utilService.setLoading(true);
    if (!this.infoObject?.informatieobjectTypeUUID) return;

    const { caseSearch } = this.form.value;
    this.zoekenService
      .listDocumentKoppelbareZaken({
        zaakIdentificator: caseSearch!,
        informationObjectTypeUuid: this.infoObject.informatieobjectTypeUUID,
      })
      .subscribe({
        next: (result) => {
          this.cases.data = result.resultaten;
          this.totalCases = result.totaal ?? 0;
          this.loading = false;
          this.utilService.setLoading(false);
        },
        error: () => {
          this.loading = false;
          this.utilService.setLoading(false);
        },
      });
  }

  protected selectCase(row: GeneratedType<"RestZaakKoppelenZoekObject">) {
    this.informatieObjectService
      .linkDocumentToCase({
        documentUUID: this.getDocumentUUID(),
        bron: this.source,
        nieuweZaakID: row.identificatie,
      })
      .subscribe({
        next: () => {
          const msgSnackbarKey =
            this.actionLabel === "actie.document.koppelen"
              ? "msg.document.koppelen.uitgevoerd"
              : "msg.document.verplaatsen.uitgevoerd";

          this.utilService.openSnackbar(msgSnackbarKey, {
            document: this.infoObject!.titel,
            case: row.identificatie,
          });
          this.close();
          this.informationObjectLinked.emit();
        },
        error: () => {
          this.loading = false;
          this.utilService.setLoading(false);
        },
      });
  }

  private getDocumentUUID() {
    if (!this.infoObject) return "";
    if ("uuid" in this.infoObject) return this.infoObject.uuid;
    if ("documentUUID" in this.infoObject) return this.infoObject.documentUUID;
    if ("enkelvoudiginformatieobjectUUID" in this.infoObject)
      return this.infoObject.enkelvoudiginformatieobjectUUID;
    return "";
  }

  protected close() {
    void this.sideNav.close();
    this.reset();
  }

  protected rowDisabled(row: GeneratedType<"RestZaakKoppelenZoekObject">) {
    return !row.isKoppelbaar || row.identificatie === this.source;
  }

  protected reset() {
    this.form.reset();
    this.cases.data = [];
    this.totalCases = 0;
    this.loading = false;
  }
}
