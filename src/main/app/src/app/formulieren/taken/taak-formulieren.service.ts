/*
 * SPDX-FileCopyrightText: 2021 - 2022 Atos, 2024 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

import { inject, Injectable } from "@angular/core";
import { TranslateService } from "@ngx-translate/core";
import { InformatieObjectenService } from "../../informatie-objecten/informatie-objecten.service";
import { FormField } from "../../shared/form/composed-form/form-field.types";
import { GeneratedType } from "../../shared/utils/generated-types";
import { AanvullendeInformatieTaskForm } from "./model/aanvullende-informatie-task-form";
import { AbstractTaskForm } from "./model/abstract-task-form";
import { AdviesTaskForm } from "./model/advies-task-form";
import { DefaultTaskForm } from "./model/default-task-form";
import { DocumentVerzendenPostTaskForm } from "./model/document-verzenden-post-task-form";
import { ExternAdviesMailTaskForm } from "./model/extern-advies-mail-task-form";
import { ExternAdviesVastleggenTaskForm } from "./model/extern-advies-vastleggen-task-form";
import { GoedkeurenTaskForm } from "./model/goedkeuren-task-form";

@Injectable({
  providedIn: "root",
})
export class TaakFormulierenService {
  private readonly translateService = inject(TranslateService);
  private readonly informatieObjectenService = inject(
    InformatieObjectenService,
  );

  private readonly goedkeurenTaskForm = inject(GoedkeurenTaskForm);
  private readonly aanvullendeInformatieTaskForm = inject(
    AanvullendeInformatieTaskForm,
  );
  private readonly adviesTaskForm = inject(AdviesTaskForm);
  private readonly externAdviesVastleggenTaskForm = inject(
    ExternAdviesVastleggenTaskForm,
  );
  private readonly externAdviesMailTaskForm = inject(ExternAdviesMailTaskForm);
  private readonly defaultTaskForm = inject(DefaultTaskForm);
  private readonly documentVerzendenPostTaskForm = inject(
    DocumentVerzendenPostTaskForm,
  );

  public async getAngularRequestFormBuilder(
    zaak: GeneratedType<"RestZaak">,
    planItem?: GeneratedType<"RESTPlanItem">,
  ): Promise<FormField[]> {
    const formulierDefinitie = planItem?.formulierDefinitie;
    switch (formulierDefinitie) {
      case "DEFAULT_TAAKFORMULIER":
        return this.defaultTaskForm.requestForm();
      case "GOEDKEUREN":
        return this.goedkeurenTaskForm.requestForm(zaak);
      case "AANVULLENDE_INFORMATIE":
        return this.aanvullendeInformatieTaskForm.requestForm(zaak, planItem!);
      case "ADVIES":
        return this.adviesTaskForm.requestForm(zaak);
      case "EXTERN_ADVIES_VASTLEGGEN":
        return this.externAdviesVastleggenTaskForm.requestForm(zaak);
      case "EXTERN_ADVIES_MAIL":
        return this.externAdviesMailTaskForm.requestForm(zaak);
      case "DOCUMENT_VERZENDEN_POST":
        return this.documentVerzendenPostTaskForm.requestForm(zaak);
      default:
        throw new Error(
          `Onbekende formulierDefinitie for Angular form: ${formulierDefinitie}`,
        );
    }
  }

  public getAngularTaskForm(
    formulierDefinitieId: GeneratedType<"RestTask">["formulierDefinitieId"],
  ): AbstractTaskForm {
    switch (formulierDefinitieId) {
      case "DEFAULT_TAAKFORMULIER":
        return this.defaultTaskForm;
      case "GOEDKEUREN":
        return this.goedkeurenTaskForm;
      case "AANVULLENDE_INFORMATIE":
        return this.aanvullendeInformatieTaskForm;
      case "ADVIES":
        return this.adviesTaskForm;
      case "EXTERN_ADVIES_VASTLEGGEN":
        return this.externAdviesVastleggenTaskForm;
      case "EXTERN_ADVIES_MAIL":
        return this.externAdviesMailTaskForm;
      case "DOCUMENT_VERZENDEN_POST":
        return this.documentVerzendenPostTaskForm;
      default:
        throw new Error(
          `${formulierDefinitieId}: Onbekende formulierDefinitie for Angular`,
        );
    }
  }

  public async getAngularHandleFormBuilder(
    taak: GeneratedType<"RestTask">,
    zaak: GeneratedType<"RestZaak">,
  ): Promise<FormField[]> {
    return this.getAngularTaskForm(taak.formulierDefinitieId).handleForm(
      taak,
      zaak,
    );
  }
}
