/*
 * SPDX-FileCopyrightText: 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

import { Injectable } from "@angular/core";
import { Validators } from "@angular/forms";
import { FormField } from "../../../shared/form/form";
import { GeneratedType } from "../../../shared/utils/generated-types";
import { AbstractTaakFormulier } from "./abstract-taak-formulier";

@Injectable({
  providedIn: "root",
})
export class ExternAdviesVastleggenTaskFields extends AbstractTaakFormulier {
  async requestForm(zaak: GeneratedType<"RestZaak">): Promise<FormField[]> {
    void zaak;
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
        type: "input",
        key: "adviseur",
        control: this.formBuilder.control("", [
          Validators.required,
          Validators.maxLength(1000),
        ]),
      },
      {
        type: "textarea",
        key: "bron",
        control: this.formBuilder.control("", [
          Validators.required,
          Validators.maxLength(1000),
        ]),
      },
    ];
  }

  async handleForm(taak: GeneratedType<"RestTask">): Promise<FormField[]> {
    void taak;
    return [
      {
        type: "plain-text",
        key: "intro",
        control: this.formBuilder.control(
          "msg.extern.advies.vastleggen.behandelen",
        ),
      },
      {
        type: "plain-text",
        key: "vraag",
        label: "vraag",
      },
      {
        type: "plain-text",
        key: "adviseur",
        label: "adviseur",
      },
      {
        type: "plain-text",
        key: "bron",
        label: "bron",
      },
      {
        type: "textarea",
        key: "externAdvies",
        control: this.formBuilder.control("", [
          Validators.required,
          Validators.maxLength(1000),
        ]),
      },
    ];
  }
}
