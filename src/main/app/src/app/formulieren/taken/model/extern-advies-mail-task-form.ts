/*
 * SPDX-FileCopyrightText: 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

import { DestroyRef, inject, Injectable } from "@angular/core";
import { takeUntilDestroyed } from "@angular/core/rxjs-interop";
import { Validators } from "@angular/forms";
import { lastValueFrom } from "rxjs";
import { KlantenService } from "src/app/klanten/klanten.service";
import { InformatieObjectenService } from "../../../informatie-objecten/informatie-objecten.service";
import { MailtemplateService } from "../../../mailtemplate/mailtemplate.service";
import { FormField } from "../../../shared/form/form";
import { GeneratedType } from "../../../shared/utils/generated-types";
import { CustomValidators } from "../../../shared/validators/customValidators";
import { ZakenService } from "../../../zaken/zaken.service";
import { OptionValue } from "../taak.utils";
import { AbstractTaskForm } from "./abstract-task-form";

@Injectable({
  providedIn: "root",
})
export class ExternAdviesMailTaskForm extends AbstractTaskForm {
  private readonly mailtemplateService = inject(MailtemplateService);
  private readonly zakenService = inject(ZakenService);
  private readonly klantenService = inject(KlantenService);
  private readonly informatieObjectenService = inject(
    InformatieObjectenService,
  );
  private readonly destroyRef = inject(DestroyRef);

  async requestForm(zaak: GeneratedType<"RestZaak">): Promise<FormField[]> {
    const replyToControl = this.formBuilder.control<string | null>(null);
    replyToControl.disable();

    const afzendersVoorZaak = await lastValueFrom(
      this.zakenService.listAfzendersVoorZaak(zaak.uuid),
    );
    const afzendersVoorZaakOptions = afzendersVoorZaak.map(
      (afzender: GeneratedType<"RestZaakAfzender">) =>
        ({
          ...afzender,
          key: afzender.mail,
          value: afzender.mail,
        }) satisfies OptionValue,
    );
    const verzenderControl = this.formBuilder.control<
      (GeneratedType<"RestZaakAfzender"> & OptionValue) | null
    >(null, [Validators.required]);
    verzenderControl.valueChanges
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe((value) => {
        replyToControl.patchValue(value?.replyTo ?? null);
      });

    const defaultAfzender = afzendersVoorZaakOptions.find(
      ({ defaultMail }: GeneratedType<"RestZaakAfzender">) => defaultMail,
    );
    verzenderControl.setValue(defaultAfzender ?? null);

    const mailTemplate = await lastValueFrom(
      this.mailtemplateService.findMailtemplate(
        "TAAK_ADVIES_EXTERN",
        zaak.uuid,
      ),
    );
    const htmlEditorControl = this.formBuilder.control<string>(
      mailTemplate.body,
      [Validators.required],
    );

    const emailadresControl = this.formBuilder.control<string | null>(null, [
      Validators.required,
      CustomValidators.emails,
    ]);
    const emailAddress = zaak.zaakSpecificContactDetails?.emailAddress;
    if (emailAddress) {
      emailadresControl.setValue(emailAddress);
    } else {
      const temporaryPersonId = zaak.initiatorIdentificatie?.temporaryPersonId;
      if (temporaryPersonId) {
        this.klantenService
          .getContactDetailsForPerson(temporaryPersonId)
          .pipe(takeUntilDestroyed(this.destroyRef))
          .subscribe((value) => {
            if (!value.emailadres) return;
            emailadresControl.setValue(value.emailadres);
          });
      }
    }

    return [
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
        control: this.formBuilder.control("TAAK_ADVIES_EXTERN"),
      },
      {
        type: "input",
        key: "adviseur",
        control: this.formBuilder.control<string | null>(null, [
          Validators.required,
          Validators.maxLength(1000),
        ]),
      },
      {
        type: "select",
        key: "verzender",
        options: afzendersVoorZaakOptions,
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
        control: emailadresControl,
      },
      {
        type: "html-editor",
        key: "body",
        control: htmlEditorControl,
        variables: mailTemplate.variabelen ?? [],
      },
      {
        type: "documents",
        key: "bijlagen",
        options:
          this.informatieObjectenService.listEnkelvoudigInformatieobjecten({
            zaakUUID: zaak.uuid,
          }),
      },
    ];
  }

  async handleForm(taak: GeneratedType<"RestTask">): Promise<FormField[]> {
    return [
      {
        type: "plain-text",
        key: "adviseur",
        label: "adviseur",
      },
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
        type: "textarea",
        key: "externAdvies",
        control: this.formBuilder.control(
          taak.taakdata?.["externAdvies"] ?? null,
          [Validators.required, Validators.maxLength(1000)],
        ),
        readonly: taak.status === "AFGEROND" || !taak.rechten?.wijzigen,
      },
    ];
  }
}
