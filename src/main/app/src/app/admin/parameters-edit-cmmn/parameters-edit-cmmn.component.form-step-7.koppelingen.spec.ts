/*
 * SPDX-FileCopyrightText: 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

import { NoopAnimationsModule } from "@angular/platform-browser/animations";
import { ActivatedRoute, RouterModule } from "@angular/router";
import { TranslateModule } from "@ngx-translate/core";
import { render, screen, within } from "@testing-library/angular";
import userEvent from "@testing-library/user-event";
import { of } from "rxjs";
import { InformatieObjectenService } from "src/app/informatie-objecten/informatie-objecten.service";
import {
  createMutationOptions,
  createQueryOptions,
  fromPartial,
} from "src/test-helpers";
import { sleep } from "../../../../setupJest";
import { ConfiguratieService } from "../../configuratie/configuratie.service";
import { IdentityService } from "../../identity/identity.service";
import { GeneratedType } from "../../shared/utils/generated-types";
import { MailtemplateBeheerService } from "../mailtemplate-beheer.service";
import { ReferentieTabelService } from "../referentie-tabel.service";
import { SmartDocumentsService } from "../smart-documents.service";
import { ZaakafhandelParametersService } from "../zaakafhandel-parameters.service";
import { ParametersEditCmmnComponent } from "./parameters-edit-cmmn.component";

// rendering this seven step form once per test needs more room than the default timeout
describe("Koppelingen form step", () => {
  const user = userEvent.setup();

  const caseDefinition = fromPartial<GeneratedType<"RESTCaseDefinition">>({
    key: "case-1",
    naam: "Case Definition 1",
  });

  function createParameters(
    overrides: Partial<GeneratedType<"RestZaaktypeConfiguration">> = {},
  ) {
    return fromPartial<GeneratedType<"RestZaaktypeConfiguration">>({
      caseDefinition,
      defaultGroepId: "test-group-id",
      defaultBehandelaarId: "test-user-id",
      zaaktype: { uuid: "test-uuid" },
      zaakNietOntvankelijkResultaattype: {
        id: "resultaat-1",
        naam: "Afgehandeld",
      },
      zaakAfzenders: [
        {
          isSpeciaal: false,
          isDefaultMail: true,
          mail: "test@example.com",
          replyTo: undefined,
        },
      ],
      humanTaskParameters: [],
      mailtemplateKoppelingen: [],
      zaakbeeindigParameters: [],
      smartDocuments: { isEnabledGlobally: false, isEnabledForZaaktype: false },
      userEventListenerParameters: [],
      betrokkeneKoppelingen: {
        isBrpKoppelenEnabled: false,
        isKvkKoppelenEnabled: false,
      },
      brpDoelbindingen: {
        zoekWaarde: "",
        raadpleegWaarde: "",
        verwerkingregisterWaarde: "",
      },
      productaanvraagtype: null,
      automaticEmailConfirmation: {
        isEnabled: false,
        templateName: null,
        emailSender: null,
        emailReply: null,
      },
      ...overrides,
    });
  }

  async function setup(parameters = createParameters()) {
    const updateZaakafhandelparameters = createMutationOptions(parameters);

    const { fixture } = await render(ParametersEditCmmnComponent, {
      imports: [TranslateModule.forRoot(), RouterModule, NoopAnimationsModule],
      providers: [
        {
          provide: ActivatedRoute,
          useValue: fromPartial<ActivatedRoute>({
            data: of({
              parameters: {
                zaakafhandelParameters: parameters,
                isSavedZaakafhandelParameters: true,
              },
            }),
          }),
        },
        {
          provide: ZaakafhandelParametersService,
          useValue: fromPartial<ZaakafhandelParametersService>({
            listCaseDefinitions: () => of([caseDefinition]),
            listFormulierDefinities: () => of([]),
            listReplyTos: () => of([{ mail: "reply1@example.com" }]),
            updateZaakafhandelparameters: () => updateZaakafhandelparameters,
          }),
        },
        {
          provide: ReferentieTabelService,
          useValue: fromPartial<ReferentieTabelService>({
            listReferentieTabellen: () => of([]),
            listAfzenders: () => of(["other@example.com"]),
            listBrpSearchValues: () => createQueryOptions([]),
            listBrpViewValues: () => createQueryOptions([]),
            listBrpProcessingValues: () => createQueryOptions([]),
          }),
        },
        {
          provide: IdentityService,
          useValue: fromPartial<IdentityService>({
            listBehandelaarGroupsForZaaktype: () =>
              of([{ id: "test-group-id", naam: "test-group" }]),
            listBehandelaarGroupsForZaaktypeQuery: () => ({
              ...createQueryOptions([
                { id: "test-group-id", naam: "test-group" },
              ]),
              queryKey: ["behandelaarGroups"],
            }),
            listUsersInGroupQuery: () => ({
              ...createQueryOptions([
                { id: "test-user-id", naam: "test-user" },
              ]),
              queryKey: ["usersInGroup"],
            }),
          }),
        },
        {
          provide: MailtemplateBeheerService,
          useValue: fromPartial<MailtemplateBeheerService>({
            listKoppelbareMailtemplates: () => createQueryOptions([]) as never,
          }),
        },
        {
          provide: ConfiguratieService,
          useValue: fromPartial<ConfiguratieService>({
            readBrpDoelbindingSetupEnabled: () => of(true),
          }),
        },
        {
          provide: SmartDocumentsService,
          useValue: fromPartial<SmartDocumentsService>({
            getAllSmartDocumentsTemplateGroups: () => of([]),
            getTemplatesMappingQuery: () => createQueryOptions([]) as never,
            addParentIdsToTemplates: () => [],
            addTemplateMappings: () => [],
            flattenGroups: () => [],
            getTemplateMappings: () => [],
            storeTemplatesMapping: () => createMutationOptions(null) as never,
          }),
        },
        {
          provide: InformatieObjectenService,
          useValue: fromPartial<InformatieObjectenService>({
            listInformatieobjecttypes: () => of([]),
          }),
        },
      ],
    });

    await fixture.whenStable();
    fixture.detectChanges();

    return { updateZaakafhandelparameters };
  }

  async function goToStep(label: string) {
    await user.click(screen.getByRole("tab", { name: new RegExp(label) }));
    return screen.getByRole("tabpanel", { name: new RegExp(label) });
  }

  describe("SmartDocuments", () => {
    it("hides the SmartDocuments form when SmartDocuments is not enabled globally", async () => {
      await setup();
      const koppelingen = await goToStep("gegevens.koppelingen");

      expect(
        within(koppelingen).queryByText("title.smartdocuments.form"),
      ).not.toBeInTheDocument();
    });

    it("shows the SmartDocuments form as disabled for a zaaktype that has it switched off", async () => {
      await setup(
        createParameters({
          smartDocuments: {
            isEnabledGlobally: true,
            isEnabledForZaaktype: false,
          },
        }),
      );
      const koppelingen = await goToStep("gegevens.koppelingen");

      expect(
        within(koppelingen).getByText("title.smartdocuments.form"),
      ).toBeVisible();
      expect(
        within(koppelingen).getByText("msg.smartdocuments.form.disabled"),
      ).toBeVisible();
    });

    it("keeps SmartDocuments switched on for the zaaktype when saving without opening the koppelingen step", async () => {
      const { updateZaakafhandelparameters } = await setup(
        createParameters({
          smartDocuments: {
            isEnabledGlobally: true,
            isEnabledForZaaktype: true,
          },
        }),
      );
      const mail = await goToStep("gegevens.mail");

      await user.click(
        within(mail).getByRole("button", { name: "actie.opslaan" }),
      );
      await sleep();

      expect(updateZaakafhandelparameters.mutationFn.mock.calls[0][0]).toEqual(
        expect.objectContaining({
          smartDocuments: {
            isEnabledGlobally: true,
            isEnabledForZaaktype: true,
          },
        }),
      );
    });

    it("saves SmartDocuments switched off for the zaaktype once it is switched off in the koppelingen step", async () => {
      const { updateZaakafhandelparameters } = await setup(
        createParameters({
          smartDocuments: {
            isEnabledGlobally: true,
            isEnabledForZaaktype: true,
          },
        }),
      );
      const koppelingen = await goToStep("gegevens.koppelingen");

      await user.click(
        within(koppelingen).getByRole("switch", {
          name: "title.smartdocuments.form",
        }),
      );
      await user.click(
        within(koppelingen).getByRole("button", { name: "actie.opslaan" }),
      );
      await sleep();

      expect(updateZaakafhandelparameters.mutationFn.mock.calls[0][0]).toEqual(
        expect.objectContaining({
          smartDocuments: {
            isEnabledGlobally: true,
            isEnabledForZaaktype: false,
          },
        }),
      );
    });
  });

  describe("Automatische ontvangstbevestiging", () => {
    it("starts switched off without asking for a template or a sender", async () => {
      await setup();
      const mail = await goToStep("gegevens.mail");

      expect(
        within(mail).getByRole("switch", { name: /ontvangstbevestiging/i }),
      ).not.toBeChecked();
      expect(
        within(mail).queryByRole("combobox", { name: /mail.antwoord/i }),
      ).not.toBeInTheDocument();
    });

    it("blocks saving until a template and a sender are chosen", async () => {
      await setup();
      const mail = await goToStep("gegevens.mail");

      await user.click(
        within(mail).getByRole("switch", { name: /ontvangstbevestiging/i }),
      );

      expect(
        within(mail).getByRole("combobox", { name: /mail.antwoord/i }),
      ).toBeVisible();
      expect(
        within(mail).getByRole("button", { name: "actie.opslaan" }),
      ).toBeDisabled();
    });

    it("allows saving again when switched back off", async () => {
      await setup();
      const mail = await goToStep("gegevens.mail");
      const ontvangstbevestiging = within(mail).getByRole("switch", {
        name: /ontvangstbevestiging/i,
      });

      await user.click(ontvangstbevestiging);
      await user.click(ontvangstbevestiging);

      expect(
        within(mail).getByRole("button", { name: "actie.opslaan" }),
      ).toBeEnabled();
    });
  });
});
