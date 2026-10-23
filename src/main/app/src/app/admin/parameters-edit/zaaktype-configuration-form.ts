/*
 * SPDX-FileCopyrightText: 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

import { FormGroup } from "@angular/forms";
import { GeneratedType } from "../../shared/utils/generated-types";
import {
  ActiesForm,
  createActiesForm,
  toUserEventListenerParameters,
} from "./steps/parameters-step-acties/acties-form";
import {
  AlgemeenForm,
  createAlgemeenForm,
  toAlgemeenSettings,
} from "./steps/parameters-step-algemeen/algemeen-form";
import {
  createZaakbeeindigingForm,
  toZaakbeeindigParameters,
  ZaakbeeindigingForm,
} from "./steps/parameters-step-beeindiging/zaakbeeindiging-form";
import {
  createHumanTasksForm,
  HumanTasksForm,
  toHumanTaskParameters,
} from "./steps/parameters-step-humantasks/human-tasks-form";
import {
  createKoppelingenForm,
  KoppelingenForm,
  toBetrokkeneKoppelingen,
  toBrpDoelbindingen,
} from "./steps/parameters-step-koppelingen/koppelingen-form";
import {
  createMailForm,
  MailForm,
  toAutomaticEmailConfirmation,
  toMailtemplateKoppelingen,
  toZaakAfzenders,
} from "./steps/parameters-step-mail/mail-form";
import {
  createProcessModelForm,
  ProcessDefinition,
  ProcessModelForm,
  toProcessDefinition,
} from "./steps/parameters-step-proces-model/process-model-form";

type ZaaktypeConfiguration = GeneratedType<"RestZaaktypeConfiguration">;

export type ZaaktypeConfigurationForm = FormGroup<{
  procesModel: ProcessModelForm;
  algemeen: AlgemeenForm;
  humanTasks: HumanTasksForm;
  acties: ActiesForm;
  mail: MailForm;
  beeindiging: ZaakbeeindigingForm;
  koppelingen: KoppelingenForm;
}>;

const hasUserEventListener = (
  processDefinition: ProcessDefinition | null,
  actie: GeneratedType<"UserEventListenerActie">,
) =>
  Boolean(
    processDefinition?.userEventListenerDefinitions?.some(
      ({ id }) => id === actie,
    ),
  );

/**
 * Enables the settings that the process definition has use for and disables the others, so that only those count
 * for the validity of the form and are shown.
 */
export function applyProcessDefinition(
  form: ZaaktypeConfigurationForm,
  processDefinition: ProcessDefinition | null,
) {
  const { humanTasks, acties, mail } = form.controls;
  const isCmmn = processDefinition?.processEngine === "CMMN";
  const settingsInUse: [{ enable(): void; disable(): void }, boolean][] = [
    [humanTasks, Boolean(processDefinition?.humanTaskDefinitions?.length)],
    [acties, Boolean(processDefinition?.userEventListenerDefinitions?.length)],
    // The mail settings of a BPMN zaaktype are not released to beheerders yet; their stored values are still saved.
    // The group comes before its controls, because enabling the group enables them all.
    [mail, isCmmn],
    [
      mail.controls.intakeMail,
      isCmmn && hasUserEventListener(processDefinition, "INTAKE_AFRONDEN"),
    ],
    [
      mail.controls.afrondenMail,
      isCmmn && hasUserEventListener(processDefinition, "ZAAK_AFHANDELEN"),
    ],
    // A BPMN process sends the confirmation of receipt itself.
    [mail.controls.automaticEmailConfirmation, isCmmn],
  ];

  for (const [control, isInUse] of settingsInUse) {
    if (isInUse) {
      control.enable();
    } else {
      control.disable();
    }
  }
}

export function toForm(
  configuration: ZaaktypeConfiguration,
  { isBrpDoelbindingSetupEnabled }: { isBrpDoelbindingSetupEnabled: boolean },
): ZaaktypeConfigurationForm {
  const processDefinition = configuration.processDefinition ?? null;

  const form: ZaaktypeConfigurationForm = new FormGroup({
    procesModel: createProcessModelForm(processDefinition),
    algemeen: createAlgemeenForm(configuration),
    humanTasks: createHumanTasksForm(configuration.humanTaskParameters),
    acties: createActiesForm(configuration.userEventListenerParameters),
    mail: createMailForm(configuration),
    beeindiging: createZaakbeeindigingForm(configuration),
    koppelingen: createKoppelingenForm(configuration, {
      isBrpDoelbindingSetupEnabled,
    }),
  });

  applyProcessDefinition(form, processDefinition);
  if (processDefinition) form.controls.procesModel.disable();
  return form;
}

/**
 * Converts the form to the configuration to store. It carries every setting of every step, also of the steps that
 * the beheerder did not open.
 */
export function toRestConfiguration(
  form: ZaaktypeConfigurationForm,
  configuration: ZaaktypeConfiguration,
  {
    isSmartDocumentsEnabledForZaaktype,
  }: { isSmartDocumentsEnabledForZaaktype?: boolean } = {},
): ZaaktypeConfiguration {
  const { procesModel, humanTasks, acties, mail, beeindiging, koppelingen } =
    form.controls;

  return {
    id: configuration.id,
    zaaktype: configuration.zaaktype,
    creatiedatum: configuration.creatiedatum,
    processDefinition: toProcessDefinition(procesModel) ?? undefined,
    ...toAlgemeenSettings(form.controls.algemeen),
    intakeMail: mail.controls.intakeMail.value,
    afrondenMail: mail.controls.afrondenMail.value,
    humanTaskParameters: toHumanTaskParameters(humanTasks),
    userEventListenerParameters: toUserEventListenerParameters(acties),
    automaticEmailConfirmation: toAutomaticEmailConfirmation(mail),
    zaakNietOntvankelijkResultaattype:
      beeindiging.controls.zaakNietOntvankelijkResultaattype.value,
    zaakbeeindigParameters: toZaakbeeindigParameters(beeindiging),
    mailtemplateKoppelingen: toMailtemplateKoppelingen(mail),
    zaakAfzenders: toZaakAfzenders(mail),
    betrokkeneKoppelingen: toBetrokkeneKoppelingen(koppelingen),
    brpDoelbindingen: toBrpDoelbindingen(koppelingen),
    smartDocuments: {
      isEnabledGlobally: configuration.smartDocuments.isEnabledGlobally,
      isEnabledForZaaktype:
        isSmartDocumentsEnabledForZaaktype ??
        configuration.smartDocuments.isEnabledForZaaktype,
    },
  };
}
