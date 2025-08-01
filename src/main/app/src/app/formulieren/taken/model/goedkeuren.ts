/*
 * SPDX-FileCopyrightText: 2022 Atos
 * SPDX-License-Identifier: EUPL-1.2+
 */

import { Validators } from "@angular/forms";
import { TranslateService } from "@ngx-translate/core";
import { Observable, of } from "rxjs";
import { InformatieObjectenService } from "../../../informatie-objecten/informatie-objecten.service";
import { DocumentenLijstFieldBuilder } from "../../../shared/material-form-builder/form-components/documenten-lijst/documenten-lijst-field-builder";
import { DocumentenOndertekenenFieldBuilder } from "../../../shared/material-form-builder/form-components/documenten-ondertekenen/documenten-ondertekenen-field-builder";
import { ParagraphFormFieldBuilder } from "../../../shared/material-form-builder/form-components/paragraph/paragraph-form-field-builder";
import { RadioFormFieldBuilder } from "../../../shared/material-form-builder/form-components/radio/radio-form-field-builder";
import { ReadonlyFormFieldBuilder } from "../../../shared/material-form-builder/form-components/readonly/readonly-form-field-builder";
import { TextareaFormFieldBuilder } from "../../../shared/material-form-builder/form-components/textarea/textarea-form-field-builder";
import { TakenService } from "../../../taken/taken.service";
import { AbstractTaakFormulier } from "../abstract-taak-formulier";
import { Goedkeuring } from "../goedkeuring.enum";

export class Goedkeuren extends AbstractTaakFormulier {
  private readonly GOEDKEUREN_ENUM_PREFIX: string = "goedkeuren.";

  fields = {
    VRAAG: "vraag",
    GOEDKEUREN: "goedkeuren",
    RELEVANTE_DOCUMENTEN: "relevanteDocumenten",
    ONDERTEKENEN: "ondertekenen",
  };

  taakinformatieMapping = {
    uitkomst: this.fields.GOEDKEUREN,
    opmerking: AbstractTaakFormulier.TOELICHTING_FIELD,
  };

  constructor(
    translate: TranslateService,
    public takenService: TakenService,
    public informatieObjectenService: InformatieObjectenService,
  ) {
    super(translate, informatieObjectenService);
  }

  protected _initStartForm() {
    const documenten =
      this.informatieObjectenService.listEnkelvoudigInformatieobjecten({
        zaakUUID: this.zaak.uuid,
      });
    const fields = this.fields;
    this.form.push(
      [
        new TextareaFormFieldBuilder()
          .id(fields.VRAAG)
          .label(fields.VRAAG)
          .validators(Validators.required)
          .maxlength(1000)
          .build(),
      ],
      [
        new DocumentenLijstFieldBuilder()
          .id(fields.RELEVANTE_DOCUMENTEN)
          .label(fields.RELEVANTE_DOCUMENTEN)
          .documenten(documenten)
          .openInNieuweTab()
          .build(),
      ],
    );
  }

  protected _initBehandelForm() {
    const fields = this.fields;
    const goedkeurenDataElement = this.getDataElement(fields.GOEDKEUREN);
    this.form.push(
      [
        new ParagraphFormFieldBuilder()
          .text(
            this.translate.instant("msg.goedkeuring.behandelen", {
              zaaknummer: this.taak.zaakIdentificatie,
            }),
          )
          .build(),
      ],
      [
        new ReadonlyFormFieldBuilder(this.getDataElement(fields.VRAAG))
          .id(fields.VRAAG)
          .label(fields.VRAAG)
          .build(),
      ],
      [
        new DocumentenOndertekenenFieldBuilder()
          .id(fields.ONDERTEKENEN)
          .label(fields.ONDERTEKENEN)
          .readonly(this.readonly)
          .documenten(this.getDocumenten$(fields.RELEVANTE_DOCUMENTEN))
          .documentenChecked(this.getDocumentenChecked(fields.ONDERTEKENEN))
          .build(),
      ],
      [
        new RadioFormFieldBuilder(
          this.readonly && goedkeurenDataElement
            ? this.translate.instant(goedkeurenDataElement)
            : goedkeurenDataElement,
        )
          .id(fields.GOEDKEUREN)
          .label(fields.GOEDKEUREN)
          .options(this.getGoedkeurenOpties$())
          .validators(Validators.required)
          .readonly(this.readonly)
          .build(),
      ],
    );
  }

  private getDocumenten$(field: string) {
    const dataElement = this.getDataElement(field);
    if (!dataElement) return of([]);

    return this.informatieObjectenService.listEnkelvoudigInformatieobjecten({
      zaakUUID: this.zaak.uuid,
      informatieobjectUUIDs: dataElement.split(
        AbstractTaakFormulier.TAAK_DATA_MULTIPLE_VALUE_JOIN_CHARACTER,
      ),
    });
  }

  private getDocumentenChecked(field: string): string[] {
    const dataElement = this.getDataElement(field);
    if (!dataElement) return [];

    return dataElement.split(
      AbstractTaakFormulier.TAAK_DATA_MULTIPLE_VALUE_JOIN_CHARACTER,
    );
  }

  private getGoedkeurenOpties$(): Observable<string[]> {
    return of(
      Object.keys(Goedkeuring).map(
        (k) =>
          this.GOEDKEUREN_ENUM_PREFIX +
          Goedkeuring[k as keyof typeof Goedkeuring],
      ),
    );
  }
}
