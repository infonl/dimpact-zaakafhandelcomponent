/*
 * SPDX-FileCopyrightText: 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

import {
  AbstractControl,
  FormArray,
  FormControl,
  FormGroup,
  FormRecord,
  ValidationErrors,
  Validators,
} from "@angular/forms";
import { GeneratedType } from "../../../../shared/utils/generated-types";
import { getBeschikbareMailtemplateKoppelingen } from "../../../model/mail-utils";

type StatusMailOption = GeneratedType<"ZaakafhandelparametersStatusMailOption">;

export type ZaakAfzenderForm = FormGroup<{
  id: FormControl<number | null>;
  mail: FormControl<string>;
  suffix: FormControl<string | null>;
  isSpeciaal: FormControl<boolean>;
  isDefaultMail: FormControl<boolean>;
  replyTo: FormControl<string | null>;
}>;

export type AutomaticEmailConfirmationForm = FormGroup<{
  id: FormControl<number | null>;
  isEnabled: FormControl<boolean>;
  templateName: FormControl<string | null>;
  emailSender: FormControl<string | null>;
  emailReply: FormControl<string | null>;
}>;

export type MailForm = FormGroup<{
  intakeMail: FormControl<StatusMailOption>;
  afrondenMail: FormControl<StatusMailOption>;
  mailtemplateKoppelingen: FormRecord<
    FormControl<GeneratedType<"RestMailtemplate"> | null>
  >;
  zaakAfzenders: FormArray<ZaakAfzenderForm>;
  automaticEmailConfirmation: AutomaticEmailConfirmationForm;
}>;

const hasDefaultAfzender = (
  zaakAfzenders: AbstractControl<{ isDefaultMail?: boolean }[]>,
): ValidationErrors | null =>
  zaakAfzenders.value.some(({ isDefaultMail }) => isDefaultMail)
    ? null
    : { noDefaultAfzender: true };

export function createZaakAfzenderForm({
  id,
  mail,
  suffix,
  isSpeciaal,
  isDefaultMail,
  replyTo,
}: GeneratedType<"RestZaakAfzender">): ZaakAfzenderForm {
  return new FormGroup({
    id: new FormControl(id ?? null),
    mail: new FormControl(mail ?? "", { nonNullable: true }),
    suffix: new FormControl(suffix ?? null),
    isSpeciaal: new FormControl(isSpeciaal ?? false, { nonNullable: true }),
    isDefaultMail: new FormControl(isDefaultMail ?? false, {
      nonNullable: true,
    }),
    replyTo: new FormControl(replyTo ?? null),
  });
}

function createAutomaticEmailConfirmationForm(
  automaticEmailConfirmation: GeneratedType<"RestAutomaticEmailConfirmation"> = {},
): AutomaticEmailConfirmationForm {
  const form: AutomaticEmailConfirmationForm = new FormGroup({
    id: new FormControl(automaticEmailConfirmation.id ?? null),
    isEnabled: new FormControl(automaticEmailConfirmation.isEnabled ?? false, {
      nonNullable: true,
    }),
    templateName: new FormControl(
      automaticEmailConfirmation.templateName ?? null,
    ),
    emailSender: new FormControl(
      automaticEmailConfirmation.emailSender ?? null,
    ),
    emailReply: new FormControl(automaticEmailConfirmation.emailReply ?? null),
  });

  const requireTemplateAndSender = (isEnabled: boolean) => {
    for (const control of [
      form.controls.templateName,
      form.controls.emailSender,
    ]) {
      control.setValidators(isEnabled ? [Validators.required] : []);
      // onlySelf, because revalidating the group while it is being disabled would enable it again; the group
      // revalidates itself right after this listener
      control.updateValueAndValidity({ onlySelf: true, emitEvent: false });
    }
  };
  requireTemplateAndSender(form.controls.isEnabled.value);
  form.updateValueAndValidity({ emitEvent: false });
  form.controls.isEnabled.valueChanges.subscribe(requireTemplateAndSender);

  return form;
}

export function createMailForm({
  intakeMail,
  afrondenMail,
  mailtemplateKoppelingen,
  zaakAfzenders,
  automaticEmailConfirmation,
}: Partial<
  Pick<
    GeneratedType<"RestZaaktypeConfiguration">,
    | "intakeMail"
    | "afrondenMail"
    | "mailtemplateKoppelingen"
    | "zaakAfzenders"
    | "automaticEmailConfirmation"
  >
>): MailForm {
  const koppelingen = new FormRecord<
    FormControl<GeneratedType<"RestMailtemplate"> | null>
  >({});
  for (const mail of getBeschikbareMailtemplateKoppelingen()) {
    koppelingen.addControl(
      mail,
      new FormControl(
        mailtemplateKoppelingen?.find(
          ({ mailtemplate }) => mailtemplate?.mail === mail,
        )?.mailtemplate ?? null,
      ),
    );
  }

  return new FormGroup({
    intakeMail: new FormControl<StatusMailOption>(
      intakeMail ?? "BESCHIKBAAR_UIT",
      { nonNullable: true, validators: [Validators.required] },
    ),
    afrondenMail: new FormControl<StatusMailOption>(
      afrondenMail ?? "BESCHIKBAAR_UIT",
      { nonNullable: true, validators: [Validators.required] },
    ),
    mailtemplateKoppelingen: koppelingen,
    zaakAfzenders: new FormArray(
      (zaakAfzenders ?? []).map(createZaakAfzenderForm),
      [hasDefaultAfzender],
    ),
    automaticEmailConfirmation: createAutomaticEmailConfirmationForm(
      automaticEmailConfirmation ?? undefined,
    ),
  });
}

export function toMailtemplateKoppelingen(
  form: MailForm,
): GeneratedType<"RESTMailtemplateKoppeling">[] {
  return Object.values(form.controls.mailtemplateKoppelingen.getRawValue())
    .filter((mailtemplate) => mailtemplate !== null)
    .map((mailtemplate) => ({ mailtemplate }));
}

export function toZaakAfzenders(
  form: MailForm,
): GeneratedType<"RestZaakAfzender">[] {
  return form.controls.zaakAfzenders.getRawValue();
}

export function toAutomaticEmailConfirmation(
  form: MailForm,
): GeneratedType<"RestAutomaticEmailConfirmation"> {
  return form.controls.automaticEmailConfirmation.getRawValue();
}
