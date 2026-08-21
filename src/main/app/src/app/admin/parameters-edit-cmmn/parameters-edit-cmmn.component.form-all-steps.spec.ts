/*
 * SPDX-FileCopyrightText: 2025, 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 *
 */

import { provideHttpClient } from "@angular/common/http";
import { provideHttpClientTesting } from "@angular/common/http/testing";
import { NoopAnimationsModule } from "@angular/platform-browser/animations";
import { ActivatedRoute, RouterModule } from "@angular/router";
import { TranslateModule } from "@ngx-translate/core";
import { provideQueryClient } from "@tanstack/angular-query-experimental";
import { render, screen } from "@testing-library/angular";
import { of } from "rxjs";
import { createMutationOptions, fromPartial } from "src/test-helpers";
import { testQueryClient } from "../../../../setupJest";
import { ConfiguratieService } from "../../configuratie/configuratie.service";
import { IdentityService } from "../../identity/identity.service";
import { GeneratedType } from "../../shared/utils/generated-types";
import { MailtemplateBeheerService } from "../mailtemplate-beheer.service";
import { ReferentieTabelService } from "../referentie-tabel.service";
import { ZaakafhandelParametersService } from "../zaakafhandel-parameters.service";
import { ParametersEditCmmnComponent } from "./parameters-edit-cmmn.component";

// rendering this seven step form once per test needs more room than the default timeout
describe(ParametersEditCmmnComponent.name, () => {
  const stepLabels = [
    "gegevens.proces-model-methode.CMMN",
    "gegevens.algemeen",
    "gegevens.humantasks",
    "gegevens.acties",
    "gegevens.mail",
    "gegevens.beeindiging",
    "gegevens.koppelingen",
  ];

  const caseDefinition = fromPartial<GeneratedType<"RESTCaseDefinition">>({
    key: "case-1",
    naam: "Case Definition 1",
  });

  const zaakafhandelParameters = fromPartial<
    GeneratedType<"RestZaaktypeConfiguration">
  >({
    caseDefinition,
    defaultGroepId: "test-group-id",
    defaultBehandelaarId: "test-user-id",
    zaaktype: { uuid: "test-uuid" },
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
  });

  async function setup() {
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
                zaakafhandelParameters,
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
              createMutationOptions(zaakafhandelParameters),
          }),
        },
        {
          provide: ReferentieTabelService,
          useValue: fromPartial<ReferentieTabelService>({
            listReferentieTabellen: () => of([]),
            listAfzenders: () => of(["other@example.com"]),
            listBrpSearchValues: () => of([]),
            listBrpViewValues: () => of([]),
            listBrpProcessingValues: () => of([]),
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
            readBrpDoelbindingSetupEnabled: () => of(false),
          }),
        },
      ],
    });

    await fixture.whenStable();
    fixture.detectChanges();
  }

  it("guides the configuration through all of its steps in order", async () => {
    await setup();

    const steps = screen.getAllByRole("tab");

    expect(steps).toHaveLength(stepLabels.length);
    stepLabels.forEach((label, index) => {
      expect(steps[index]).toHaveAccessibleName(new RegExp(label));
    });
  });
});
