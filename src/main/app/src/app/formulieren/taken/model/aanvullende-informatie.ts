/*
 * SPDX-FileCopyrightText: 2021 - 2022 Atos, 2025 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

import { inject, Injectable } from "@angular/core";
import { Validators } from "@angular/forms";
import moment, { Moment } from "moment";
import { lastValueFrom, takeUntil } from "rxjs";
import { InformatieObjectenService } from "../../../informatie-objecten/informatie-objecten.service";
import { KlantenService } from "../../../klanten/klanten.service";
import { MailtemplateService } from "../../../mailtemplate/mailtemplate.service";
import { FormField } from "../../../shared/form/form";
import { GeneratedType } from "../../../shared/utils/generated-types";
import { ZakenService } from "../../../zaken/zaken.service";
import { AbstractTaakFormulier } from "./abstract-taak-formulier";

@Injectable({
  providedIn: "root",
})
export class AanvullendeInformatieFormulier extends AbstractTaakFormulier {
  private readonly mailtemplateService = inject(MailtemplateService);
  private readonly zakenService = inject(ZakenService);
  private readonly informatieObjectenService = inject(
    InformatieObjectenService,
  );
  private readonly klantenService = inject(KlantenService);

  async requestForm(zaak: GeneratedType<"RestZaak">): Promise<FormField[]> {
    const replyToControl = this.formBuilder.control<string | null>(null);
    replyToControl.disable();

    const afzendersVoorZaak = this.zakenService.listAfzendersVoorZaak(
      zaak.uuid,
    );
    const verzenderOptions = (
      await lastValueFrom(this.zakenService.listAfzendersVoorZaak(zaak.uuid))
    ).map((afzender) => ({
      ...afzender,
      key: afzender.id,
      value: afzender.mail,
    }));
    const verzenderControl =
      this.formBuilder.control<GeneratedType<"RestZaakAfzender"> | null>(null, [
        Validators.required,
      ]);
    verzenderControl.valueChanges
      .pipe(takeUntil(this.destroy$))
      .subscribe((value) => {
        replyToControl.patchValue(value?.replyTo ?? null);
      });

    afzendersVoorZaak.subscribe((options) => {
      const defaultAfzender = options.find(({ defaultMail }) => defaultMail);
      if (!defaultAfzender) return;

      verzenderControl.setValue(defaultAfzender);
    });

    const htmlEditorBody = await lastValueFrom(
      this.mailtemplateService.findMailtemplate(
        "TAAK_AANVULLENDE_INFORMATIE",
        zaak.uuid,
      ),
    );
    const htmlEditorControl = this.formBuilder.control(htmlEditorBody, [
      Validators.required,
    ]);

    const taakFataleDatumControl = this.formBuilder.control<Moment | null>(
      null,
      [Validators.min(moment().add(1, "day").startOf("day").valueOf())],
    );
    const messageControl = this.formBuilder.control(
      this.getMessageFieldLabel(zaak, null),
    );
    console.log(this.getMessageFieldLabel(zaak, null));
    // const messageControl = this.formBuilder.control(this.getMessageFieldLabel(zaak, moment(this.humanTaskData.fataledatum)))

    taakFataleDatumControl.valueChanges
      .pipe(takeUntil(this.destroy$))
      .subscribe((value) => {
        messageControl.setValue(
          this.getMessageFieldLabel(zaak, value ? moment(value) : null),
        );
      });

    const emailControl = this.formBuilder.control<string | null>(null, [
      Validators.required,
      Validators.email,
    ]);
    if (
      zaak.initiatorIdentificatie?.type &&
      zaak.initiatorIdentificatie?.bsnNummer
    ) {
      this.klantenService
        .ophalenContactGegevens(zaak.initiatorIdentificatie.bsnNummer)
        .subscribe((value) => {
          if (!value.emailadres) return;
          emailControl.setValue(value.emailadres);
        });
    }

    const formFields: FormField[] = [
      {
        type: "checkbox",
        key: "taakStuurGegevens.sendMail",
        hidden: true,
        control: this.formBuilder.control(true),
      },
      {
        type: "input",
        key: "taakStuurGegevens.mail",
        hidden: true,
        control: this.formBuilder.control("TAAK_AANVULLENDE_INFORMATIE"),
      },
      {
        type: "select",
        key: "verzender",
        options: verzenderOptions,
        optionDisplayValue: "mail",
        control: verzenderControl,
      },
      {
        type: "input",
        key: "replyTo",
        hidden: true,
        control: replyToControl,
      },
      {
        type: "input",
        key: "emailadres",
        control: emailControl,
      },
      {
        type: "html-editor",
        key: "body",
        control: htmlEditorControl,
      },
      {
        type: "date",
        key: "datumGevraagd",
        hidden: true,
        control: this.formBuilder.control(moment()),
      },
      {
        type: "documents",
        key: "bijlagen",
        options:
          this.informatieObjectenService.listEnkelvoudigInformatieobjecten({
            zaakUUID: zaak.uuid,
          }),
      },
      {
        type: "date",
        key: "taakFataledatum",
        label: "fataledatum",
        control: taakFataleDatumControl,
      },
      {
        type: "plain-text",
        key: "messageField",
        control: messageControl,
      },
    ];

    if (this.isZaakSuspendable(zaak)) {
      const zaakOpschortenControl = this.formBuilder.control(false);
      zaakOpschortenControl.valueChanges
        .pipe(takeUntil(this.destroy$))
        .subscribe((value) => {
          if (value) {
            taakFataleDatumControl.addValidators([Validators.required]);
          } else {
            taakFataleDatumControl.removeValidators([Validators.required]);
          }

          taakFataleDatumControl.updateValueAndValidity();
        });

      formFields.push({
        type: "checkbox",
        key: "zaakOpschorten",
        control: zaakOpschortenControl,
      });
    }

    return formFields;
  }

  async handleForm(
    taak: GeneratedType<"RestTask">,
    zaak: GeneratedType<"RestZaak">,
  ): Promise<FormField[]> {
    const datumGevraagdControl = this.formBuilder.control(
      taak.taakdata?.["datumGevraagd"],
    );
    datumGevraagdControl.disable();

    const formFields: FormField[] = [
      {
        type: "plain-text",
        key: "verzender",
        label: "verzender",
      },
      {
        type: "plain-text",
        key: "emailadres",
        label: "emailadres",
      },
      {
        type: "plain-text",
        key: "body",
        label: "body",
      },
      {
        type: "date",
        key: "datumGevraagd",
        readonly: true,
        control: datumGevraagdControl,
      },
      {
        type: "date",
        key: "datumGeleverd",
        control: this.formBuilder.control(taak.taakdata?.["datumGeleverd"], [
          Validators.required,
        ]),
      },
      {
        type: "radio",
        key: "aanvullendeInformatie",
        options: [
          "aanvullende-informatie.geleverd-akkoord",
          "aanvullende-informatie.geleverd-niet-akkoord",
          "aanvullende-informatie.niet-geleverd",
        ],
        control: this.formBuilder.control(
          taak.taakdata?.["aanvullendeInformatie"],
          [Validators.required],
        ),
      },
    ];

    if (this.toonHervatten(zaak, taak)) {
      formFields.push({
        type: "radio",
        key: "zaakHervatten",
        label: "actie.zaak.hervatten",
        options: ["zaak.hervatten.ja", "zaak.hervatten.nee"],
      });
    }

    return formFields;
  }

  private isZaakSuspendable(zaak: GeneratedType<"RestZaak">) {
    return Boolean(
      zaak.zaaktype.opschortingMogelijk &&
        !zaak.redenOpschorting &&
        !zaak.isHeropend &&
        zaak.rechten.behandelen &&
        !zaak.isEerderOpgeschort,
    );
  }

  private getMessageFieldLabel(
    zaak: GeneratedType<"RestZaak">,
    humanTaskDataFatalDate?: string | Moment | null,
  ): string {
    const fatalZaakDate =
      zaak.uiterlijkeEinddatumAfdoening &&
      moment(zaak.uiterlijkeEinddatumAfdoening);
    const suspendedTextSuffix = this.isZaakSuspendable(zaak)
      ? ""
      : ".opgeschort";

    if (!fatalZaakDate) {
      return `msg.taak.aanvullendeInformatie.fataleDatumZaak.leeg`;
    }

    if (!humanTaskDataFatalDate) {
      return `msg.taak.aanvullendeInformatie.fataleDatumTaak.overig${suspendedTextSuffix}`;
    }

    if (moment(humanTaskDataFatalDate).isAfter(fatalZaakDate)) {
      return `msg.taak.aanvullendeInformatie.fataleDatumTaak.overschreden${suspendedTextSuffix}`;
    }

    return `msg.taak.aanvullendeInformatie.fataleDatumTaak.overig${suspendedTextSuffix}`;
  }

  private toonHervatten(
    zaak: GeneratedType<"RestZaak">,
    taak: GeneratedType<"RestTask">,
  ) {
    if (taak?.status === "AFGEROND" || !taak?.rechten.wijzigen) {
      return Boolean(taak.taakdata?.["zaakHervatten"]);
    }
    return Boolean(zaak.isOpgeschort && zaak.rechten.behandelen);
  }
}
