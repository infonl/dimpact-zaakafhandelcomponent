/*
 * SPDX-FileCopyrightText: 2025 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

import { inject, Injectable } from "@angular/core";
import { FormGroup, Validators } from "@angular/forms";
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

  /**
   * What the taak actually signed: the documents it put forward for signing that carry a
   * signature now. Re-reading all relevante documenten and dropping the signed ones would
   * show exactly the opposite.
   */
  private async readSignedDocuments(taak: GeneratedType<"RestTask">) {
    const signedDocumentUUIDs = mapStringToDocumentenStrings(
      taak.taakdata?.["ondertekenen"],
    );

    const documents = await lastValueFrom(
      this.informatieObjectenService.listEnkelvoudigInformatieobjecten({
        zaakUUID: taak.zaakUuid,
        informatieobjectUUIDs: signedDocumentUUIDs,
      }),
    );

    return documents.filter((document) => document.ondertekening);
  }

  /** Already signed documents cannot be offered for signing again */
  private async readDocumentsAwaitingSignature(
    taak: GeneratedType<"RestTask">,
    relevantDocumentUUIDs: string[],
  ) {
    const documents = await lastValueFrom(
      this.informatieObjectenService.listEnkelvoudigInformatieobjecten({
        zaakUUID: taak.zaakUuid,
        informatieobjectUUIDs: relevantDocumentUUIDs,
      }),
    );

    return documents.filter((document) => !document.ondertekening);
  }

  /**
   * Completing signs the chosen documents, which changes what this form should show: the
   * record of what was signed instead of a choice of what to sign.
   */
  override async onTaskCompleted(
    taak: GeneratedType<"RestTask">,
    form: FormGroup,
    formFields: FormField[],
  ) {
    const ondertekenenField = formFields.find(
      (formField) => formField.key === "ondertekenen",
    );
    if (ondertekenenField?.type !== "documents") return;

    const signedDocuments = await this.readSignedDocuments(taak);
    ondertekenenField.options = signedDocuments;
    form.get("ondertekenen")?.setValue(signedDocuments);
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

    const documentsToSign = readonly
      ? await this.readSignedDocuments(taak)
      : await this.readDocumentsAwaitingSignature(taak, relevantDocumentUUIDs);

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
