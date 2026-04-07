/*
 * SPDX-FileCopyrightText: 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

import { provideHttpClient } from "@angular/common/http";
import { provideHttpClientTesting } from "@angular/common/http/testing";
import { ComponentFixture, TestBed } from "@angular/core/testing";
import { MatCheckboxChange } from "@angular/material/checkbox";
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

describe("Beeindiging form step", () => {
  let fixture: ComponentFixture<ParametersEditCmmnComponent>;
  let zaakafhandelParametersService: ZaakafhandelParametersService;
  let referentieTabelService: ReferentieTabelService;
  let identityService: IdentityService;
  let mailtemplateBeheerService: MailtemplateBeheerService;
  let utilService: UtilService;
  let activatedRouteMock: Pick<ActivatedRoute, "data">;

  const reden = fromPartial<GeneratedType<"RestZaakbeeindigReden">>({
    id: "1",
    naam: "Reden 1",
  });

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
      .mockReturnValue(of([reden]));
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
    jest.spyOn(utilService, "compare").mockReturnValue(false);

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

  it("should initialize empty zaakbeeindigParameters list when none provided", () => {
    const component = fixture.componentInstance;
    // zaakbeeindigParameters from parameters fixture is [], only zaakNietOntvankelijk + reden from service
    // The component always adds zaakNietOntvankelijk + redenen from service
    // With one reden mocked, total should be 2 entries
    expect(component.zaakbeeindigParameters.length).toBe(2);
  });

  it("isZaaknietontvankelijkParameter returns true when zaakbeeindigReden is undefined", () => {
    const component = fixture.componentInstance;
    const param = fromPartial<GeneratedType<"RestZaakbeeindigParameter">>({});
    expect(component["isZaaknietontvankelijkParameter"](param)).toBe(true);
  });

  it("isZaaknietontvankelijkParameter returns false when zaakbeeindigReden is set", () => {
    const component = fixture.componentInstance;
    const param = fromPartial<GeneratedType<"RestZaakbeeindigParameter">>({
      zaakbeeindigReden: reden,
    });
    expect(component["isZaaknietontvankelijkParameter"](param)).toBe(false);
  });

  it("changeSelection toggles selection of a parameter", () => {
    const component = fixture.componentInstance;
    // The reden parameter (index 1) is not auto-selected since compareObject returns false
    const redenParam = component.zaakbeeindigParameters[1];
    const wasSelected = component.selection.isSelected(redenParam);

    component["changeSelection"](
      fromPartial<MatCheckboxChange>({ checked: true }),
      redenParam,
    );

    expect(component.selection.isSelected(redenParam)).toBe(!wasSelected);
  });

  it("zaakNietOntvankelijk parameter is auto-selected", () => {
    const component = fixture.componentInstance;
    // The first entry in zaakbeeindigParameters is always the zaakNietOntvankelijk one
    const zaakNietOntvankelijkParam = component.zaakbeeindigParameters[0];
    expect(component.selection.isSelected(zaakNietOntvankelijkParam)).toBe(
      true,
    );
  });

  it("when a reden exists from listZaakbeeindigRedenen, zaakbeeindigParameters contains it", () => {
    const component = fixture.componentInstance;
    const redenParam = component.zaakbeeindigParameters.find(
      (p) => p.zaakbeeindigReden?.naam === "Reden 1",
    );
    expect(redenParam).toBeDefined();
  });
});
