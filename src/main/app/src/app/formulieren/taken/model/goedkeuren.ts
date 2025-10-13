/*
 * SPDX-FileCopyrightText: 2025 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

import { inject, Injectable } from "@angular/core";
import { Validators } from "@angular/forms";
import { lastValueFrom } from "rxjs";
import { InformatieObjectenService } from "../../../informatie-objecten/informatie-objecten.service";
import { FormField } from "../../../shared/form/form";
import { GeneratedType } from "../../../shared/utils/generated-types";
import { Goedkeuring } from "../goedkeuring.enum";
import { AbstractTaakFormulier } from "./abstract-taak-formulier";

@Injectable({
  providedIn: "root",
})
export class GoedkeurenFormulier extends AbstractTaakFormulier {
  private informatieObjectenService = inject(InformatieObjectenService);

  async requestForm(zaak: GeneratedType<"RestZaak">): Promise<FormField[]> {
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
        options:
          this.informatieObjectenService.listEnkelvoudigInformatieobjecten({
            zaakUUID: zaak.uuid,
          }),
      },
    ];
  }

  async handleForm(taak: GeneratedType<"RestTask">): Promise<FormField[]> {
    const goedkeurenControl = this.formBuilder.control(
      taak.taakdata?.["goedkeuren"],
      [Validators.required],
    );

    const checkedDocuments = (
      (taak.taakdata?.["ondertekenen"] as string) ?? ""
    ).split(";");
    const relevantDocumentUUIDs = taak.taakdata?.["relevanteDocumenten"]
      ? String(taak.taakdata?.["relevanteDocumenten"]).split(";")
      : [];

    const documentsToSign = await lastValueFrom(
      this.informatieObjectenService.listEnkelvoudigInformatieobjecten({
        zaakUUID: taak.zaakUuid,
        informatieobjectUUIDs: relevantDocumentUUIDs,
      }),
    );
    const initiallyCheckedDocuments = documentsToSign.filter((doc) =>
      checkedDocuments.includes(doc.uuid!),
    );
    const documentsToSignControl = this.formBuilder.control(
      initiallyCheckedDocuments,
    );
    return [
      {
        type: "plain-text",
        key: "titel",
        control: this.formBuilder.control(
          this.translateService.instant("msg.goedkeuring.behandelen", {
            zaaknummer: taak.zaakIdentificatie,
          }),
        ),
      },
      {
        type: "plain-text",
        key: "vraag",
        label: "vraag",
      },
      {
        type: "documents",
        key: "ondertekenen",
        options: documentsToSign,
        control: documentsToSignControl,
      },
      {
        type: "radio",
        key: "goedkeuren",
        options: Object.values(Goedkeuring).map(
          (value) => `goedkeuren.${value}`,
        ),
        control: goedkeurenControl,
      },
    ];
  }
}
