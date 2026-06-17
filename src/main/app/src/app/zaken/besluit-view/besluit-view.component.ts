/*
 * SPDX-FileCopyrightText: 2022 Atos, 2024 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

import { NgFor, NgIf } from "@angular/common";
import {
  Component,
  EventEmitter,
  Input,
  OnChanges,
  OnInit,
  Output,
} from "@angular/core";

import { FormBuilder, FormControl, FormGroup } from "@angular/forms";
import { MatButtonModule } from "@angular/material/button";
import { MatDialog } from "@angular/material/dialog";
import { MatExpansionModule } from "@angular/material/expansion";
import { MatIconModule } from "@angular/material/icon";
import { MatSortModule } from "@angular/material/sort";
import { MatTableDataSource, MatTableModule } from "@angular/material/table";
import { MatTabsModule } from "@angular/material/tabs";
import { MatTooltipModule } from "@angular/material/tooltip";
import { TranslateModule } from "@ngx-translate/core";
import { DateConditionals } from "src/app/shared/utils/date-conditionals";
import { TextIcon } from "../../shared/edit/text-icon";
import { ZacDocuments } from "../../shared/form/documents/documents";
import { BesluitIndicatiesComponent } from "../../shared/indicaties/besluit-indicaties/besluit-indicaties.component";
import { IndicatiesLayout } from "../../shared/indicaties/indicaties.component";
import { DatumPipe } from "../../shared/pipes/datum.pipe";
import { EmptyPipe } from "../../shared/pipes/empty.pipe";
import { MimetypeToExtensionPipe } from "../../shared/pipes/mimetypeToExtension.pipe";
import { ReadMoreComponent } from "../../shared/read-more/read-more.component";
import { StaticTextComponent } from "../../shared/static-text/static-text.component";
import { GeneratedType } from "../../shared/utils/generated-types";
import { ZakenService } from "../zaken.service";
import { BesluitIntrekkenDialogComponent } from "./besluit-intrekken-dialog/besluit-intrekken-dialog.component";

@Component({
  selector: "zac-besluit-view",
  templateUrl: "./besluit-view.component.html",
  styleUrls: ["./besluit-view.component.less"],
  standalone: true,
  imports: [
    NgFor,
    NgIf,
    MatButtonModule,
    MatExpansionModule,
    MatIconModule,
    MatSortModule,
    MatTableModule,
    MatTabsModule,
    MatTooltipModule,
    TranslateModule,
    DatumPipe,
    EmptyPipe,
    MimetypeToExtensionPipe,
    StaticTextComponent,
    ReadMoreComponent,
    BesluitIndicatiesComponent,
    ZacDocuments,
  ],
})
export class BesluitViewComponent implements OnInit, OnChanges {
  @Input({ required: true }) besluiten!: GeneratedType<"RestDecision">[];
  @Input({ required: true }) readonly!: boolean;
  @Output() besluitWijzigen = new EventEmitter<GeneratedType<"RestDecision">>();
  readonly indicatiesLayout = IndicatiesLayout;
  histories: Record<
    string,
    MatTableDataSource<GeneratedType<"RestTaskHistoryLine">>
  > = {};

  protected documentenForms: Record<
    string,
    FormGroup<{
      documenten: FormControl<
        GeneratedType<"RestEnkelvoudigInformatieobject">[] | null
      >;
    }>
  > = {};

  toolTipIcon = new TextIcon(
    DateConditionals.provideFormControlValue(DateConditionals.always),
    "info",
    "toolTip_icon",
    "",
    "pointer",
    true,
  );

  constructor(
    private zakenService: ZakenService,
    private dialog: MatDialog,
    private formBuilder: FormBuilder,
  ) {}

  ngOnInit(): void {
    if (this.besluiten.length > 0) {
      this.loadBesluitData(this.besluiten[0].uuid);
    }
  }

  ngOnChanges() {
    for (const historieKey in this.histories) {
      this.loadHistorie(historieKey);
    }
  }

  loadBesluitData(uuid: string) {
    if (!this.histories[uuid]) {
      this.loadHistorie(uuid);
    }

    this.documentenForms[uuid] ??= this.formBuilder.group({
      documenten: this.formBuilder.control<
        GeneratedType<"RestEnkelvoudigInformatieobject">[] | null
      >([]),
    });
  }

  private loadHistorie(uuid: string) {
    this.zakenService.listBesluitHistorie(uuid).subscribe((historie) => {
      this.histories[uuid] = new MatTableDataSource<
        GeneratedType<"RestTaskHistoryLine">
      >();
      this.histories[uuid].data = historie;
    });
  }

  isReadonly(besluit: GeneratedType<"RestDecision">) {
    return this.readonly || besluit.isIngetrokken;
  }

  intrekken(besluit: GeneratedType<"RestDecision">) {
    this.dialog.open(BesluitIntrekkenDialogComponent, { data: besluit });
  }
}
