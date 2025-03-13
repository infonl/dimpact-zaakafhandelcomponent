/*
 * SPDX-FileCopyrightText: 2022 Atos
 * SPDX-License-Identifier: EUPL-1.2+
 */

import { FormGroup } from "@angular/forms";
import { TranslateService } from "@ngx-translate/core";
import { InformatieObjectenService } from "../../informatie-objecten/informatie-objecten.service";
import { InformatieobjectZoekParameters } from "../../informatie-objecten/model/informatieobject-zoek-parameters";
import { HumanTaskData } from "../../plan-items/model/human-task-data";
import { TaakStuurGegevens } from "../../plan-items/model/taak-stuur-gegevens";
import { DocumentenLijstFieldBuilder } from "../../shared/material-form-builder/form-components/documenten-lijst/documenten-lijst-field-builder";
import { TextareaFormFieldBuilder } from "../../shared/material-form-builder/form-components/textarea/textarea-form-field-builder";
import { AbstractFormField } from "../../shared/material-form-builder/model/abstract-form-field";
import { GeneratedType } from "../../shared/utils/generated-types";
import { Taak } from "../../taken/model/taak";
import { Taakinformatie } from "../../taken/model/taakinformatie";
import { Zaak } from "../../zaken/model/zaak";

export abstract class AbstractTaakFormulier {
  public static TAAK_TOEKENNING = "taakToekenning";
  protected static TAAK_FATALEDATUM = "taakFataledatum";
  protected static BIJLAGEN_FIELD = "bijlagen";
  protected static ONDERTEKENEN_FIELD = "ondertekenen";
  protected static TOELICHTING_FIELD = "toelichting";
  protected static TAAK_DATA_MULTIPLE_VALUE_JOIN_CHARACTER = ";";

  zaak: Zaak;
  taakNaam: string;
  humanTaskData: HumanTaskData;
  taak: Taak;
  tabellen: Record<string, string[]>;
  abstract taakinformatieMapping: {
    uitkomst: string;
    bijlagen?: string;
    opmerking?: string;
  };
  dataElementen: Record<string, string> = {};
  readonly: boolean;
  form: AbstractFormField[][];
  disablePartialSave = false;
  taakDocumenten: GeneratedType<"RestEnkelvoudigInformatieobject">[];

  protected constructor(
    protected translate: TranslateService,
    public informatieObjectenService: InformatieObjectenService,
  ) {}

  initStartForm() {
    this.humanTaskData.taakStuurGegevens = new TaakStuurGegevens();
    this.form = [];
    this._initStartForm();
  }

  initBehandelForm(readonly: boolean) {
    this.form = [];
    this.readonly = readonly;
    this._initBehandelForm();
    this.initToelichtingVeld();
    this.refreshTaakdocumentenEnBijlagen();
  }

  protected abstract _initStartForm();

  protected abstract _initBehandelForm();

  getBehandelTitel(): string {
    if (this.readonly) {
      return this.translate.instant(`title.taak.raadplegen`, {
        taak: this.taak.naam,
      });
    } else {
      return this.translate.instant(`title.taak.behandelen`, {
        taak: this.taak.naam,
      });
    }
  }

  getHumanTaskData(formGroup: FormGroup): HumanTaskData {
    const values = formGroup.value;
    const toekenning = values[AbstractTaakFormulier.TAAK_TOEKENNING];
    const fataledatum = values[AbstractTaakFormulier.TAAK_FATALEDATUM];
    const toelichting = values[AbstractTaakFormulier.TOELICHTING_FIELD];
    this.humanTaskData.medewerker = toekenning.medewerker;
    this.humanTaskData.groep = toekenning.groep;
    this.humanTaskData.fataledatum = fataledatum;
    this.humanTaskData.toelichting = toelichting;
    this.humanTaskData.taakdata = this.getDataElementen(formGroup);
    return this.humanTaskData;
  }

  getTaak(formGroup: FormGroup): Taak {
    this.taak.taakdata = this.getDataElementen(formGroup);
    this.taak.toelichting = this.getFormField(
      AbstractTaakFormulier.TOELICHTING_FIELD,
    ).formControl.value;
    this.taak.taakinformatie = this.getTaakinformatie(formGroup);
    return this.taak;
  }

  protected getDataElement(key: string): any {
    return key in this.dataElementen ? this.dataElementen[key] : null;
  }

  refreshTaakdocumentenEnBijlagen() {
    this.form.forEach((value, index) => {
      value.forEach((field) => {
        if (field.id === AbstractTaakFormulier.BIJLAGEN_FIELD) {
          this.form.splice(index, 1);
        }
      });
    });

    const bijlagen = this.dataElementen[AbstractTaakFormulier.BIJLAGEN_FIELD];
    const taakDocumenten$ = this.getTaakdocumentenEnBijlagen(bijlagen);

    this.form.push([
      new DocumentenLijstFieldBuilder()
        .id(AbstractTaakFormulier.BIJLAGEN_FIELD)
        .label(AbstractTaakFormulier.BIJLAGEN_FIELD)
        .documenten(taakDocumenten$)
        .readonly(true)
        .build(),
    ]);

    taakDocumenten$.subscribe((taakDocumenten) => {
      this.taakDocumenten = taakDocumenten;
    });
  }

  private getTaakdocumentenEnBijlagen(bijlagen: string) {
    const zoekParameters = new InformatieobjectZoekParameters();
    zoekParameters.zaakUUID = this.zaak.uuid;
    zoekParameters.informatieobjectUUIDs = [];

    if (this.taak?.taakdocumenten) {
      this.taak.taakdocumenten.forEach((uuid) => {
        zoekParameters.informatieobjectUUIDs.push(uuid);
      });
    }

    bijlagen?.split(";").forEach((uuid) => {
      zoekParameters.informatieobjectUUIDs.push(uuid);
    });

    return this.informatieObjectenService.listEnkelvoudigInformatieobjecten(
      zoekParameters,
    );
  }

  private getDocumentInformatie() {
    const documentNamen: string[] = [];

    this.taakDocumenten.forEach((taakDocument) => {
      documentNamen.push(taakDocument.titel);
    });

    return documentNamen.join(", ");
  }

  private getDataElementen(formGroup: FormGroup) {
    Object.entries(formGroup.value)
      .filter(([key]) => key !== AbstractTaakFormulier.TAAK_TOEKENNING)
      .filter(([key]) => key !== AbstractTaakFormulier.TAAK_FATALEDATUM)
      .filter(([key]) => key !== AbstractTaakFormulier.TOELICHTING_FIELD)
      .filter(
        ([key]) =>
          !this.isReadonlyFormField(key) ||
          key === AbstractTaakFormulier.ONDERTEKENEN_FIELD,
      )
      .forEach(([key, value]) => {
        if (typeof this.dataElementen[key] === "boolean") {
          this.dataElementen[key] = `${this.dataElementen[key]}`;
        } else {
          this.dataElementen[key] = value as any;
        }
      });
    return this.dataElementen;
  }

  private isReadonlyFormField(id: string): boolean {
    for (const fieldArray of this.form) {
      for (const field of fieldArray) {
        if (field.id === id) {
          return field.readonly;
        }
      }
    }
    return false;
  }

  private getTaakinformatie(formGroup: FormGroup): Taakinformatie {
    return {
      uitkomst: formGroup.controls[this.taakinformatieMapping.uitkomst]?.value,
      opmerking:
        formGroup.controls[this.taakinformatieMapping.opmerking]?.value,
      bijlagen: this.getDocumentInformatie(),
    };
  }

  getFormField(id: string): AbstractFormField {
    for (const fieldArray of this.form) {
      for (const field of fieldArray) {
        if (field.id === id) {
          return field;
        }
      }
    }
    throw new Error(`FormField: "${id}" not found!`);
  }

  private initToelichtingVeld(): void {
    this.form.push([
      new TextareaFormFieldBuilder(this.taak.toelichting)
        .id(AbstractTaakFormulier.TOELICHTING_FIELD)
        .label(AbstractTaakFormulier.TOELICHTING_FIELD)
        .readonly(this.readonly)
        .maxlength(1000)
        .build(),
    ]);
  }
}
