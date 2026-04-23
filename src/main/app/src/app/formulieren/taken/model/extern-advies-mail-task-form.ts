/*
 * SPDX-FileCopyrightText: 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

import { inject, Injectable } from "@angular/core";
import { Validators } from "@angular/forms";
import { lastValueFrom, takeUntil } from "rxjs";
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
  private readonly informatieObjectenService = inject(
    InformatieObjectenService,
  );

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
      .pipe(takeUntil(this.destroy$))
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
        control: this.formBuilder.control<string | null>(null, [
          Validators.required,
          CustomValidators.emails,
        ]),
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
