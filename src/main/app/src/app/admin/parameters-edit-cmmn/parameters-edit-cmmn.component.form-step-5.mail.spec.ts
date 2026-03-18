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

describe("Mailgegevens form step", () => {
  let fixture: ComponentFixture<ParametersEditCmmnComponent>;
  let zaakafhandelParametersService: ZaakafhandelParametersService;
  let referentieTabelService: ReferentieTabelService;
  let identityService: IdentityService;
  let mailtemplateBeheerService: MailtemplateBeheerService;
  let utilService: UtilService;
  let activatedRouteMock: Pick<ActivatedRoute, "data">;

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
