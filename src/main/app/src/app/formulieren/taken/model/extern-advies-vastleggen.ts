/*
 * SPDX-FileCopyrightText: 2022 Atos, 2025 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
import { Injectable } from "@angular/core";
import { Validators } from "@angular/forms";
import { FormField } from "../../../shared/form/form";
import { AbstractTaakFormulier } from "./abstract-taak-formulier";

@Injectable({
  providedIn: "root",
})
export class ExternAdviesVastleggenFormulier extends AbstractTaakFormulier {
  async requestForm(): Promise<FormField[]> {
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

  async handleForm(): Promise<FormField[]> {
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
