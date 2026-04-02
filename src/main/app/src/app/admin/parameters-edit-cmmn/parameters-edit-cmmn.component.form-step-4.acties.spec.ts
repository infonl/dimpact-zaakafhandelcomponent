/*
 * SPDX-FileCopyrightText: 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

import { provideHttpClient } from "@angular/common/http";
import { provideHttpClientTesting } from "@angular/common/http/testing";
import { ComponentFixture, TestBed } from "@angular/core/testing";
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

describe("Acties form step", () => {
  let fixture: ComponentFixture<ParametersEditCmmnComponent>;
  let zaakafhandelParametersService: ZaakafhandelParametersService;
  let referentieTabelService: ReferentieTabelService;
  let identityService: IdentityService;
  let mailtemplateBeheerService: MailtemplateBeheerService;
  let utilService: UtilService;
  let activatedRouteMock: Pick<ActivatedRoute, "data">;

  const userEventListenerParameters = [
    fromPartial<GeneratedType<"RESTUserEventListenerParameter">>({
      id: "event-1",
      naam: "Event 1",
      toelichting: "initial toelichting",
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
    humanTaskParameters: [],
    mailtemplateKoppelingen: [],
    zaakbeeindigParameters: [],
    smartDocuments: { enabledGlobally: false, enabledForZaaktype: false },
    userEventListenerParameters,
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
  });

  it("should create a toelichting control for each userEventListenerParameter", () => {
    const component = fixture.componentInstance;
    const eventId = userEventListenerParameters[0].id ?? "";
    const eventFormGroup = component.userEventListenersFormGroup.get(eventId);
    expect(eventFormGroup).not.toBeNull();
    const toelichtingControl = eventFormGroup?.get("toelichting");
    expect(toelichtingControl).not.toBeNull();
  });

  it("should initialize toelichting with value from parameters", () => {
    const component = fixture.componentInstance;
    const eventId = userEventListenerParameters[0].id ?? "";
    const toelichtingControl = component.userEventListenersFormGroup
      .get(eventId)
      ?.get("toelichting");
    expect(toelichtingControl?.value).toBe("initial toelichting");
  });

  it("should allow updating toelichting value", () => {
    const component = fixture.componentInstance;
    const eventId = userEventListenerParameters[0].id ?? "";
    const toelichtingControl = component.userEventListenersFormGroup
      .get(eventId)
      ?.get("toelichting");
    toelichtingControl?.setValue("updated toelichting");
    expect(toelichtingControl?.value).toBe("updated toelichting");
  });
});
