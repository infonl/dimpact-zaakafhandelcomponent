/*
 * SPDX-FileCopyrightText: 2022 Atos, 2024 INFO.nl, 2025 INFO.nl
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

import { Validators } from "@angular/forms";
import { MatDialog } from "@angular/material/dialog";
import { MatTableDataSource } from "@angular/material/table";
import { TranslateService } from "@ngx-translate/core";
import moment from "moment";
import { Observable, of } from "rxjs";
import { DateConditionals } from "src/app/shared/utils/date-conditionals";
import { UtilService } from "../../core/service/util.service";
import { DialogData } from "../../shared/dialog/dialog-data";
import { DialogComponent } from "../../shared/dialog/dialog.component";
import { TextIcon } from "../../shared/edit/text-icon";
import { HistorieRegel } from "../../shared/historie/model/historie-regel";
import { IndicatiesLayout } from "../../shared/indicaties/indicaties.component";
import { DateFormFieldBuilder } from "../../shared/material-form-builder/form-components/date/date-form-field-builder";
import { DocumentenLijstFieldBuilder } from "../../shared/material-form-builder/form-components/documenten-lijst/documenten-lijst-field-builder";
import { DocumentenLijstFormField } from "../../shared/material-form-builder/form-components/documenten-lijst/documenten-lijst-form-field";
import { HiddenFormFieldBuilder } from "../../shared/material-form-builder/form-components/hidden/hidden-form-field-builder";
import { InputFormFieldBuilder } from "../../shared/material-form-builder/form-components/input/input-form-field-builder";
import { MessageFormFieldBuilder } from "../../shared/material-form-builder/form-components/message/message-form-field-builder";
import { MessageLevel } from "../../shared/material-form-builder/form-components/message/message-level.enum";
import { SelectFormFieldBuilder } from "../../shared/material-form-builder/form-components/select/select-form-field-builder";
import { GeneratedType } from "../../shared/utils/generated-types";
import { VervalReden } from "../model/vervalReden";
import { ZakenService } from "../zaken.service";

@Component({
  selector: "zac-besluit-view",
  templateUrl: "./besluit-view.component.html",
  styleUrls: ["./besluit-view.component.less"],
})
export class BesluitViewComponent implements OnInit, OnChanges {
  @Input({ required: true }) besluiten!: GeneratedType<"RestDecision">[];
  @Input({ required: true }) result!: GeneratedType<"RestZaakResultaat">;
  @Input({ required: true }) readonly!: boolean;
  @Output() besluitWijzigen = new EventEmitter<GeneratedType<"RestDecision">>();
  @Output() doIntrekking = new EventEmitter();
  readonly indicatiesLayout = IndicatiesLayout;
  histories: Record<string, MatTableDataSource<HistorieRegel>> = {};

  besluitInformatieobjecten: Record<string, DocumentenLijstFormField> = {};
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
    private translate: TranslateService,
    private utilService: UtilService,
  ) {}

  ngOnInit(): void {
    if (this.besluiten.length > 0) {
      this.loadBesluitData(this.besluiten[0].uuid);
    }
  }

  ngOnChanges() {
    for (const key in this.besluitInformatieobjecten) {
      const besluit = this.getBesluit(key);
      if (!besluit?.informatieobjecten) {
        return;
      }
      this.besluitInformatieobjecten[key].updateDocumenten(
        of(besluit.informatieobjecten!),
      );
    }

    for (const historieKey in this.histories) {
      this.loadHistorie(historieKey);
    }
  }

  loadBesluitData(uuid: string) {
    if (!this.histories[uuid]) {
      this.loadHistorie(uuid);
    }

    if (!this.besluitInformatieobjecten[uuid]) {
      const besluit = this.getBesluit(uuid);
      if (!besluit?.informatieobjecten) {
        return;
      }
      this.besluitInformatieobjecten[uuid] = new DocumentenLijstFieldBuilder()
        .id("documenten")
        .label("documenten")
        .documenten(of(besluit.informatieobjecten))
        .removeColumn("status")
        .readonly(true)
        .build();
    }
  }

  private loadHistorie(uuid: string) {
    this.zakenService.listBesluitHistorie(uuid).subscribe((historie) => {
      this.histories[uuid] = new MatTableDataSource<HistorieRegel>();
      this.histories[uuid].data = historie;
    });
  }

  private getBesluit(uuid: string) {
    return this.besluiten.find((value) => value.uuid === uuid);
  }

  isReadonly(besluit: GeneratedType<"RestDecision">) {
    return this.readonly || besluit.isIngetrokken;
  }

  intrekken(besluit: GeneratedType<"RestDecision">) {
    const dialogData = new DialogData({
      formFields: [
        this.maakIdField(besluit),
        this.maakVervaldatumField(besluit),
        this.maakVervalredenField(besluit),
        this.maakToelichtingField(),
        this.maakMessageField(besluit),
      ],
      callback: (results) => this.saveIntrekking(results),
      melding: this.translate.instant("msg.besluit.intrekken.melding"),
      confirmButtonActionKey: "actie.besluit.intrekken",
      icon: "stop_circle",
    });

    this.dialog.open(DialogComponent, { data: dialogData });
  }

  saveIntrekking(results: unknown): Observable<null> {
    this.doIntrekking.emit(results);
    return of(null);
  }

  private maakIdField(besluit: GeneratedType<"RestDecision">) {
    return new HiddenFormFieldBuilder(besluit.uuid).id("uuid").build();
  }

  private maakVervaldatumField(besluit: GeneratedType<"RestDecision">) {
    return new DateFormFieldBuilder(besluit.vervaldatum)
      .id("vervaldatum")
      .label("vervaldatum")
      .minDate(moment(besluit.ingangsdatum, moment.ISO_8601).toDate())
      .validators(Validators.required)
      .build();
  }

  private maakVervalredenField(besluit: GeneratedType<"RestDecision">) {
    const vervalRedenen = this.utilService.getEnumAsSelectListExceptFor(
      "besluit.vervalreden",
      VervalReden,
      [VervalReden.TIJDELIJK],
    );
    const vervalReden = besluit.vervalreden
      ? {
          label: this.translate.instant(
            "besluit.vervalreden." + besluit.vervalreden,
          ),
          value: String(besluit.vervalreden),
        }
      : null;
    return new SelectFormFieldBuilder(vervalReden)
      .id("vervalreden")
      .label("besluit.vervalreden")
      .optionLabel("label")
      .options(vervalRedenen)
      .validators(Validators.required)
      .build();
  }

  private maakToelichtingField() {
    return new InputFormFieldBuilder()
      .id("toelichting")
      .label("toelichting")
      .validators(Validators.required)
      .build();
  }

  private maakMessageField(besluit: GeneratedType<"RestDecision">) {
    const documentenVerstuurd = besluit.informatieobjecten?.some(
      ({ verzenddatum }) => verzenddatum != null,
    );
    return new MessageFormFieldBuilder(!!documentenVerstuurd)
      .id("documentenverstuurd")
      .level(MessageLevel.WARNING)
      .text("msg.besluit.documenten.verstuurd")
      .build();
  }
}
