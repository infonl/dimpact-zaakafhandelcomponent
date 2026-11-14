/*
 * SPDX-FileCopyrightText: 2025 Lifely
 * SPDX-License-Identifier: EUPL-1.2+
 *
 */

import { HarnessLoader } from "@angular/cdk/testing";
import { TestbedHarnessEnvironment } from "@angular/cdk/testing/testbed";
import { provideHttpClient } from "@angular/common/http";
import { provideHttpClientTesting } from "@angular/common/http/testing";
import { ComponentFixture, TestBed } from "@angular/core/testing";
import { MatSelectHarness } from "@angular/material/select/testing";
import { NoopAnimationsModule } from "@angular/platform-browser/animations";
import { ActivatedRoute, RouterModule } from "@angular/router";
import { TranslateModule } from "@ngx-translate/core";
import { fromPartial } from "@total-typescript/shoehorn";
import { of } from "rxjs";
import { UtilService } from "../../core/service/util.service";
import { IdentityService } from "../../identity/identity.service";
import { MaterialModule } from "../../shared/material/material.module";
import { PipesModule } from "../../shared/pipes/pipes.module";
import { SideNavComponent } from "../../shared/side-nav/side-nav.component";
import { StaticTextComponent } from "../../shared/static-text/static-text.component";
import { GeneratedType } from "../../shared/utils/generated-types";
import { MailtemplateBeheerService } from "../mailtemplate-beheer.service";
import { ReferentieTabelService } from "../referentie-tabel.service";
import { ZaakafhandelParametersService } from "../zaakafhandel-parameters.service";
import { ParameterEditComponent } from "./parameter-edit.component";

describe(ParameterEditComponent.name, () => {
  let fixture: ComponentFixture<ParameterEditComponent>;
  let zaakafhandelParametersService: ZaakafhandelParametersService;
  let referentieTabelService: ReferentieTabelService;
  let identityService: IdentityService;
  let mailtemplateBeheerService: MailtemplateBeheerService;
  let loader: HarnessLoader;
  let utilService: UtilService;

  const zaakAfhandelParamaters = fromPartial<
    GeneratedType<"RestZaakafhandelParameters">
  >({
    defaultGroepId: "test-group-id",
    defaultBehandelaarId: "test-user-id",
    zaaktype: {
      uuid: "test-uuid",
    },
    humanTaskParameters: [],
    userEventListenerParameters: [],
    mailtemplateKoppelingen: [],
    zaakAfzenders: [],
    smartDocuments: {},
  });

  const users: GeneratedType<"RestUser">[] = [
    { id: "test-user-id", naam: "test-user" },
    { id: "test-user-id-2", naam: "test-user-2" },
  ];

  const groups: GeneratedType<"RestGroup">[] = [
    { id: "test-group-id", naam: "test-group" },
    { id: "test-group-id-2", naam: "test-group-2" },
  ];

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      declarations: [
        ParameterEditComponent,
        SideNavComponent,
        StaticTextComponent,
      ],
      imports: [
        TranslateModule.forRoot(),
        MaterialModule,
        NoopAnimationsModule,
        RouterModule,
        PipesModule,
      ],
      providers: [
        provideHttpClient(),
        provideHttpClientTesting(),
        {
          provide: ActivatedRoute,
          useValue: {
            data: of({ parameters: zaakAfhandelParamaters }),
          },
        },
      ],
    }).compileComponents();

    zaakafhandelParametersService = TestBed.inject(
      ZaakafhandelParametersService,
    );
    jest
      .spyOn(zaakafhandelParametersService, "listCaseDefinitions")
      .mockReturnValue(of([]));
    jest
      .spyOn(zaakafhandelParametersService, "listFormulierDefinities")
      .mockReturnValue(of([]));
    jest
      .spyOn(zaakafhandelParametersService, "listReplyTos")
      .mockReturnValue(of([]));
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
    jest.spyOn(referentieTabelService, "listAfzenders").mockReturnValue(of([]));

    identityService = TestBed.inject(IdentityService);
    jest.spyOn(identityService, "listGroups").mockReturnValue(of(groups));
    jest
      .spyOn(identityService, "listUsersInGroup")
      .mockReturnValueOnce(of(users))
      .mockReturnValue(of([]));

    utilService = TestBed.inject(UtilService);
    jest.spyOn(utilService, "compare").mockReturnValue(true);

    mailtemplateBeheerService = TestBed.inject(MailtemplateBeheerService);
    jest
      .spyOn(mailtemplateBeheerService, "listKoppelbareMailtemplates")
      .mockReturnValue(of([]));

    fixture = TestBed.createComponent(ParameterEditComponent);
    loader = TestbedHarnessEnvironment.loader(fixture);
  });

  describe("Case handler", () => {
    it("should set the case handlers which are in the selected group", async () => {
      const selectFields = await loader.getAllHarnesses(MatSelectHarness);
      const caseHandlerSelect = selectFields[3];

      const value = await caseHandlerSelect.getValueText();
      expect(value).toBe("test-user");
    });

    it("should update the case handlers when the group changes", async () => {
      const selectFields = await loader.getAllHarnesses(MatSelectHarness);

      const groupField = selectFields[2];
      await groupField.clickOptions({ text: "test-group-2" });

      const caseHandlerSelect = selectFields[3];

      const value = await caseHandlerSelect.getValueText();
      expect(value).toBe("behandelaar.-geen-");
    });
  });
});
