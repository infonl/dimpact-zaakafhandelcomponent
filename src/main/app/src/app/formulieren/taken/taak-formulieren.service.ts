/*
 * SPDX-FileCopyrightText: 2021 - 2022 Atos, 2024 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

import { Injectable } from "@angular/core";
import { FormBuilder, FormControl, Validators } from "@angular/forms";
import { TranslateService } from "@ngx-translate/core";
import { lastValueFrom } from "rxjs";
import { InformatieObjectenService } from "../../informatie-objecten/informatie-objecten.service";
import { KlantenService } from "../../klanten/klanten.service";
import { MailtemplateService } from "../../mailtemplate/mailtemplate.service";
import { FormField } from "../../shared/form/form";
import { GeneratedType } from "../../shared/utils/generated-types";
import { TakenService } from "../../taken/taken.service";
import { ZakenService } from "../../zaken/zaken.service";
import { Goedkeuring } from "./goedkeuring.enum";
import { AanvullendeInformatie } from "./model/aanvullende-informatie";
import { Advies } from "./model/advies";
import { DefaultTaakformulier } from "./model/default-taakformulier";
import { DocumentVerzendenPost } from "./model/document-verzenden-post";
import { ExternAdviesMail } from "./model/extern-advies-mail";
import { ExternAdviesVastleggen } from "./model/extern-advies-vastleggen";
import { TaakFormulierBuilder } from "./taak-formulier-builder";

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

  public getAngularRequestFormBuilder(
    zaak: GeneratedType<"RestZaak">,
    formulierDefinitie?: GeneratedType<"FormulierDefinitie"> | null,
  ): [FormField, FormControl?][] {
    switch (formulierDefinitie) {
      case "GOEDKEUREN":
        return [
          [
            { type: "textarea", key: "vraag" },
            this.formBuilder.control("", [
              Validators.required,
              Validators.maxLength(1000),
            ]),
          ],
          [
            {
              type: "documents",
              key: "relevanteDocumenten",
              options:
                this.informatieObjectenService.listEnkelvoudigInformatieobjecten(
                  {
                    zaakUUID: zaak.uuid,
                  },
                ),
            },
            this.formBuilder.control(null),
          ],
        ];
      default:
        throw new Error(`Onbekende formulierDefinitie for Angular form: ${formulierDefinitie}`);
    }
  }

  public async getAngularHandleFormBuilder(
    taak: GeneratedType<"RestTask">,
  ): Promise<[FormField, FormControl?][]> {
    switch (taak.formulierDefinitieId) {
      case "GOEDKEUREN": {
        const goedkeuren = taak.taakdata?.["goedkeuren"] as string;
        const goedkeurenControl = this.formBuilder.control(goedkeuren, [
          Validators.required,
        ]);

        const checkedDocuments = (
          (taak.taakdata?.["ondertekenen"] as string) ?? ""
        ).split(";");
        const relevantDocumentUUIDs = taak.taakdata?.["relevanteDocumenten"]
          ? String(taak.taakdata?.["relevanteDocumenten"]).split(";")
          : [];

        const documentsToSign = await lastValueFrom(
          this.informatieObjectenService.listEnkelvoudigInformatieobjecten({
            zaakUUID: taak.zaakUuid,
            informatieobjectUUIDs: relevantDocumentUUIDs,
          }),
        );
        const initiallyCheckedDocuments = documentsToSign.filter((doc) =>
          checkedDocuments.includes(doc.uuid!),
        );
        const documentsToSignControl = this.formBuilder.control(
          initiallyCheckedDocuments,
        );
        return [
          [
            {
              type: "plain-text",
              text: this.translate.instant("msg.goedkeuring.behandelen", {
                zaaknummer: taak.zaakIdentificatie,
              }),
            },
          ],
          [
            {
              type: "plain-text",
              text: (taak.taakdata?.["vraag"] as string) ?? "",
              header: "vraag",
            },
          ],
          [
            {
              type: "documents",
              key: "ondertekenen",
              options: documentsToSign,
            },
            documentsToSignControl,
          ],
          [
            {
              type: "radio",
              key: "goedkeuren",
              options: Object.values(Goedkeuring).map(
                (value) => `goedkeuren.${value}`,
              ),
            },
            goedkeurenControl,
          ],
        ];
      }
      default:
        throw new Error(
          `Onbekende formulierDefinitie for Angular form: ${taak.formulierDefinitie}`,
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
        throw new Error("This form is DEPRECATED, use Angular form");
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
