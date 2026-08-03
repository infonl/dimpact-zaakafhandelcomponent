/*
 * SPDX-FileCopyrightText: 2025 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

import { inject, Injectable } from "@angular/core";
import { Validators } from "@angular/forms";
import { lastValueFrom, map } from "rxjs";
import { mapStringToDocumentenStrings } from "../../../documenten/document-utils";
import { InformatieObjectenService } from "../../../informatie-objecten/informatie-objecten.service";
import { FormField } from "../../../shared/form/composed-form/form-field.types";
import { GeneratedType } from "../../../shared/utils/generated-types";
import { Goedkeuring } from "../goedkeuring.enum";
import { AbstractTaskForm } from "./abstract-task-form";

@Injectable({
  providedIn: "root",
})
export class GoedkeurenTaskForm extends AbstractTaskForm {
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
        options: this.informatieObjectenService
          .listEnkelvoudigInformatieobjecten({
            zaakUUID: zaak.uuid,
          })
          .pipe(
            map(
              (
                documenten: GeneratedType<"RestEnkelvoudigInformatieobject">[],
              ) => documenten.filter((document) => !document.ondertekening),
            ),
          ),
      },
    ];
  }

  async handleForm(taak: GeneratedType<"RestTask">): Promise<FormField[]> {
    const goedkeurenControl = this.formBuilder.control(
      taak.taakdata?.["goedkeuren"],
      [Validators.required],
    );

    const checkedDocuments = mapStringToDocumentenStrings(
      taak.taakdata?.["ondertekenen"],
    );

    const relevantDocumentUUIDs = mapStringToDocumentenStrings(
      taak.taakdata?.["relevanteDocumenten"],
    );

    const readonly = taak.status === "AFGEROND" || !taak.rechten?.wijzigen;

    // A read-only taak reports what it did: only the documents it put forward for signing
    // that actually carry a signature now. Re-reading all relevante documenten and dropping
    // the signed ones would show exactly the opposite.
    const documentUUIDs = readonly ? checkedDocuments : relevantDocumentUUIDs;

    const documents = await lastValueFrom(
      this.informatieObjectenService.listEnkelvoudigInformatieobjecten({
        zaakUUID: taak.zaakUuid,
        informatieobjectUUIDs: documentUUIDs,
      }),
    );

    const documentsToSign = readonly
      ? documents.filter((document) => document.ondertekening)
      : documents.filter((document) => !document.ondertekening);

    const initiallyCheckedDocuments = documentsToSign.filter((document) =>
      checkedDocuments.includes(document.uuid!),
    );

    const documentsToSignControl = this.formBuilder.control(
      initiallyCheckedDocuments,
    );

    return [
      {
        type: "plain-text",
        key: "intro",
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
