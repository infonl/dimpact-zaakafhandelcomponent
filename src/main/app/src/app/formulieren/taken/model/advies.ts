/*
 * SPDX-FileCopyrightText: 2022 Atos, 2024 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

import { Validators } from "@angular/forms";
import { TranslateService } from "@ngx-translate/core";
import { of } from "rxjs";
import { InformatieObjectenService } from "../../../informatie-objecten/informatie-objecten.service";
import { DocumentenLijstFieldBuilder } from "../../../shared/material-form-builder/form-components/documenten-lijst/documenten-lijst-field-builder";
import { ParagraphFormFieldBuilder } from "../../../shared/material-form-builder/form-components/paragraph/paragraph-form-field-builder";
import { RadioFormFieldBuilder } from "../../../shared/material-form-builder/form-components/radio/radio-form-field-builder";
import { ReadonlyFormFieldBuilder } from "../../../shared/material-form-builder/form-components/readonly/readonly-form-field-builder";
import { TextareaFormFieldBuilder } from "../../../shared/material-form-builder/form-components/textarea/textarea-form-field-builder";
import { TakenService } from "../../../taken/taken.service";
import { AbstractTaakFormulier } from "../abstract-taak-formulier";

export class Advies extends AbstractTaakFormulier {
  fields = {
    VRAAG: "vraag",
    ADVIES: "advies",
    RELEVANTE_DOCUMENTEN: "relevanteDocumenten",
  };

  taakinformatieMapping = {
    uitkomst: this.fields.ADVIES,
    opmerking: AbstractTaakFormulier.TOELICHTING_FIELD,
  };

  constructor(
    translate: TranslateService,
    public takenService: TakenService,
    public informatieObjectenService: InformatieObjectenService,
  ) {
    super(translate, informatieObjectenService);
  }

  _initStartForm() {
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

  _initBehandelForm() {
    const fields = this.fields;
    this.form.push(
      [new ParagraphFormFieldBuilder().text("msg.advies.behandelen").build()],
      [
        new ReadonlyFormFieldBuilder(this.getDataElement(fields.VRAAG))
          .id(fields.VRAAG)
          .label(fields.VRAAG)
          .build(),
      ],
      [
        new DocumentenLijstFieldBuilder()
          .id(fields.RELEVANTE_DOCUMENTEN)
          .label(fields.RELEVANTE_DOCUMENTEN)
          .documenten(this.getDocumenten$(fields.RELEVANTE_DOCUMENTEN))
          .readonly(true)
          .build(),
      ],
      [
        new RadioFormFieldBuilder(this.getDataElement(fields.ADVIES))
          .id(fields.ADVIES)
          .label(fields.ADVIES)
          .options(this.tabellen["ADVIES"])
          .validators(Validators.required)
          .readonly(this.readonly)
          .build(),
      ],
    );
  }

  getDocumenten$(field: string) {
    const dataElement = this.getDataElement(field);
    if (!dataElement) return of([]);

    return this.informatieObjectenService.listEnkelvoudigInformatieobjecten({
      zaakUUID: this.zaak.uuid,
      informatieobjectUUIDs: dataElement.split(
        AbstractTaakFormulier.TAAK_DATA_MULTIPLE_VALUE_JOIN_CHARACTER,
      ),
    });
  }
}
