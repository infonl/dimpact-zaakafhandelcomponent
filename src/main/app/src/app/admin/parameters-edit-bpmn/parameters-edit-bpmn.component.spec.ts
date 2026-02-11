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
import { MatSelectHarness } from "@angular/material/select/testing";
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
import { ParametersEditBpmnComponent } from "./parameters-edit-bpmn.component";

describe(ParametersEditBpmnComponent.name, () => {
  let fixture: ComponentFixture<ParametersEditBpmnComponent>;
  let zaakafhandelParametersService: ZaakafhandelParametersService;
  let referentieTabelService: ReferentieTabelService;
  let identityService: IdentityService;
  let mailtemplateBeheerService: MailtemplateBeheerService;
  let loader: HarnessLoader;
  let utilService: UtilService;

  const bpmnZaakafhandelParameters: Partial<
    GeneratedType<"RestZaaktypeBpmnConfiguration"> & {
      zaaktype: GeneratedType<"RestZaaktype">;
    }
  > = fromPartial({
    zaaktypeUuid: "zaaktypeuuid",
    zaaktypeOmschrijving: "omschrijving",
    bpmnProcessDefinitionKey: "bpmnProcessDefinitionKey",
    productaanvraagtype: null,
    groepNaam: "test-group-bpmn",
    zaaktype: {
      uuid: "test-uuid",
      identificatie: "test-definitie",
      doel: "test-doel",
      omschrijving: "test-omschrijving",
    },
  });

  const bpmnProcessDefinitions: GeneratedType<"RestBpmnProcessDefinition">[] = [
    {
      id: "RestBpmnProcessDefinition-1",
      key: "itProcessDefinition-2",
      name: "BPMN Process Definition - 2",
      version: 1,
    },
    {
      id: "RestBpmnProcessDefinition-2",
      key: "itProcessDefinition-2",
      name: "BPMN Process Definition - 2",
      version: 1,
    },
  ];

  const zaakafhandelParameters = fromPartial<
    GeneratedType<"RestZaakafhandelParameters">
  >({
    defaultGroepId: "test-group-id",
    defaultBehandelaarId: "test-user-id",
    zaaktype: {
      uuid: "test-uuid",
    },
  });

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      declarations: [
        ParametersEditBpmnComponent,
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
                bpmnZaakafhandelParameters,
                bpmnProcessDefinitions,
                isSavedZaakafhandelParameters: true,
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
    jest
      .spyOn(referentieTabelService, "listBrpViewValues")
      .mockReturnValue(of([]));
    jest
      .spyOn(referentieTabelService, "listBrpProcessingValues")
      .mockReturnValue(of([]));
    jest
      .spyOn(referentieTabelService, "listBrpSearchValues")
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

    fixture = TestBed.createComponent(ParametersEditBpmnComponent);
    fixture.detectChanges();
    await fixture.whenStable();

    loader = TestbedHarnessEnvironment.loader(fixture);
  });

  describe("Case handler", () => {
    it("should set the case handlers selected group", async () => {
      const selectFields = await loader.getAllHarnesses(MatSelectHarness);
      const processDefinitionField = selectFields[0];

      const processDefinitionFieldValue =
        await processDefinitionField.getValueText();
      expect(processDefinitionFieldValue).toBe("-kies.generiek-");

      const groupField = selectFields[1];

      const value = await groupField.getValueText();
      expect(value).toBe("test-group");
    });
  });
});
