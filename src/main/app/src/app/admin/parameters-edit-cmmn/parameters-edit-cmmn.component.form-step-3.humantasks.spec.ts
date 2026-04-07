/*
 * SPDX-FileCopyrightText: 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

import { provideHttpClient } from "@angular/common/http";
import { provideHttpClientTesting } from "@angular/common/http/testing";
import { ComponentFixture, TestBed } from "@angular/core/testing";
import { MatSelectChange } from "@angular/material/select";
import { NoopAnimationsModule } from "@angular/platform-browser/animations";
import { ActivatedRoute, RouterModule } from "@angular/router";
import { TranslateModule } from "@ngx-translate/core";
import { of } from "rxjs";
import { fromPartial } from "src/test-helpers";
import { ConfiguratieService } from "../../configuratie/configuratie.service";
import { UtilService } from "../../core/service/util.service";
import { IdentityService } from "../../identity/identity.service";
import { GeneratedType } from "../../shared/utils/generated-types";
import { MailtemplateBeheerService } from "../mailtemplate-beheer.service";
import { ReferentieTabelService } from "../referentie-tabel.service";
import { ZaakafhandelParametersService } from "../zaakafhandel-parameters.service";
import { ParametersEditCmmnComponent } from "./parameters-edit-cmmn.component";

describe("Human tasks form step", () => {
  let fixture: ComponentFixture<ParametersEditCmmnComponent>;
  let zaakafhandelParametersService: ZaakafhandelParametersService;
  let referentieTabelService: ReferentieTabelService;
  let identityService: IdentityService;
  let mailtemplateBeheerService: MailtemplateBeheerService;
  let utilService: UtilService;
  let activatedRouteMock: Pick<ActivatedRoute, "data">;

  const humanTaskParameters = [
    fromPartial<GeneratedType<"RESTHumanTaskParameters">>({
      planItemDefinition: { id: "task-1", naam: "Taak 1", type: "HUMAN_TASK" },
      actief: true,
      doorlooptijd: 5,
      formulierDefinitieId: undefined,
      referentieTabellen: [],
    }),
  ];

  const zaakafhandelParameters = fromPartial<
    GeneratedType<"RestZaakafhandelParameters">
  >({
    defaultGroepId: "test-group-id",
    defaultBehandelaarId: "test-user-id",
    zaaktype: { uuid: "test-uuid" },
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
    humanTaskParameters,
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

  beforeEach(async () => {
    activatedRouteMock = {
      data: of({
        parameters: {
          zaakafhandelParameters,
          isSavedZaakafhandelParameters: true,
          featureFlagPabcIntegration: true,
        },
      }),
    };

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
        { provide: ActivatedRoute, useValue: activatedRouteMock },
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
        { mail: "reply1@example.com", speciaal: false },
        { mail: "reply2@example.com", speciaal: false },
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
      .spyOn(identityService, "listBehandelaarGroupsForZaaktype")
      .mockReturnValue(
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
    fixture.detectChanges();
    await fixture.whenStable();
  });

  it("should create a form group for each humanTaskParameter", () => {
    const component = fixture.componentInstance;
    const taskId = humanTaskParameters[0].planItemDefinition?.id ?? "";
    const taskFormGroup = component.humanTasksFormGroup.get(taskId);
    expect(taskFormGroup).not.toBeNull();
  });

  it("should have actief control with initial value true", () => {
    const component = fixture.componentInstance;
    const actief = component["getHumanTaskControl"](
      humanTaskParameters[0],
      "actief",
    );
    expect(actief.value).toBe(true);
  });

  it("should have doorlooptijd control with initial value 5", () => {
    const component = fixture.componentInstance;
    const doorlooptijd = component["getHumanTaskControl"](
      humanTaskParameters[0],
      "doorlooptijd",
    );
    expect(doorlooptijd.value).toBe(5);
  });

  it("should validate doorlooptijd with min(0) - set to -1, expect invalid", () => {
    const component = fixture.componentInstance;
    const doorlooptijd = component["getHumanTaskControl"](
      humanTaskParameters[0],
      "doorlooptijd",
    );
    doorlooptijd.setValue(-1);
    doorlooptijd.updateValueAndValidity();
    expect(doorlooptijd.hasError("min")).toBe(true);
  });

  it("isHumanTaskParameterValid returns false when formulierDefinitie is required but missing", () => {
    const component = fixture.componentInstance;
    // formulierDefinitieId is undefined → formulierDefinitie control has Validators.required and no value
    const isValid = component["isHumanTaskParameterValid"](
      humanTaskParameters[0],
    );
    expect(isValid).toBe(false);
  });

  it("getHumanTaskControl returns the correct control", () => {
    const component = fixture.componentInstance;
    const actief = component["getHumanTaskControl"](
      humanTaskParameters[0],
      "actief",
    );
    expect(actief).not.toBeNull();
    expect(actief.value).toBe(true);
  });

  it("isHumanTaskParameterValid returns true when formulierDefinitie is set", () => {
    const component = fixture.componentInstance;
    component["getHumanTaskControl"](
      humanTaskParameters[0],
      "formulierDefinitie",
    ).setValue("DEFAULT_TAAKFORMULIER");
    expect(component["isHumanTaskParameterValid"](humanTaskParameters[0])).toBe(
      true,
    );
  });

  it("formulierDefinitieChanged updates formulierDefinitieId and rebuilds the task form group", () => {
    const component = fixture.componentInstance;
    const task = {
      ...humanTaskParameters[0],
      formulierDefinitieId: undefined as string | undefined,
    };

    component["formulierDefinitieChanged"](
      { value: "DEFAULT_TAAKFORMULIER" } as MatSelectChange,
      task,
    );

    expect(task.formulierDefinitieId).toBe("DEFAULT_TAAKFORMULIER");
    expect(
      component.humanTasksFormGroup.get(task.planItemDefinition?.id ?? ""),
    ).not.toBeNull();
  });

  it("opslaan reads doorlooptijd and actief from form controls into humanTaskParameters", () => {
    const component = fixture.componentInstance;
    jest
      .spyOn(zaakafhandelParametersService, "updateZaakafhandelparameters")
      .mockReturnValue(of(component.parameters));

    component["getHumanTaskControl"](
      humanTaskParameters[0],
      "formulierDefinitie",
    ).setValue("DEFAULT_TAAKFORMULIER");
    component["getHumanTaskControl"](
      humanTaskParameters[0],
      "doorlooptijd",
    ).setValue(10);
    component["getHumanTaskControl"](humanTaskParameters[0], "actief").setValue(
      false,
    );

    component.algemeenFormGroup.controls["caseDefinition"].setValue(
      fromPartial<GeneratedType<"RESTCaseDefinition">>({ key: "case-1" }),
      { emitEvent: false },
    );
    component.algemeenFormGroup.controls["defaultGroep"].setValue(
      { id: "test-group-id", naam: "test-group" },
      { emitEvent: false },
    );
    component["updateZaakAfzenders"]("test@example.com");

    component["opslaan"]();

    expect(component["humanTaskParameters"][0].doorlooptijd).toBe(10);
    expect(component["humanTaskParameters"][0].actief).toBe(false);
  });
});
