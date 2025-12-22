/*
 * SPDX-FileCopyrightText: 2025 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 *
 */

import { HarnessLoader } from "@angular/cdk/testing";
import { TestbedHarnessEnvironment } from "@angular/cdk/testing/testbed";
import { provideHttpClient } from "@angular/common/http";
import { provideHttpClientTesting } from "@angular/common/http/testing";
import { ComponentFixture, TestBed } from "@angular/core/testing";
import { MatRadioButtonHarness } from "@angular/material/radio/testing";
import { MatSelectHarness } from "@angular/material/select/testing";
import { MatStepperHarness } from "@angular/material/stepper/testing";
import { NoopAnimationsModule } from "@angular/platform-browser/animations";
import { ActivatedRoute, RouterModule } from "@angular/router";
import { TranslateModule } from "@ngx-translate/core";
import { fromPartial } from "@total-typescript/shoehorn";
import { of } from "rxjs";
import { ConfiguratieService } from "../../configuratie/configuratie.service";
import { UtilService } from "../../core/service/util.service";
import { IdentityService } from "../../identity/identity.service";
import { MaterialFormBuilderModule } from "../../shared/material-form-builder/material-form-builder.module";
import { MaterialModule } from "../../shared/material/material.module";
import { PipesModule } from "../../shared/pipes/pipes.module";
import { SideNavComponent } from "../../shared/side-nav/side-nav.component";
import { StaticTextComponent } from "../../shared/static-text/static-text.component";
import { GeneratedType } from "../../shared/utils/generated-types";
import { MailtemplateBeheerService } from "../mailtemplate-beheer.service";
import { ReferentieTabelService } from "../referentie-tabel.service";
import { ZaakafhandelParametersService } from "../zaakafhandel-parameters.service";
import { ParametersEditCmmnComponent } from "./parameters-edit-cmmn.component";

describe(ParametersEditCmmnComponent.name, () => {
  let fixture: ComponentFixture<ParametersEditCmmnComponent>;
  let zaakafhandelParametersService: ZaakafhandelParametersService;
  let referentieTabelService: ReferentieTabelService;
  let identityService: IdentityService;
  let mailtemplateBeheerService: MailtemplateBeheerService;
  let loader: HarnessLoader;
  let utilService: UtilService;

  const zaakafhandelParameters = fromPartial<
    GeneratedType<"RestZaakafhandelParameters">
  >({
    defaultGroepId: "test-group-id",
    defaultBehandelaarId: "test-user-id",
    zaaktype: {
      uuid: "test-uuid",
    },
    zaakAfzenders: [
      {
        speciaal: false,
        defaultMail: false,
        mail: "test@example.com",
        replyTo: undefined,
      },
      {
        speciaal: false,
        defaultMail: false,
        mail: "test2@example.com",
        replyTo: undefined,
      },
    ],
    humanTaskParameters: [],
    mailtemplateKoppelingen: [],
    zaakbeeindigParameters: [],
    smartDocuments: {
      enabledGlobally: false,
      enabledForZaaktype: false,
    },
    userEventListenerParameters: [],
    betrokkeneKoppelingen: {
      brpKoppelen: false,
      kvkKoppelen: false,
    },
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

  async function selectStepperStep(stepIndex: number): Promise<void> {
    await (
      await (await loader.getHarness(MatStepperHarness)).getSteps()
    )[stepIndex].select();
  }

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      declarations: [
        ParametersEditCmmnComponent,
        SideNavComponent,
        StaticTextComponent,
      ],
      imports: [
        TranslateModule.forRoot(),
        MaterialModule,
        RouterModule,
        PipesModule,
        MaterialFormBuilderModule,
        NoopAnimationsModule,
      ],
      providers: [
        provideHttpClient(),
        provideHttpClientTesting(),
        {
          provide: ActivatedRoute,
          useValue: {
            data: of({
              parameters: {
                zaakafhandelParameters,
                isSavedZaakafhandelParameters: true,
                featureFlagPabcIntegration: true,
              },
            }),
          },
        },
      ],
    }).compileComponents();

    zaakafhandelParametersService = TestBed.inject(
      ZaakafhandelParametersService,
    );
    jest
      .spyOn(zaakafhandelParametersService, "listCaseDefinitions")
      .mockReturnValue(
        of([
          fromPartial<GeneratedType<"RESTCaseDefinition">>({
            key: "case-1",
            naam: "Case Definition 1",
          }),
        ]),
      );
    jest
      .spyOn(zaakafhandelParametersService, "listFormulierDefinities")
      .mockReturnValue(of([]));
    jest.spyOn(zaakafhandelParametersService, "listReplyTos").mockReturnValue(
      of([
        {
          mail: "reply1@example.com",
          speciaal: false,
        },
        {
          mail: "reply2@example.com",
          speciaal: false,
        },
      ]),
    );
    jest
      .spyOn(zaakafhandelParametersService, "listZaakbeeindigRedenen")
      .mockReturnValue(of([]));
    jest
      .spyOn(zaakafhandelParametersService, "listResultaattypes")
      .mockReturnValue(of([]));

    referentieTabelService = TestBed.inject(ReferentieTabelService);
    jest
      .spyOn(referentieTabelService, "listReferentieTabellen")
      .mockReturnValue(of([]));
    jest.spyOn(referentieTabelService, "listDomeinen").mockReturnValue(of([]));
    jest
      .spyOn(referentieTabelService, "listAfzenders")
      .mockReturnValue(of(["test@example.com", "other@example.com"]));
    jest
      .spyOn(referentieTabelService, "listBrpViewValues")
      .mockReturnValue(of([]));
    jest
      .spyOn(referentieTabelService, "listBrpSearchValues")
      .mockReturnValue(of([]));
    jest
      .spyOn(referentieTabelService, "listBrpProcessingValues")
      .mockReturnValue(of([]));

    identityService = TestBed.inject(IdentityService);
    jest.spyOn(identityService, "listGroups").mockReturnValue(
      of([
        { id: "test-group-id", naam: "test-group" },
        { id: "test-group-id-2", naam: "test-group-2" },
      ]),
    );
    jest
      .spyOn(identityService, "listUsersInGroup")
      .mockReturnValueOnce(
        of([
          { id: "test-user-id", naam: "test-user" },
          { id: "test-user-id-2", naam: "test-user-2" },
        ]),
      )
      .mockReturnValue(of([]));

    utilService = TestBed.inject(UtilService);
    jest.spyOn(utilService, "compare").mockReturnValue(true);

    mailtemplateBeheerService = TestBed.inject(MailtemplateBeheerService);
    jest
      .spyOn(mailtemplateBeheerService, "listKoppelbareMailtemplates")
      .mockReturnValue(of([]));

    const configuratieService = TestBed.inject(ConfiguratieService);
    jest
      .spyOn(configuratieService, "readBrpProtocollering")
      .mockReturnValue(of(""));

    fixture = TestBed.createComponent(ParametersEditCmmnComponent);
    await fixture.whenStable();
    fixture.detectChanges();

    loader = TestbedHarnessEnvironment.loader(fixture);
  });

  describe("Stepper", () => {
    it("should render all stepper steps", async () => {
      const stepper = await loader.getHarness(MatStepperHarness);
      const steps = await stepper.getSteps();
      expect(steps.length).toBe(7);
    });
  });

  describe("Algemeen form step", () => {
    it("should have valid Algemeen form group after selecting case and group", async () => {
      const component = fixture.componentInstance;
      await selectStepperStep(2);

      const selects = await loader.getAllHarnesses(MatSelectHarness);
      expect(selects.length).toEqual(3);

      const select = selects[0];
      await select.open();
      const options = await select.getOptions();
      expect(options.length).toBe(1);

      const optionText = await options[0].getText();
      await options[0].click();
      const selectedValue = await select.getValueText();
      expect(selectedValue).toBe(optionText);

      expect(component.algemeenFormGroup.valid).toBe(true);
    });
  });

  describe("Mailgegevens form step", () => {
    beforeEach(async () => {
      const component = fixture.componentInstance;
      component.algemeenFormGroup.patchValue({
        caseDefinition: fromPartial<GeneratedType<"RESTCaseDefinition">>({
          key: "case-1",
          naam: "Case Definition 1",
        }),
        domein: "test-domein",
        defaultGroep: { id: "test-group-id", naam: "test-group" },
        defaultBehandelaar: { id: "test-user-id", naam: "test-user" },
        einddatumGeplandWaarschuwing: 5,
        uiterlijkeEinddatumAfdoeningWaarschuwing: 10,
        productaanvraagtype: null,
      });
      fixture.detectChanges();
    });

    it("should have valid mailFormGroup after selecting afzender", async () => {
      const component = fixture.componentInstance;
      await selectStepperStep(3);

      const radioButtons = await loader.getAllHarnesses(MatRadioButtonHarness);
      await radioButtons[0].check();

      const selects = await loader.getAllHarnesses(MatSelectHarness);
      const select = selects[2];
      await select.open();
      const options = await select.getOptions();
      await options[0].click();
    });
  });
});
