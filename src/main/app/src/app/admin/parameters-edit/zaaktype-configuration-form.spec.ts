/*
 * SPDX-FileCopyrightText: 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

import { fromPartial } from "src/test-helpers";
import { GeneratedType } from "../../shared/utils/generated-types";
import {
  applyProcessDefinition,
  toForm,
  toRestConfiguration,
} from "./zaaktype-configuration-form";

type ZaaktypeConfiguration = GeneratedType<"RestZaaktypeConfiguration">;

const resultaattype = fromPartial<GeneratedType<"RestResultaattype">>({
  id: "fakeResultaattypeId",
  naam: "Afgehandeld",
});

const sharedSettings = {
  id: 12,
  zaaktype: fromPartial<GeneratedType<"RestZaaktypeOverzicht">>({
    uuid: "fakeZaaktypeUuid",
    omschrijving: "fakeOmschrijving",
  }),
  defaultGroepId: "fakeGroepId",
  defaultBehandelaarId: "fakeBehandelaarId",
  einddatumGeplandWaarschuwing: 3,
  uiterlijkeEinddatumAfdoeningWaarschuwing: 2,
  productaanvraagtype: "fakeProductaanvraagtype",
  zaakNietOntvankelijkResultaattype: resultaattype,
  zaakbeeindigParameters: [
    {
      id: 7,
      zaakbeeindigReden: { id: "fakeRedenId", naam: "Reden" },
      resultaattype,
    },
  ],
  mailtemplateKoppelingen: [
    {
      mailtemplate: fromPartial<GeneratedType<"RestMailtemplate">>({
        id: 4,
        mail: "ZAAK_ALGEMEEN",
        mailTemplateNaam: "fakeMailtemplate",
      }),
    },
  ],
  zaakAfzenders: [
    {
      id: 9,
      mail: "GEMEENTE",
      suffix: null,
      isSpeciaal: true,
      isDefaultMail: true,
      replyTo: null,
    },
  ],
  betrokkeneKoppelingen: {
    isBrpKoppelenEnabled: true,
    isKvkKoppelenEnabled: false,
  },
  brpDoelbindingen: {
    zoekWaarde: "fakeZoekWaarde",
    raadpleegWaarde: "fakeRaadpleegWaarde",
    verwerkingregisterWaarde: "fakeVerwerkingregisterWaarde",
  },
  smartDocuments: { isEnabledGlobally: true, isEnabledForZaaktype: true },
} satisfies Partial<ZaaktypeConfiguration>;

const caseDefinition = fromPartial<GeneratedType<"RestProcessDefinition">>({
  processEngine: "CMMN",
  key: "fakeCaseDefinitionKey",
  naam: "fakeCaseDefinition",
  humanTaskDefinitions: [{ id: "fakeTaskId", naam: "Taak" }],
  userEventListenerDefinitions: [
    { id: "INTAKE_AFRONDEN", naam: "Intake afronden" },
    {
      id: "ZAAK_AFHANDELEN",
      naam: "Zaak afhandelen",
      type: "USER_EVENT_LISTENER",
    },
  ],
});

const bpmnProcessDefinition = fromPartial<
  GeneratedType<"RestProcessDefinition">
>({
  processEngine: "BPMN",
  key: "fakeProcessDefinitionKey",
  naam: "fakeProcessDefinition",
});

const cmmnConfiguration = fromPartial<ZaaktypeConfiguration>({
  ...sharedSettings,
  processDefinition: caseDefinition,
  intakeMail: "BESCHIKBAAR_AAN",
  afrondenMail: "NIET_BESCHIKBAAR",
  humanTaskParameters: [
    {
      planItemDefinition: { id: "fakeTaskId", naam: "Taak" },
      formulierDefinitieId: "DEFAULT_TAAKFORMULIER",
      actief: true,
      referentieTabellen: [],
    },
  ],
  userEventListenerParameters: [
    {
      id: "INTAKE_AFRONDEN",
      naam: "Intake afronden",
      toelichting: "fakeToelichting",
    },
  ],
  automaticEmailConfirmation: {
    isEnabled: true,
    templateName: "fakeTemplate",
    emailSender: "GEMEENTE",
    emailReply: null,
  },
});

const bpmnConfiguration = fromPartial<ZaaktypeConfiguration>({
  ...sharedSettings,
  processDefinition: bpmnProcessDefinition,
});

const unsavedConfiguration = fromPartial<ZaaktypeConfiguration>({
  zaaktype: sharedSettings.zaaktype,
  defaultGroepId: null,
  smartDocuments: { isEnabledGlobally: false, isEnabledForZaaktype: false },
});

const brpSetup = { isBrpDoelbindingSetupEnabled: true };

describe("The configuration of a zaaktype in the admin screen", () => {
  describe.each([
    ["CMMN case definition", cmmnConfiguration],
    ["BPMN process definition", bpmnConfiguration],
  ])(
    "for a configuration that is bound to a %s",
    (_definition, configuration) => {
      it("stores every setting that applies to every process as it was read, also of the steps that are never opened", () => {
        const restConfiguration = toRestConfiguration(
          toForm(configuration, brpSetup),
          configuration,
        );

        expect(restConfiguration).toEqual(
          expect.objectContaining(sharedSettings),
        );
      });

      it("stores the process definition it is bound to", () => {
        const restConfiguration = toRestConfiguration(
          toForm(configuration, brpSetup),
          configuration,
        );

        expect(restConfiguration.processDefinition).toEqual(
          configuration.processDefinition,
        );
      });

      it("keeps the process definition it is bound to", () => {
        expect(
          toForm(configuration, brpSetup).controls.procesModel.disabled,
        ).toBe(true);
      });

      it("is valid as it was read", () => {
        expect(toForm(configuration, brpSetup).valid).toBe(true);
      });

      it("stores the SmartDocuments setting of the koppelingen step once that step was opened", () => {
        const restConfiguration = toRestConfiguration(
          toForm(configuration, brpSetup),
          configuration,
          { isSmartDocumentsEnabledForZaaktype: false },
        );

        expect(restConfiguration.smartDocuments).toEqual({
          isEnabledGlobally: true,
          isEnabledForZaaktype: false,
        });
      });

      it("stores a changed default groep and keeps the BRP doelbindingen", () => {
        const form = toForm(configuration, brpSetup);
        form.controls.algemeen.controls.defaultGroepId.setValue(
          "fakeOtherGroepId",
        );

        const restConfiguration = toRestConfiguration(form, configuration);

        expect(restConfiguration.defaultGroepId).toBe("fakeOtherGroepId");
        expect(restConfiguration.brpDoelbindingen).toEqual(
          sharedSettings.brpDoelbindingen,
        );
      });
    },
  );

  it("stores the settings of the plan items of a case definition", () => {
    const restConfiguration = toRestConfiguration(
      toForm(cmmnConfiguration, brpSetup),
      cmmnConfiguration,
    );

    expect(restConfiguration).toEqual(
      expect.objectContaining({
        intakeMail: "BESCHIKBAAR_AAN",
        afrondenMail: "NIET_BESCHIKBAAR",
        humanTaskParameters: [
          expect.objectContaining({
            planItemDefinition: { id: "fakeTaskId", naam: "Taak" },
            formulierDefinitieId: "DEFAULT_TAAKFORMULIER",
            actief: true,
          }),
        ],
        userEventListenerParameters: [
          {
            id: "INTAKE_AFRONDEN",
            naam: "Intake afronden",
            toelichting: "fakeToelichting",
          },
        ],
        automaticEmailConfirmation: {
          id: null,
          isEnabled: true,
          templateName: "fakeTemplate",
          emailSender: "GEMEENTE",
          emailReply: null,
        },
      }),
    );
  });

  it("does not demand the settings that a BPMN process definition has no use for", () => {
    const { controls, valid } = toForm(bpmnConfiguration, brpSetup);

    expect(controls.humanTasks.disabled).toBe(true);
    expect(controls.acties.disabled).toBe(true);
    expect(controls.mail.controls.intakeMail.disabled).toBe(true);
    expect(controls.mail.controls.afrondenMail.disabled).toBe(true);
    expect(controls.mail.controls.automaticEmailConfirmation.disabled).toBe(
      true,
    );
    expect(valid).toBe(true);
  });

  it("offers the mail on completing a plan item only for the plan items that the process definition has", () => {
    const form = toForm(cmmnConfiguration, brpSetup);

    applyProcessDefinition(form, {
      ...caseDefinition,
      userEventListenerDefinitions: [
        {
          id: "ZAAK_AFHANDELEN",
          naam: "Zaak afhandelen",
          type: "USER_EVENT_LISTENER",
        },
      ],
    });

    expect(form.controls.mail.controls.intakeMail.disabled).toBe(true);
    expect(form.controls.mail.controls.afrondenMail.enabled).toBe(true);
  });

  describe("for a configuration that was never saved", () => {
    it("lets the beheerder choose a process definition", () => {
      expect(
        toForm(unsavedConfiguration, brpSetup).controls.procesModel.enabled,
      ).toBe(true);
    });

    it("names no process definition and demands one", () => {
      const form = toForm(unsavedConfiguration, brpSetup);

      expect(form.controls.procesModel.value.processDefinition).toBeNull();
      expect(form.controls.procesModel.valid).toBe(false);
    });

    it("stores no process definition while none is chosen", () => {
      const restConfiguration = toRestConfiguration(
        toForm(unsavedConfiguration, brpSetup),
        unsavedConfiguration,
      );

      expect(restConfiguration.processDefinition).toBeUndefined();
    });
  });
});
