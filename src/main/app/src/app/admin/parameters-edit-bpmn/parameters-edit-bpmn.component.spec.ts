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
import { MatButtonHarness } from "@angular/material/button/testing";
import { MatDialog } from "@angular/material/dialog";
import { MatSelectHarness } from "@angular/material/select/testing";
import { NoopAnimationsModule } from "@angular/platform-browser/animations";
import { ActivatedRoute } from "@angular/router";
import { TranslateModule } from "@ngx-translate/core";
import { of } from "rxjs";
import { fromPartial } from "src/test-helpers";
import { ConfiguratieService } from "../../configuratie/configuratie.service";
import { UtilService } from "../../core/service/util.service";
import { IdentityService } from "../../identity/identity.service";
import { MaterialFormBuilderModule } from "../../shared/material-form-builder/material-form-builder.module";
import { MaterialModule } from "../../shared/material/material.module";
import { StaticTextComponent } from "../../shared/static-text/static-text.component";
import { GeneratedType } from "../../shared/utils/generated-types";
import { ProcessModelMethodSelection } from "../model/parameters/process-model-method";
import { ReferentieTabelService } from "../referentie-tabel.service";
import { ZaakafhandelParametersService } from "../zaakafhandel-parameters.service";
import { ParametersEditBpmnComponent } from "./parameters-edit-bpmn.component";

describe(ParametersEditBpmnComponent.name, () => {
  let fixture: ComponentFixture<ParametersEditBpmnComponent>;
  let component: ParametersEditBpmnComponent;
  let zaakafhandelParametersService: ZaakafhandelParametersService;
  let referentieTabelService: ReferentieTabelService;
  let identityService: IdentityService;
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
      details: {
        inUse: true,
      },
    },
    {
      id: "RestBpmnProcessDefinition-2",
      key: "itProcessDefinition-2",
      name: "BPMN Process Definition - 2",
      version: 1,
      details: {
        inUse: true,
      },
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
      imports: [
        ParametersEditBpmnComponent,
        StaticTextComponent,
        TranslateModule.forRoot(),
        MaterialModule,
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
          } satisfies Pick<ActivatedRoute, "data">,
        },
      ],
    }).compileComponents();

    zaakafhandelParametersService = TestBed.inject(
      ZaakafhandelParametersService,
    );
    jest
      .spyOn(zaakafhandelParametersService, "listZaakbeeindigRedenen")
      .mockReturnValue(of([]));
    jest
      .spyOn(zaakafhandelParametersService, "listResultaattypes")
      .mockReturnValue(of([]));

    referentieTabelService = TestBed.inject(ReferentieTabelService);
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

    const configuratieService = TestBed.inject(ConfiguratieService);
    jest
      .spyOn(configuratieService, "readBrpDoelbindingSetupEnabled")
      .mockReturnValue(of(false));

    fixture = TestBed.createComponent(ParametersEditBpmnComponent);
    component = fixture.componentInstance;
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

  describe("opslaan", () => {
    it("should disable opslaan when the form is invalid", async () => {
      expect(component["algemeenFormGroup"].controls.bpmnDefinition.value).toBe(
        null,
      );
      expect(component["algemeenFormGroup"].invalid).toBe(true);

      const opslaanButton = await loader.getHarness(
        MatButtonHarness.with({ text: /actie\.opslaan/ }),
      );
      expect(await opslaanButton.isDisabled()).toBe(true);
    });
  });

  describe("switchModellingMethod", () => {
    it("should emit CMMN when selected and form is not dirty", () => {
      const emitted: ProcessModelMethodSelection[] = [];
      fixture.componentInstance.switchModellingMethod.subscribe((v) =>
        emitted.push(v),
      );

      component["cmmnBpmnFormGroup"].enable({ emitEvent: false });
      component["cmmnBpmnFormGroup"].controls.options.setValue({
        value: "CMMN",
        label: "CMMN",
      });

      expect(emitted).toEqual([{ type: "CMMN" }]);
    });

    it("should open confirm dialog when CMMN selected while form is dirty", () => {
      const dialog = fixture.debugElement.injector.get(MatDialog);
      jest.spyOn(dialog, "open").mockReturnValue({
        afterClosed: () => of(false),
      } as ReturnType<MatDialog["open"]>);

      component["cmmnBpmnFormGroup"].enable({ emitEvent: false });
      component["algemeenFormGroup"].markAsDirty();
      component["cmmnBpmnFormGroup"].controls.options.setValue({
        value: "CMMN",
        label: "CMMN",
      });

      expect(dialog.open).toHaveBeenCalled();
    });
  });
});
