/*
 * SPDX-FileCopyrightText: 2022 Atos, 2024 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

import { inject, Injectable } from "@angular/core";
import { Validators } from "@angular/forms";
import { lastValueFrom } from "rxjs";
import { mapStringToDocumentenStrings } from "../../../documenten/document-utils";
import { InformatieObjectenService } from "../../../informatie-objecten/informatie-objecten.service";
import { FormField } from "../../../shared/form/form";
import { GeneratedType } from "../../../shared/utils/generated-types";
import { AbstractTaakFormulier } from "./abstract-taak-formulier";

@Injectable({
  providedIn: "root",
})
export class AdviesFormulier extends AbstractTaakFormulier {
  private readonly informatieObjectenService = inject(
    InformatieObjectenService,
  );

  async requestForm(zaak: GeneratedType<"RestZaak">): Promise<FormField[]> {
    const documenten = await lastValueFrom(
      this.informatieObjectenService.listEnkelvoudigInformatieobjecten({
        zaakUUID: zaak.uuid,
      }),
    );

    return [
      {
        type: "textarea",
        key: "vraag",
        control: this.formBuilder.control("", [
          Validators.required,
          Validators.maxLength(1000),
        ]),
      },
      {
        type: "documents",
        key: "relevanteDocumenten",
        options: documenten,
        viewDocumentInNewTab: true,
      },
    ];
  }

  async handleForm(taak: GeneratedType<"RestTask">): Promise<FormField[]> {
    const relevanteDocumentenUUIDs = mapStringToDocumentenStrings(
      taak.taakdata?.["relevanteDocumenten"],
    );

    const relevanteDocumenten = await lastValueFrom(
      this.informatieObjectenService.listEnkelvoudigInformatieobjecten({
        zaakUUID: taak.zaakUuid,
        informatieobjectUUIDs: relevanteDocumentenUUIDs,
      }),
    );

    const adviesControl = this.formBuilder.control(taak.taakdata?.["advies"], [
      Validators.required,
    ]);

    return [
      {
        type: "plain-text",
        key: "titel",
        control: this.formBuilder.control(
          this.translateService.instant("msg.advies.behandelen"),
        ),
      },
      {
        type: "plain-text",
        key: "vraag",
        label: "vraag",
      },
      {
        type: "documents",
        key: "relevanteDocumenten",
        readonly: true,
        options: relevanteDocumenten,
      },
      {
        type: "radio",
        key: "advies",
        options: taak.tabellen?.["ADVIES"] ?? [],
        control: adviesControl,
      },
    ];
  }
}
