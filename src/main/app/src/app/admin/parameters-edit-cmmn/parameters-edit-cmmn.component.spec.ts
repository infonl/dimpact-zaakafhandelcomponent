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
import { MatStepperHarness } from "@angular/material/stepper/testing";
import { NoopAnimationsModule } from "@angular/platform-browser/animations";
import { ActivatedRoute, RouterModule } from "@angular/router";
import { TranslateModule } from "@ngx-translate/core";
import { fromPartial } from "@total-typescript/shoehorn";
import { of } from "rxjs";
import { ConfiguratieService } from "../../configuratie/configuratie.service";
import { UtilService } from "../../core/service/util.service";
import { IdentityService } from "../../identity/identity.service";
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

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [
        ParametersEditCmmnComponent,
        TranslateModule.forRoot(),
        RouterModule,
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
      .spyOn(configuratieService, "readBrpDoelbindingSetupEnabled")
      .mockReturnValue(of(false));

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

  describe("Process definition form", () => {
    it("should disable cmmnBpmnFormGroup when parameters are already saved", () => {
      const component = fixture.componentInstance;
      expect(component["cmmnBpmnFormGroup"].disabled).toBe(true);
    });

    it("should set featureFlagPabcIntegration from route data", () => {
      const component = fixture.componentInstance;
      expect(component["featureFlagPabcIntegration"]).toBe(true);
    });
  });

  describe("Default mail values", () => {
    it("should default intakeMail to BESCHIKBAAR_UIT when not provided", () => {
      const component = fixture.componentInstance;
      expect(component.parameters.intakeMail).toBe("BESCHIKBAAR_UIT");
    });

    it("should default afrondenMail to BESCHIKBAAR_UIT when not provided", () => {
      const component = fixture.componentInstance;
      expect(component.parameters.afrondenMail).toBe("BESCHIKBAAR_UIT");
    });
  });

  describe("Algemeen form step", () => {
    it("should have valid Algemeen form group after patching with valid values", async () => {
      const component = fixture.componentInstance;

      expect(component.algemeenFormGroup.valid).toBe(false);

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

      expect(component.algemeenFormGroup.valid).toBe(true);
      expect(component.algemeenFormGroup.value.caseDefinition?.key).toBe(
        "case-1",
      );
      expect(component.algemeenFormGroup.value.defaultGroep?.id).toBe(
        "test-group-id",
      );
      expect(component.algemeenFormGroup.value.defaultBehandelaar?.id).toBe(
        "test-user-id",
      );
    });
  });

  describe("Case handler", () => {
    it("should load medewerkers for the default group on initialization", () => {
      const component = fixture.componentInstance;
      expect(component["medewerkers"]).toEqual([
        { id: "test-user-id", naam: "test-user" },
        { id: "test-user-id-2", naam: "test-user-2" },
      ]);
    });

    it("should set the default behandelaar matching defaultBehandelaarId", () => {
      const component = fixture.componentInstance;
      expect(
        component.algemeenFormGroup.controls.defaultBehandelaar.value,
      ).toEqual({ id: "test-user-id", naam: "test-user" });
    });

    it("should reload medewerkers when the selected group changes", async () => {
      const component = fixture.componentInstance;

      component.algemeenFormGroup.controls.defaultGroep.setValue({
        id: "test-group-id-2",
        naam: "test-group-2",
      });
      await fixture.whenStable();
      fixture.detectChanges();

      expect(component["medewerkers"]).toEqual([]);
    });

    it("should clear defaultBehandelaar when new group has no matching user", async () => {
      const component = fixture.componentInstance;

      component.algemeenFormGroup.controls.defaultGroep.setValue({
        id: "test-group-id-2",
        naam: "test-group-2",
      });
      await fixture.whenStable();
      fixture.detectChanges();

      expect(
        component.algemeenFormGroup.controls.defaultBehandelaar.value,
      ).toBeNull();
    });
  });

  describe("Mailgegevens form step", () => {
    it("should be invalid when no default afzender is set", () => {
      const component = fixture.componentInstance;
      expect(component.mailFormGroup.valid).toBe(false);
    });

    it("should become valid after selecting a default afzender", () => {
      const component = fixture.componentInstance;

      component["updateZaakAfzenders"]("test@example.com");
      fixture.detectChanges();

      expect(component.mailFormGroup.valid).toBe(true);
    });

    it("should mark the selected afzender as defaultMail", () => {
      const component = fixture.componentInstance;

      component["updateZaakAfzenders"]("test@example.com");

      const defaultAfzender = component.parameters.zaakAfzenders?.find(
        (afzender) => afzender.defaultMail === true,
      );
      expect(defaultAfzender?.mail).toBe("test@example.com");
    });

    it("should add a new afzender to the data source", () => {
      const component = fixture.componentInstance;
      const initialCount = component.zaakAfzendersDataSource.data.length;

      component["addZaakAfzender"]("new@example.com");

      expect(component.zaakAfzendersDataSource.data.length).toBe(
        initialCount + 1,
      );
      expect(
        component.zaakAfzendersDataSource.data.some(
          (a) => a.mail === "new@example.com",
        ),
      ).toBe(true);
    });

    it("should remove an afzender from the data source", () => {
      const component = fixture.componentInstance;
      const initialCount = component.zaakAfzendersDataSource.data.length;

      component["removeZaakAfzender"]("test@example.com");

      expect(component.zaakAfzendersDataSource.data.length).toBe(
        initialCount - 1,
      );
      expect(
        component.zaakAfzendersDataSource.data.some(
          (a) => a.mail === "test@example.com",
        ),
      ).toBe(false);
    });
  });

  describe("Betrokkene koppelingen", () => {
    it("should initialize form with values from parameters", () => {
      const component = fixture.componentInstance;
      expect(component["betrokkeneKoppelingen"].value).toEqual({
        brpKoppelen: false,
        kvkKoppelen: false,
      });
    });

    it("should add required validators to brp fields when brpKoppelen is enabled", () => {
      const component = fixture.componentInstance;

      component["betrokkeneKoppelingen"].controls.brpKoppelen.setValue(true);
      component.brpProtocoleringFormGroup.controls.raadpleegWaarde.updateValueAndValidity();
      component.brpProtocoleringFormGroup.controls.zoekWaarde.updateValueAndValidity();
      component.brpProtocoleringFormGroup.controls.verwerkingregisterWaarde.updateValueAndValidity();

      expect(
        component.brpProtocoleringFormGroup.controls.raadpleegWaarde.hasError(
          "required",
        ),
      ).toBe(true);
      expect(
        component.brpProtocoleringFormGroup.controls.zoekWaarde.hasError(
          "required",
        ),
      ).toBe(true);
      expect(
        component.brpProtocoleringFormGroup.controls.verwerkingregisterWaarde.hasError(
          "required",
        ),
      ).toBe(true);
    });

    it("should clear required validators from brp fields when brpKoppelen is disabled", () => {
      const component = fixture.componentInstance;

      component["betrokkeneKoppelingen"].controls.brpKoppelen.setValue(true);
      component["betrokkeneKoppelingen"].controls.brpKoppelen.setValue(false);
      component.brpProtocoleringFormGroup.controls.raadpleegWaarde.updateValueAndValidity();

      expect(
        component.brpProtocoleringFormGroup.controls.raadpleegWaarde.hasError(
          "required",
        ),
      ).toBe(false);
    });

    it("should reset brp fields when brpKoppelen is disabled", () => {
      const component = fixture.componentInstance;

      component.brpProtocoleringFormGroup.controls.raadpleegWaarde.setValue(
        "some-value",
      );
      component["betrokkeneKoppelingen"].controls.brpKoppelen.setValue(true);
      component["betrokkeneKoppelingen"].controls.brpKoppelen.setValue(false);

      expect(
        component.brpProtocoleringFormGroup.controls.raadpleegWaarde.value,
      ).toBeNull();
    });
  });
});
