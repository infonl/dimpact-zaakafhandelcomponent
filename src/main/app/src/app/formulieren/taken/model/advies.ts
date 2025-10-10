/*
 * SPDX-FileCopyrightText: 2022 Atos, 2024 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
import { inject, Injectable } from "@angular/core";
import { Validators } from "@angular/forms";
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

  async handleForm(
    taak: GeneratedType<"RestTask">,
    zaak: GeneratedType<"RestZaak">,
  ): Promise<FormField[]> {
    return [
      {
        type: "plain-text",
        key: "intro",
        control: this.formBuilder.control("msg.advies.behandelen"),
      },
      {
        type: "documents",
        key: "relevanteDocumenten",
        options: taak.taakdata?.["relevanteDocumenten"]
          ? this.informatieObjectenService.listEnkelvoudigInformatieobjecten({
              zaakUUID: zaak.uuid,
              informatieobjectUUIDs: String(
                taak.taakdata?.["relevanteDocumenten"],
              ).split(";"),
            })
          : [],
        readonly: true,
      },
      {
        type: "radio",
        key: "advies",
        options: taak.tabellen["ADVIES"] ?? [],
        control: this.formBuilder.control(taak.taakdata?.["advies"], [
          Validators.required,
        ]),
      },
    ];
  }
}
