/*
 * SPDX-FileCopyrightText: 2022 Atos, 2024 Lifely
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
import { FormGroup, Validators } from "@angular/forms";
import { MatDrawer } from "@angular/material/sidenav";
import { Subject } from "rxjs";
import { takeUntil } from "rxjs/operators";
import { UtilService } from "../../core/service/util.service";
import { InformatieObjectenService } from "../../informatie-objecten/informatie-objecten.service";
import { InformatieobjectZoekParameters } from "../../informatie-objecten/model/informatieobject-zoek-parameters";
import { DateFormField } from "../../shared/material-form-builder/form-components/date/date-form-field";
import { DateFormFieldBuilder } from "../../shared/material-form-builder/form-components/date/date-form-field-builder";
import { DocumentenLijstFieldBuilder } from "../../shared/material-form-builder/form-components/documenten-lijst/documenten-lijst-field-builder";
import { InputFormFieldBuilder } from "../../shared/material-form-builder/form-components/input/input-form-field-builder";
import { SelectFormFieldBuilder } from "../../shared/material-form-builder/form-components/select/select-form-field-builder";
import { TextareaFormFieldBuilder } from "../../shared/material-form-builder/form-components/textarea/textarea-form-field-builder";
import { AbstractFormField } from "../../shared/material-form-builder/model/abstract-form-field";
import { FormConfig } from "../../shared/material-form-builder/model/form-config";
import { FormConfigBuilder } from "../../shared/material-form-builder/model/form-config-builder";
import { GeneratedType } from "../../shared/utils/generated-types";
import { Zaak } from "../model/zaak";
import { ZakenService } from "../zaken.service";

@Component({
  selector: "zac-besluit-edit",
  templateUrl: "./besluit-edit.component.html",
  styleUrls: ["./besluit-edit.component.less"],
})
export class BesluitEditComponent implements OnInit, OnDestroy {
  formConfig: FormConfig;
  @Input() besluit: GeneratedType<"RestBesluit">;
  @Input() zaak: Zaak;
  @Input() sideNav: MatDrawer;
  @Output() besluitGewijzigd = new EventEmitter<boolean>();
  fields: Array<AbstractFormField[]>;
  private ngDestroy = new Subject<void>();

  constructor(
    private zakenService: ZakenService,
    private informatieObjectenService: InformatieObjectenService,
    public utilService: UtilService,
  ) {}

  ngOnInit(): void {
    this.formConfig = new FormConfigBuilder()
      .saveText("actie.wijzigen")
      .cancelText("actie.annuleren")
      .build();
    const resultaattypeField = new SelectFormFieldBuilder(
      this.zaak.resultaat.resultaattype,
    )
      .id("resultaattype")
      .label("resultaat")
      .optionLabel("naam")
      .validators(Validators.required)
      .options(this.zakenService.listResultaattypes(this.zaak.zaaktype.uuid))
      .build();
    const besluittypeField = new InputFormFieldBuilder(
      this.besluit.besluittype.naam,
    )
      .id("besluittype")
      .label("besluit")
      .disabled()
      .build();
    const toelichtingField = new TextareaFormFieldBuilder(
      this.besluit.toelichting,
    )
      .id("toelichting")
      .label("besluitToelichting")
      .maxlength(1000)
      .build();
    const ingangsdatumField = new DateFormFieldBuilder(
      this.besluit.ingangsdatum,
    )
      .id("ingangsdatum")
      .label("ingangsdatum")
      .validators(Validators.required)
      .build();
    const vervaldatumField = new DateFormFieldBuilder(this.besluit.vervaldatum)
      .id("vervaldatum")
      .label("vervaldatum")
      .minDate(ingangsdatumField.formControl.value)
      .build();
    const documentenField = new DocumentenLijstFieldBuilder()
      .id("documenten")
      .label("documenten")
      .documentenChecked(
        this.besluit.informatieobjecten
          ? this.besluit.informatieobjecten.map((i) => i.uuid)
          : [],
      )
      .documenten(this.listInformatieObjecten(this.besluit.besluittype.id))
      .build();
    const redenField = new InputFormFieldBuilder()
      .id("reden")
      .label("wijziging.reden")
      .maxlength(80)
      .validators(Validators.required)
      .build();

    this.fields = [
      [resultaattypeField],
      [besluittypeField],
      [ingangsdatumField],
      [vervaldatumField],
      [toelichtingField],
      [documentenField],
      [redenField],
    ];

    resultaattypeField.formControl.valueChanges
      .pipe(takeUntil(this.ngDestroy))
      .subscribe((value) => {
        if (value) {
          vervaldatumField.required = (
            value as GeneratedType<"RestResultaattype">
          ).vervaldatumBesluitVerplicht;
        }
      });
    ingangsdatumField.formControl.valueChanges
      .pipe(takeUntil(this.ngDestroy))
      .subscribe((value) => {
        (vervaldatumField as DateFormField).minDate = value;
      });

    besluittypeField.formControl.valueChanges
      .pipe(takeUntil(this.ngDestroy))
      .subscribe((value) => {
        documentenField.updateDocumenten(this.listInformatieObjecten(value.id));
      });
  }

  listInformatieObjecten(besluittypeUUID: string) {
    const zoekparameters = new InformatieobjectZoekParameters();
    zoekparameters.zaakUUID = this.zaak.uuid;
    zoekparameters.besluittypeUUID = besluittypeUUID;
    return this.informatieObjectenService.listEnkelvoudigInformatieobjecten(
      zoekparameters,
    );
  }

  onFormSubmit(formGroup: FormGroup): void {
    if (formGroup) {
      const gegevens: GeneratedType<"RestBesluitWijzigenGegevens"> = {
        besluitUuid: this.besluit.uuid,
        resultaattypeUuid: (
          formGroup.controls["resultaattype"]
            .value as GeneratedType<"RestResultaattype">
        ).id,
        toelichting: formGroup.controls["toelichting"].value,
        ingangsdatum: formGroup.controls["ingangsdatum"].value,
        vervaldatum: formGroup.controls["vervaldatum"].value,
        informatieobjecten: formGroup.controls["documenten"].value
          ? formGroup.controls["documenten"].value.split(";")
          : [],
        reden: formGroup.controls["reden"].value,
      };

      this.zakenService.updateBesluit(gegevens).subscribe(() => {
        this.utilService.openSnackbar("msg.besluit.gewijzigd");
        this.besluitGewijzigd.emit(true);
      });
    } else {
      this.besluitGewijzigd.emit(false);
    }
  }

  ngOnDestroy(): void {
    this.ngDestroy.next();
    this.ngDestroy.complete();
  }
}
