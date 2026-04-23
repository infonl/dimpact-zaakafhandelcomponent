/*
 * SPDX-FileCopyrightText: 2021 - 2022 Atos, 2024 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

import { inject, Injectable } from "@angular/core";
import { TranslateService } from "@ngx-translate/core";
import { InformatieObjectenService } from "../../informatie-objecten/informatie-objecten.service";
import { FormField } from "../../shared/form/form";
import { GeneratedType } from "../../shared/utils/generated-types";
import { TakenService } from "../../taken/taken.service";
import { AanvullendeInformatieTaskForm } from "./model/aanvullende-informatie-task-form";
import { AdviesTaskForm } from "./model/advies-task-form";
import { DefaultTaakformulier } from "./model/default-taakformulier";
import { DocumentVerzendenPost } from "./model/document-verzenden-post";
import { ExternAdviesMailTaskForm } from "./model/extern-advies-mail-task-form";
import { ExternAdviesVastleggenTaskForm } from "./model/extern-advies-vastleggen-task-form";
import { GoedkeurenTaskForm } from "./model/goedkeuren-task-form";
import { TaakFormulierBuilder } from "./taak-formulier-builder";

@Injectable({
  providedIn: "root",
})
export class TaakFormulierenService {
  private readonly translateService = inject(TranslateService);
  private readonly informatieObjectenService = inject(
    InformatieObjectenService,
  );
  private readonly takenService = inject(TakenService);

  private readonly goedkeurenFormulier = inject(GoedkeurenTaskForm);
  private readonly aanvullendeInformatieFormulier = inject(
    AanvullendeInformatieTaskForm,
  );
  private readonly adviesFormulier = inject(AdviesTaskForm);
  private readonly externAdviesVastleggenFormulier = inject(
    ExternAdviesVastleggenTaskForm,
  );
  private readonly externAdviesMailFormulier = inject(ExternAdviesMailTaskForm);

  public async getAngularRequestFormBuilder(
    zaak: GeneratedType<"RestZaak">,
    planItem?: GeneratedType<"RESTPlanItem">,
  ): Promise<FormField[]> {
    const formulierDefinitie = planItem?.formulierDefinitie;
    switch (formulierDefinitie) {
      case "GOEDKEUREN":
        return this.goedkeurenFormulier.requestForm(zaak);
      case "AANVULLENDE_INFORMATIE":
        return this.aanvullendeInformatieFormulier.requestForm(zaak, planItem!);
      case "ADVIES":
        return this.adviesFormulier.requestForm(zaak);
      case "EXTERN_ADVIES_VASTLEGGEN":
        return this.externAdviesVastleggenFormulier.requestForm(zaak);
      case "EXTERN_ADVIES_MAIL":
        return this.externAdviesMailFormulier.requestForm(zaak);
      default:
        throw new Error(
          `Onbekende formulierDefinitie for Angular form: ${formulierDefinitie}`,
        );
    }
  }

  public async getAngularHandleFormBuilder(
    taak: GeneratedType<"RestTask">,
    zaak: GeneratedType<"RestZaak">,
  ): Promise<FormField[]> {
    switch (taak.formulierDefinitieId) {
      case "GOEDKEUREN":
        return this.goedkeurenFormulier.handleForm(taak);
      case "AANVULLENDE_INFORMATIE":
        return this.aanvullendeInformatieFormulier.handleForm(taak, zaak);
      case "ADVIES":
        return this.adviesFormulier.handleForm(taak);
      case "EXTERN_ADVIES_VASTLEGGEN":
        return this.externAdviesVastleggenFormulier.handleForm(taak);
      case "EXTERN_ADVIES_MAIL":
        return this.externAdviesMailFormulier.handleForm(taak);
      default:
        throw new Error(
          `${taak.formulierDefinitieId}: Onbekende formulierDefinitie for Angular`,
        );
    }
  }

  public getFormulierBuilder(
    formulierDefinitie?: GeneratedType<"FormulierDefinitie"> | null,
  ): TaakFormulierBuilder {
    switch (formulierDefinitie) {
      case "DEFAULT_TAAKFORMULIER":
        return new TaakFormulierBuilder(
          new DefaultTaakformulier(
            this.translateService,
            this.informatieObjectenService,
          ),
        );
      case "AANVULLENDE_INFORMATIE":
        throw new Error(
          `${formulierDefinitie} is DEPRECATED, use Angular form`,
        );
      case "ADVIES":
        throw new Error(
          `${formulierDefinitie} is DEPRECATED, use Angular form`,
        );
      case "EXTERN_ADVIES_VASTLEGGEN":
        throw new Error(
          `${formulierDefinitie} is DEPRECATED, use Angular form`,
        );
      case "EXTERN_ADVIES_MAIL":
        throw new Error(
          `${formulierDefinitie} is DEPRECATED, use Angular form`,
        );
      case "GOEDKEUREN":
        throw new Error(
          `${formulierDefinitie} is DEPRECATED, use Angular form`,
        );
      case "DOCUMENT_VERZENDEN_POST":
        return new TaakFormulierBuilder(
          new DocumentVerzendenPost(
            this.translateService,
            this.takenService,
            this.informatieObjectenService,
          ),
        );
      default:
        throw new Error(`Onbekende formulierDefinitie: ${formulierDefinitie}`);
    }
  }
}
