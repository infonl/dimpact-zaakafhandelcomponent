/*
 * SPDX-FileCopyrightText: 2021 - 2022 Atos
 * SPDX-License-Identifier: EUPL-1.2+
 */

import { Injectable, OnDestroy } from "@angular/core";
import { TranslateService } from "@ngx-translate/core";
import { Subject } from "rxjs";
import { FormulierDefinitieID } from "../../admin/model/formulier-definitie";
import { ZaakafhandelParametersService } from "../../admin/zaakafhandel-parameters.service";
import { InformatieObjectenService } from "../../informatie-objecten/informatie-objecten.service";
import { KlantenService } from "../../klanten/klanten.service";
import { MailtemplateService } from "../../mailtemplate/mailtemplate.service";
import { TakenService } from "../../taken/taken.service";
import { ZakenService } from "../../zaken/zaken.service";
import { AanvullendeInformatie } from "./model/aanvullende-informatie";
import { Advies } from "./model/advies";
import { DefaultTaakformulier } from "./model/default-taakformulier";
import { DocumentVerzendenPost } from "./model/document-verzenden-post";
import { ExternAdviesMail } from "./model/extern-advies-mail";
import { ExternAdviesVastleggen } from "./model/extern-advies-vastleggen";
import { Goedkeuren } from "./model/goedkeuren";
import { TaakFormulierBuilder } from "./taak-formulier-builder";

@Injectable({
  providedIn: "root",
})
export class TaakFormulierenService implements OnDestroy {
  constructor(
    private translate: TranslateService,
    private informatieObjectenService: InformatieObjectenService,
    private takenService: TakenService,
    private zakenService: ZakenService,
    private zaakafhandelParametersService: ZaakafhandelParametersService,
    private mailtemplateService: MailtemplateService,
    private klantenService: KlantenService,
  ) {}

  destroy$ = new Subject<void>();

  ngOnDestroy(): void {
    this.destroy$.next();
    this.destroy$.complete();
  }

  public getFormulierBuilder(
    formulierDefinitie: FormulierDefinitieID,
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
        return new TaakFormulierBuilder(
          new AanvullendeInformatie(
            this.destroy$,
            this.translate,
            this.takenService,
            this.informatieObjectenService,
            this.mailtemplateService,
            this.klantenService,
            this.zakenService,
          ),
        );
      case "ADVIES":
        return new TaakFormulierBuilder(
          new Advies(
            this.translate,
            this.takenService,
            this.informatieObjectenService,
            this.zakenService,
            this.zaakafhandelParametersService,
          ),
        );
      case "EXTERN_ADVIES_VASTLEGGEN":
        return new TaakFormulierBuilder(
          new ExternAdviesVastleggen(
            this.translate,
            this.takenService,
            this.informatieObjectenService,
          ),
        );
      case "EXTERN_ADVIES_MAIL":
        return new TaakFormulierBuilder(
          new ExternAdviesMail(
            this.destroy$,
            this.translate,
            this.takenService,
            this.informatieObjectenService,
            this.mailtemplateService,
            this.zakenService,
          ),
        );
      case "GOEDKEUREN":
        return new TaakFormulierBuilder(
          new Goedkeuren(
            this.translate,
            this.takenService,
            this.informatieObjectenService,
          ),
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
        throw new Error(`Onbekend formulier: ${formulierDefinitie}`);
    }
  }
}
