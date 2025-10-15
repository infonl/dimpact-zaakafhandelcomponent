/*
 * SPDX-FileCopyrightText: 2021 - 2022 Atos, 2024 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

import { inject, Injectable } from "@angular/core";
import { TranslateService } from "@ngx-translate/core";
import { InformatieObjectenService } from "../../informatie-objecten/informatie-objecten.service";
import { MailtemplateService } from "../../mailtemplate/mailtemplate.service";
import { FormField } from "../../shared/form/form";
import { GeneratedType } from "../../shared/utils/generated-types";
import { TakenService } from "../../taken/taken.service";
import { ZakenService } from "../../zaken/zaken.service";
import { AanvullendeInformatieFormulier } from "./model/aanvullende-informatie";
import { AdviesFormulier } from "./model/advies";
import { DefaultTaakformulier } from "./model/default-taakformulier";
import { DocumentVerzendenPost } from "./model/document-verzenden-post";
import { ExternAdviesMail } from "./model/extern-advies-mail";
import { ExternAdviesVastleggenFormulier } from "./model/extern-advies-vastleggen";
import { GoedkeurenFormulier } from "./model/goedkeuren";
import { TaakFormulierBuilder } from "./taak-formulier-builder";

@Injectable({
  providedIn: "root",
})
export class TaakFormulierenService {
  private readonly goedkeurenFormulier = inject(GoedkeurenFormulier);
  private readonly aanvullendeInformatieFormulier = inject(
    AanvullendeInformatieFormulier,
  );
  private readonly adviesFormulier = inject(AdviesFormulier);
  private readonly externAdviesVastleggenFormulier = inject(
    ExternAdviesVastleggenFormulier,
  );

  constructor(
    private readonly translate: TranslateService,
    private readonly informatieObjectenService: InformatieObjectenService,
    private readonly takenService: TakenService,
    private readonly zakenService: ZakenService,
    private readonly mailtemplateService: MailtemplateService,
  ) {}

  public async getAngularRequestFormBuilder(
    zaak: GeneratedType<"RestZaak">,
    formulierDefinitie?: GeneratedType<"FormulierDefinitie"> | null,
  ): Promise<FormField[]> {
    switch (formulierDefinitie) {
      case "AANVULLENDE_INFORMATIE":
        return this.aanvullendeInformatieFormulier.requestForm(zaak);
      case "ADVIES":
        return this.adviesFormulier.requestForm(zaak);
      case "EXTERN_ADVIES_VASTLEGGEN":
        return this.externAdviesVastleggenFormulier.requestForm();
      case "GOEDKEUREN":
        return this.goedkeurenFormulier.requestForm(zaak);
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
      case "AANVULLENDE_INFORMATIE":
        return this.aanvullendeInformatieFormulier.handleForm(taak, zaak);
      case "ADVIES":
        return this.adviesFormulier.handleForm(taak, zaak);
      case "EXTERN_ADVIES_VASTLEGGEN":
        return this.externAdviesVastleggenFormulier.handleForm();
      case "GOEDKEUREN":
        return this.goedkeurenFormulier.handleForm(taak);
      default:
        throw new Error(
          `${taak.formulierDefinitie}: Onbekende formulierDefinitie for Angular`,
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
            this.translate,
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
        return new TaakFormulierBuilder(
          new ExternAdviesMail(
            this.translate,
            this.takenService,
            this.informatieObjectenService,
            this.mailtemplateService,
            this.zakenService,
          ),
        );
      case "GOEDKEUREN":
        throw new Error(
          `${formulierDefinitie} is DEPRECATED, use Angular form`,
        );
      case "DOCUMENT_VERZENDEN_POST":
        return new TaakFormulierBuilder(
          new DocumentVerzendenPost(
            this.translate,
            this.takenService,
            this.informatieObjectenService,
          ),
        );
      default:
        throw new Error(`Onbekende formulierDefinitie: ${formulierDefinitie}`);
    }
  }
}
