/*
 * SPDX-FileCopyrightText: 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

import { provideHttpClient } from "@angular/common/http";
import { provideHttpClientTesting } from "@angular/common/http/testing";
import { NoopAnimationsModule } from "@angular/platform-browser/animations";
import { ActivatedRoute, RouterModule } from "@angular/router";
import { TranslateModule } from "@ngx-translate/core";
import { provideQueryClient } from "@tanstack/angular-query-experimental";
import { render, screen, within } from "@testing-library/angular";
import userEvent from "@testing-library/user-event";
import { of } from "rxjs";
import { InformatieObjectenService } from "src/app/informatie-objecten/informatie-objecten.service";
import { createMutationOptions, fromPartial } from "src/test-helpers";
import { testQueryClient } from "../../../../setupJest";
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

  const brpSearchValues = ["zoeken-1", "zoeken-2"];
  const brpViewValues = ["raadplegen-1", "raadplegen-2"];
  const brpProcessingValues = ["verwerken-1", "verwerken-2"];

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
          speciaal: false,
          defaultMail: true,
          mail: "test@example.com",
          replyTo: undefined,
        },
      ],
      humanTaskParameters: [],
      mailtemplateKoppelingen: [],
      zaakbeeindigParameters: [],
      smartDocuments: { enabledGlobally: false, enabledForZaaktype: false },
      userEventListenerParameters: [],
      betrokkeneKoppelingen: { brpKoppelen: false, kvkKoppelen: false },
      brpDoelbindingen: {
        zoekWaarde: "",
        raadpleegWaarde: "",
        verwerkingregisterWaarde: "",
      },
      productaanvraagtype: null,
      automaticEmailConfirmation: {
        enabled: false,
        templateName: null,
        emailSender: null,
        emailReply: null,
      },
      ...overrides,
    });
  }

  async function setup(parameters = createParameters()) {
    const { fixture } = await render(ParametersEditCmmnComponent, {
      imports: [TranslateModule.forRoot(), RouterModule, NoopAnimationsModule],
      providers: [
        provideQueryClient(testQueryClient),
        provideHttpClient(),
        provideHttpClientTesting(),
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
            listZaakbeeindigRedenen: () => of([]),
            listResultaattypes: () => of([]),
            updateZaakafhandelparameters: () =>
              createMutationOptions(parameters),
          }),
        },
        {
          provide: ReferentieTabelService,
          useValue: fromPartial<ReferentieTabelService>({
            listReferentieTabellen: () => of([]),
            listAfzenders: () => of(["other@example.com"]),
            listBrpSearchValues: () => of(brpSearchValues),
            listBrpViewValues: () => of(brpViewValues),
            listBrpProcessingValues: () => of(brpProcessingValues),
          }),
        },
        {
          provide: IdentityService,
          useValue: fromPartial<IdentityService>({
            listGroups: () => of([{ id: "test-group-id", naam: "test-group" }]),
            listUsersInGroup: () =>
              of([{ id: "test-user-id", naam: "test-user" }]),
          }),
        },
        {
          provide: MailtemplateBeheerService,
          useValue: fromPartial<MailtemplateBeheerService>({
            listKoppelbareMailtemplates: () => of([]),
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
            getTemplatesMapping: () => of([]),
            addParentIdsToTemplates: () => [],
            addTemplateMappings: () => [],
            flattenGroups: () => [],
            getTemplateMappings: () => [],
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

    return fixture;
  }

  async function goToStep(label: string) {
    await user.click(screen.getByRole("tab", { name: new RegExp(label) }));
    return screen.getByRole("tabpanel", { name: new RegExp(label) });
  }

  describe("Landelijke koppelingen", () => {
    it("starts with both koppelingen switched off", async () => {
      await setup();
      const koppelingen = await goToStep("gegevens.koppelingen");

      expect(
        within(koppelingen).getByRole("switch", {
          name: "gegevens.koppelen.brp",
        }),
      ).not.toBeChecked();
      expect(
        within(koppelingen).getByRole("switch", {
          name: "gegevens.koppelen.kvk",
        }),
      ).not.toBeChecked();
    });

    it("asks for the BRP doelbinding values once BRP koppelen is switched on", async () => {
      await setup();
      const koppelingen = await goToStep("gegevens.koppelingen");

      expect(
        within(koppelingen).queryByRole("combobox", {
          name: "brpDoelbinding.zoekWaarde",
        }),
      ).not.toBeInTheDocument();

      await user.click(
        within(koppelingen).getByRole("switch", {
          name: "gegevens.koppelen.brp",
        }),
      );

      expect(
        within(koppelingen).getByRole("combobox", {
          name: "brpDoelbinding.zoekWaarde",
        }),
      ).toBeVisible();
      expect(
        within(koppelingen).getByRole("combobox", {
          name: "brpDoelbinding.raadpleegWaarde",
        }),
      ).toBeVisible();
      expect(
        within(koppelingen).getByRole("combobox", {
          name: "brpDoelbinding.verwerkingregisterWaarde",
        }),
      ).toBeVisible();
    });

    it("blocks saving until every BRP doelbinding value is chosen", async () => {
      await setup();
      const koppelingen = await goToStep("gegevens.koppelingen");
      const opslaan = within(koppelingen).getByRole("button", {
        name: "actie.opslaan",
      });

      await user.click(
        within(koppelingen).getByRole("switch", {
          name: "gegevens.koppelen.brp",
        }),
      );

      expect(opslaan).toBeDisabled();

      await chooseBrpDoelbinding(koppelingen, {
        "brpDoelbinding.zoekWaarde": brpSearchValues[0],
        "brpDoelbinding.raadpleegWaarde": brpViewValues[0],
        "brpDoelbinding.verwerkingregisterWaarde": brpProcessingValues[0],
      });

      expect(opslaan).toBeEnabled();
    });

    it("allows saving again when BRP koppelen is switched back off", async () => {
      await setup();
      const koppelingen = await goToStep("gegevens.koppelingen");
      const brpKoppelen = within(koppelingen).getByRole("switch", {
        name: "gegevens.koppelen.brp",
      });
      const opslaan = within(koppelingen).getByRole("button", {
        name: "actie.opslaan",
      });

      await user.click(brpKoppelen);
      expect(opslaan).toBeDisabled();

      await user.click(brpKoppelen);

      expect(
        within(koppelingen).queryByRole("combobox", {
          name: "brpDoelbinding.zoekWaarde",
        }),
      ).not.toBeInTheDocument();
      expect(opslaan).toBeEnabled();
    });

    it("forgets the chosen BRP doelbinding values when BRP koppelen is switched off", async () => {
      await setup();
      const koppelingen = await goToStep("gegevens.koppelingen");
      const brpKoppelen = within(koppelingen).getByRole("switch", {
        name: "gegevens.koppelen.brp",
      });

      await user.click(brpKoppelen);
      await chooseBrpDoelbinding(koppelingen, {
        "brpDoelbinding.zoekWaarde": brpSearchValues[0],
      });
      await user.click(brpKoppelen);
      await user.click(brpKoppelen);

      expect(
        within(koppelingen).getByRole("combobox", {
          name: "brpDoelbinding.zoekWaarde",
        }),
      ).not.toHaveTextContent(brpSearchValues[0]);
    });
  });

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
          smartDocuments: { enabledGlobally: true, enabledForZaaktype: false },
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

  async function chooseBrpDoelbinding(
    koppelingen: HTMLElement,
    values: Record<string, string>,
  ) {
    for (const [label, value] of Object.entries(values)) {
      await user.click(
        within(koppelingen).getByRole("combobox", { name: label }),
      );
      await user.click(screen.getByRole("option", { name: value }));
    }
  }
});
