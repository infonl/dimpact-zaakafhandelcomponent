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

describe(ParametersEditCmmnComponent.name, () => {
  const caseDefinition = fromPartial<GeneratedType<"RESTCaseDefinition">>({
    key: "fakeCaseDefinitionKey",
    naam: "fakeCaseDefinitionNaam",
  });

  const zaakafhandelParameters = fromPartial<
    GeneratedType<"RestZaaktypeConfiguration">
  >({
    caseDefinition,
    defaultGroepId: "fakeGroupId",
    defaultBehandelaarId: "fakeUserId",
    zaaktype: { uuid: "fakeZaaktypeUuid" },
    zaakAfzenders: [],
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

  async function setup(inputs: { selectedIndexStart?: number } = {}) {
    const rendered = await render(ParametersEditCmmnComponent, {
      inputs,
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
            listReplyTos: () => of([]),
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
            listAfzenders: () => of([]),
            listBrpSearchValues: () => of([]),
            listBrpViewValues: () => of([]),
            listBrpProcessingValues: () => of([]),
          }),
        },
        {
          provide: IdentityService,
          useValue: fromPartial<IdentityService>({
            listGroups: () =>
              of([{ id: "fakeGroupId", naam: "fakeGroupNaam" }]),
            listUsersInGroup: () =>
              of([{ id: "fakeUserId", naam: "fakeUserNaam" }]),
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

    await rendered.fixture.whenStable();
    rendered.fixture.detectChanges();

    return rendered;
  }

  function selectedStep() {
    return screen.getByRole("tab", { selected: true });
  }

  describe("selectedIndexStart", () => {
    it("starts at the first step by default", async () => {
      await setup();

      expect(selectedStep()).toHaveAccessibleName(
        /gegevens.proces-model-methode.CMMN/,
      );
    });

    it("starts at the step it points to", async () => {
      await setup({ selectedIndexStart: 1 });

      expect(selectedStep()).toHaveAccessibleName(/gegevens.algemeen/);
    });

    it("moves back to the first step when it changes to 0", async () => {
      const { fixture } = await setup({ selectedIndexStart: 1 });

      fixture.componentRef.setInput("selectedIndexStart", 0);
      fixture.detectChanges();

      expect(selectedStep()).toHaveAccessibleName(
        /gegevens.proces-model-methode.CMMN/,
      );
    });
  });
});
