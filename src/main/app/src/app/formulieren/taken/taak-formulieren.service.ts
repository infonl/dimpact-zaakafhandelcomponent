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
import { Advies } from "./model/advies";
import { DefaultTaakformulier } from "./model/default-taakformulier";
import { DocumentVerzendenPost } from "./model/document-verzenden-post";
import { ExternAdviesMail } from "./model/extern-advies-mail";
import { ExternAdviesVastleggen } from "./model/extern-advies-vastleggen";
import { GoedkeurenFormulier } from "./model/goedkeuren";
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
  private readonly zakenService = inject(ZakenService);
  private readonly mailtemplateService = inject(MailtemplateService);

  private readonly goedkeurenFormulier = inject(GoedkeurenFormulier);
  private readonly aanvullendeInformatieFormulier = inject(
    AanvullendeInformatieFormulier,
  );

  public async getAngularRequestFormBuilder(
    zaak: GeneratedType<"RestZaak">,
    formulierDefinitie?: GeneratedType<"FormulierDefinitie"> | null,
  ): Promise<FormField[]> {
    switch (formulierDefinitie) {
      case "GOEDKEUREN":
        return this.goedkeurenFormulier.requestForm(zaak);
      case "AANVULLENDE_INFORMATIE":
        return this.aanvullendeInformatieFormulier.requestForm(zaak);
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
            this.translateService,
            this.informatieObjectenService,
          ),
        );
      case "AANVULLENDE_INFORMATIE":
        throw new Error(
          `${formulierDefinitie} is DEPRECATED, use Angular form`,
        );
      case "ADVIES":
        return new TaakFormulierBuilder(
          new Advies(
            this.translateService,
            this.takenService,
            this.informatieObjectenService,
          ),
        );
      case "EXTERN_ADVIES_VASTLEGGEN":
        return new TaakFormulierBuilder(
          new ExternAdviesVastleggen(
            this.translateService,
            this.takenService,
            this.informatieObjectenService,
          ),
        );
      case "EXTERN_ADVIES_MAIL":
        return new TaakFormulierBuilder(
          new ExternAdviesMail(
            this.translateService,
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
