/*
 * SPDX-FileCopyrightText: 2022 Atos, 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

import {
  Component,
  EventEmitter,
  Input,
  OnChanges,
  OnInit,
  Output,
} from "@angular/core";

import { FormBuilder, FormControl, FormGroup } from "@angular/forms";
import { MatDialog } from "@angular/material/dialog";
import { MatTableDataSource } from "@angular/material/table";
import { DateConditionals } from "src/app/shared/utils/date-conditionals";
import { TextIcon } from "../../shared/edit/text-icon";
import { IndicatiesLayout } from "../../shared/indicaties/indicaties.component";
import { GeneratedType } from "../../shared/utils/generated-types";
import { ZakenService } from "../zaken.service";
import { BesluitIntrekkenDialogComponent } from "./besluit-intrekken-dialog/besluit-intrekken-dialog.component";

@Component({
  selector: "zac-besluit-view",
  templateUrl: "./besluit-view.component.html",
  styleUrls: ["./besluit-view.component.less"],
  standalone: false,
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
