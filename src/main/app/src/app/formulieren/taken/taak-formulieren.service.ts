/*
 * SPDX-FileCopyrightText: 2021 - 2022 Atos, 2024 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

import {Injectable} from "@angular/core";
import {TranslateService} from "@ngx-translate/core";
import {InformatieObjectenService} from "../../informatie-objecten/informatie-objecten.service";
import {KlantenService} from "../../klanten/klanten.service";
import {MailtemplateService} from "../../mailtemplate/mailtemplate.service";
import {GeneratedType} from "../../shared/utils/generated-types";
import {TakenService} from "../../taken/taken.service";
import {ZakenService} from "../../zaken/zaken.service";
import {AanvullendeInformatie} from "./model/aanvullende-informatie";
import {Advies} from "./model/advies";
import {DefaultTaakformulier} from "./model/default-taakformulier";
import {DocumentVerzendenPost} from "./model/document-verzenden-post";
import {ExternAdviesMail} from "./model/extern-advies-mail";
import {ExternAdviesVastleggen} from "./model/extern-advies-vastleggen";
import {Goedkeuren} from "./model/goedkeuren";
import {TaakFormulierBuilder} from "./taak-formulier-builder";
import {FormField} from "../../shared/form/form";
import {FieldType} from "../../shared/material-form-builder/model/field-type.enum";
import {FormBuilder, FormControl, Validators} from "@angular/forms";

@Injectable({
  providedIn: "root",
})
export class TaakFormulierenService {
  constructor(
    private readonly translate: TranslateService,
    private readonly informatieObjectenService: InformatieObjectenService,
    private readonly takenService: TakenService,
    private readonly zakenService: ZakenService,
    private readonly mailtemplateService: MailtemplateService,
    private readonly klantenService: KlantenService,
    private readonly formBuilder: FormBuilder,
  ) {}

  public getNewFormBuilder(formulierDefinitie?: GeneratedType<"FormulierDefinitie"> | null): [FormControl, FormField][] {
    switch (formulierDefinitie) {
      case 'GOEDKEUREN':
        return [
          [
              this.formBuilder.control('', [Validators.required, Validators.maxLength(1000)]),
              { type: 'textarea', key: 'vraag'}
          ]
        ]
        break;
      default:
        throw new Error(`Onbekend formulier: ${formulierDefinitie}`);
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
        return new TaakFormulierBuilder(
          new AanvullendeInformatie(
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
