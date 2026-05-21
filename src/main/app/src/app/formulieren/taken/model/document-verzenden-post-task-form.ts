/*
 * SPDX-FileCopyrightText: 2023 Atos, 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

import { inject, Injectable } from "@angular/core";
import { Validators } from "@angular/forms";
import moment from "moment";
import { lastValueFrom } from "rxjs";
import { mapStringToDocumentenStrings } from "../../../documenten/document-utils";
import { InformatieObjectenService } from "../../../informatie-objecten/informatie-objecten.service";
import { FormField } from "../../../shared/form/form";
import { GeneratedType } from "../../../shared/utils/generated-types";
import { AbstractTaskForm } from "./abstract-task-form";

@Injectable({
  providedIn: "root",
})
export class DocumentVerzendenPostTaskForm extends AbstractTaskForm {
  private readonly informatieObjectenService = inject(
    InformatieObjectenService,
  );

  async requestForm(zaak: GeneratedType<"RestZaak">): Promise<FormField[]> {
    const documenten = await lastValueFrom(
      this.informatieObjectenService.listInformatieobjectenVoorVerzenden(
        zaak.uuid,
      ),
    );

    return [
      {
        type: "documents",
        key: "documentenVerzendenPost",
        options: documenten,
        control: this.formBuilder.control<
          GeneratedType<"RestEnkelvoudigInformatieobject">[]
        >([], [Validators.required]),
        viewDocumentInNewTab: true,
      },
      {
        type: "textarea",
        key: "toelichting",
        control: this.formBuilder.control<string | null>(null, [
          Validators.maxLength(1000),
        ]),
      },
    ];
  }

  async handleForm(taak: GeneratedType<"RestTask">): Promise<FormField[]> {
    const documentUUIDs = mapStringToDocumentenStrings(
      taak.taakdata?.["documentenVerzendenPost"],
    );

    const documenten = documentUUIDs.length
      ? await lastValueFrom(
          this.informatieObjectenService.listEnkelvoudigInformatieobjecten({
            zaakUUID: taak.zaakUuid,
            informatieobjectUUIDs: documentUUIDs,
          }),
        )
      : [];

    const readonly = taak.status === "AFGEROND" || !taak.rechten?.wijzigen;
    const verzenddatum = taak.taakdata?.["verzenddatum"];

    return [
      {
        type: "plain-text",
        key: "intro",
        control: this.formBuilder.control(
          this.translateService.instant("msg.document.verzenden.post.behandelen"),
        ),
      },
      {
        type: "documents",
        key: "documentenVerzendenPost",
        options: documenten,
        control: this.formBuilder.control(documenten),
        readonly: true,
      },
      {
        type: "date",
        key: "verzenddatum",
        control: this.formBuilder.control(
          verzenddatum ? moment(verzenddatum) : moment(),
          [Validators.required],
        ),
        readonly,
      },
    ];
  }
}
