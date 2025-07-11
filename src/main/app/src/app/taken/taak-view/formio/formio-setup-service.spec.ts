/*
 * SPDX-FileCopyrightText: 2025 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

import {
  provideHttpClient,
  withInterceptorsFromDi,
} from "@angular/common/http";
import { provideHttpClientTesting } from "@angular/common/http/testing";
import { TestBed } from "@angular/core/testing";
import { MatSidenav } from "@angular/material/sidenav";
import { NoopAnimationsModule } from "@angular/platform-browser/animations";
import { ActivatedRoute, RouterModule } from "@angular/router";
import { ExtendedComponentSchema, FormioForm } from "@formio/angular";
import { TranslateModule } from "@ngx-translate/core";
import { QueryClient } from "@tanstack/angular-query-experimental";
import { of } from "rxjs";
import { ZaakafhandelParametersService } from "../../../admin/zaakafhandel-parameters.service";
import { UtilService } from "../../../core/service/util.service";
import { IdentityService } from "../../../identity/identity.service";
import { GeneratedType } from "../../../shared/utils/generated-types";
import { FormioSetupService } from "./formio-setup-service";

const groepMedewerkerFieldset: ExtendedComponentSchema = {
  type: "groepMedewerkerFieldset",
  key: "AM_TeamBehandelaar",
  components: [
    {
      type: "select",
      key: "AM_TeamBehandelaar_Groep",
      input: true,
    },
    {
      type: "select",
      key: "AM_TeamBehandelaar_Medewerker",
      input: true,
    },
  ],
};

const smartDocumentsFieldset: ExtendedComponentSchema = {
  type: "smartDocumentsFieldset",
  key: "SD_SmartDocuments",
  components: [
    {
      type: "select",
      key: "SD_SmartDocuments_Template",
      input: true,
    },
    {
      type: "button",
      key: "SD_SmartDocuments_Create",
      input: true,
      properties: {
        SmartDocuments_Group: "Dimpact/OpenZaak",
      },
    },
  ],
};

const documentsFieldset: ExtendedComponentSchema = {
  type: "documentsFieldset",
  key: "ZAAK_Documents",
  components: [
    {
      type: "select",
      key: "ZAAK_Documents_Select",
      input: true,
      multiple: true,
    },
  ],
};

const referenceTableFieldset: ExtendedComponentSchema = {
  type: "referenceTableFieldset",
  key: "RT_ReferenceTable",
  components: [
    {
      type: "select",
      key: "RT_ReferenceTable_Values",
      input: true,
      properties: {
        ReferenceTable_Code: "COMMUNICATIEKANAAL",
      },
    },
  ],
};

describe(FormioSetupService.name, () => {
  let formioSetupService: FormioSetupService;
  let utilService: UtilService;

  const taak: GeneratedType<"RestTask"> = {
    id: "test-id",
    zaakUuid: "test-zaakUuid",
    zaaktypeUUID: "test-zaaktype-uuid",
    behandelaar: undefined,
    groep: undefined,
    naam: "test-taak",
    fataledatum: new Date().toISOString(),
    creatiedatumTijd: new Date().toISOString(),
    formioFormulier: {},
    rechten: {
      lezen: true,
      toekennen: true,
      wijzigen: true,
      toevoegenDocument: true,
    },
    status: "TOEGEKEND",
    taakdata: {},
    formulierDefinitie: undefined,
    formulierDefinitieId: "test-formulierDefinitieId",
    tabellen: {},
    taakdocumenten: [],
    taakinformatie: {},
    toelichting: undefined,
    toekenningsdatumTijd: new Date().toISOString(),
    zaaktypeOmschrijving: "test-zaaktypeOmschrijving",
    zaakIdentificatie: "test-zaakIdentificatie",
  };

  beforeEach(() => {
    TestBed.configureTestingModule({
      imports: [
        MatSidenav,
        RouterModule.forRoot([]),
        TranslateModule.forRoot(),
        NoopAnimationsModule,
      ],
      providers: [
        UtilService,
        IdentityService,
        ZaakafhandelParametersService,
        FormioSetupService,
        QueryClient,
        {
          provide: ActivatedRoute,
          useValue: { data: of({ taak }) },
        },
        provideHttpClient(withInterceptorsFromDi()),
        provideHttpClientTesting(),
      ],
    }).compileComponents();

    formioSetupService = TestBed.inject(FormioSetupService);
    utilService = TestBed.inject(UtilService);
  });

  describe(FormioSetupService.prototype.extractFieldsetName.name, () => {
    it("should extract the fieldset name from a sub-component name", () => {
      const fieldsetName: string = formioSetupService.extractFieldsetName({
        key: "AM_SmartDocuments_Create",
      });
      expect(fieldsetName).toBe("AM_SmartDocuments");
    });
  });

  describe(
    FormioSetupService.prototype.extractSmartDocumentsTemplateName.name,
    () => {
      it("should extract the smart documents template name from a sub-component name", () => {
        const smartDocumentsTemplateName: string =
          formioSetupService.extractSmartDocumentsTemplateName({
            event: undefined,
            type: "unknown",
            component: {
              key: "AM_SmartDocuments_Create",
            },
            data: {
              AM_SmartDocuments_Template: "SmartDocuments template name",
            },
          });
        expect(smartDocumentsTemplateName).toBe("SmartDocuments template name");
      });
    },
  );

  describe(
    FormioSetupService.prototype.normalizeSmartDocumentsTemplateName.name,
    () => {
      it("should normalize the smart documents template name", () => {
        const smartDocumentsTemplateName: string =
          formioSetupService.normalizeSmartDocumentsTemplateName(
            "SmartDocuments template name",
          );
        expect(smartDocumentsTemplateName).toBe("SmartDocuments_template_name");
      });
    },
  );

  describe(
    FormioSetupService.prototype.getInformatieobjecttypeUuid.name,
    () => {
      it("should obtain the informatieobjecttypeUuid from the component properties", () => {
        const uuid: string = formioSetupService.getInformatieobjecttypeUuid(
          {
            event: undefined,
            type: "unknown",
            component: {
              key: "AM_SmartDocuments_Create",
              properties: {
                SmartDocuments_InformatieobjecttypeUuid: "default uuid",
                SmartDocuments_template_name_InformatieobjecttypeUuid:
                  "fb0c792c-cf1c-4474-a361-987fbbb1f9ce",
              },
            },
            data: {
              AM_SmartDocuments_Template: "template name",
            },
          },
          "template_name",
        );
        expect(uuid).toBe("fb0c792c-cf1c-4474-a361-987fbbb1f9ce");
      });

      it("returns the default intormatieobjecttypeUuid when the a specific properties are not set", () => {
        const uuid: string = formioSetupService.getInformatieobjecttypeUuid(
          {
            event: undefined,
            type: "unknown",
            component: {
              key: "AM_SmartDocuments_Create",
              properties: {
                SmartDocuments_InformatieobjecttypeUuid: "default uuid",
              },
            },
            data: {
              AM_SmartDocuments_Template: "template name",
            },
          },
          "template_name",
        );
        expect(uuid).toBe("default uuid");
      });
    },
  );

  describe(FormioSetupService.prototype.getSmartDocumentsGroups.name, () => {
    it("should return the smart documents groups", () => {
      const smartDocumentsGroups: ExtendedComponentSchema[] =
        formioSetupService.getSmartDocumentsGroups({
          properties: {
            SmartDocuments_Group: "root/sub1/sub2",
          },
        });
      expect(smartDocumentsGroups).toStrictEqual(["root", "sub1", "sub2"]);
    });
  });

  describe(FormioSetupService.prototype.createFormioForm.name, () => {
    it("should initialize components for all defined component types", async () => {
      const mockedComponentsService = formioSetupService as unknown as {
        initializeGroepMedewerkerFieldsetComponent: jest.Mock;
        initializeSmartDocumentsFieldsetComponent: jest.Mock;
        initializeReferenceTableFieldsetComponent: jest.Mock;
        initializeAvailableDocumentsFieldsetComponent: jest.Mock;
      };

      const groepMedewerkerSpy = jest.spyOn(
        mockedComponentsService,
        "initializeGroepMedewerkerFieldsetComponent",
      );
      const smartDocumentsSpy = jest.spyOn(
        mockedComponentsService,
        "initializeSmartDocumentsFieldsetComponent",
      );
      const referenceTableSpy = jest.spyOn(
        mockedComponentsService,
        "initializeReferenceTableFieldsetComponent",
      );
      const availableDocumentsSpy = jest.spyOn(
        mockedComponentsService,
        "initializeAvailableDocumentsFieldsetComponent",
      );

      const mockFormComponents: ExtendedComponentSchema[] = [
        groepMedewerkerFieldset,
        smartDocumentsFieldset,
        referenceTableFieldset,
        documentsFieldset,
      ];

      formioSetupService.createFormioForm(
        { components: mockFormComponents } as FormioForm,
        taak,
      );

      expect(groepMedewerkerSpy).toHaveBeenCalledWith(mockFormComponents[0]);
      expect(smartDocumentsSpy).toHaveBeenCalledWith(mockFormComponents[1]);
      expect(referenceTableSpy).toHaveBeenCalledWith(mockFormComponents[2]);
      expect(availableDocumentsSpy).toHaveBeenCalledWith(mockFormComponents[3]);
    });

    it("handle cases for components with no children or properties", () => {
      const components: ExtendedComponentSchema[] = [
        {
          type: "referenceTableFieldset",
          key: "RT_Fail",
          components: [
            {
              key: "RT_Fail_Values",
              type: "select",
              properties: {
                ReferenceTable_Code: "dummy",
              },
            },
          ],
        },
        {
          type: "smartDocumentsFieldset",
          key: "SD_Fail",
          components: [
            {
              key: "SD_Fail_Template",
              type: "select",
              input: true,
            },
            {
              key: "SD_Fail_Create",
              type: "button",
              properties: {
                SmartDocuments_Group: "groep1/groep2",
              },
            },
          ],
        },
      ];

      expect(() => {
        formioSetupService.createFormioForm({ components } as FormioForm, taak);
      }).not.toThrow();
    });

    it("should invoke userGroupsQuery with zaakTypeUUID", async () => {
      const identityServiceSpy = jest
        .spyOn(formioSetupService["identityService"], "listGroups")
        .mockReturnValue(of([]));

      const groepComponent: ExtendedComponentSchema = {
        key: "groep",
        type: "select",
        input: true,
      };

      const medewerkerComponent: ExtendedComponentSchema = {
        key: "medewerker",
        type: "select",
        input: true,
      };

      formioSetupService.createFormioForm(
        {
          components: [
            {
              type: "groepMedewerkerFieldset",
              key: "fieldset",
              components: [groepComponent, medewerkerComponent],
            },
          ],
        } as FormioForm,
        taak,
      );

      await groepComponent.data.custom();

      expect(identityServiceSpy).toHaveBeenCalledWith("test-zaaktype-uuid");
    });

    it("should catch errors from component initializers and call handleFormIOInitError", () => {
      const component: ExtendedComponentSchema = {
        type: "smartDocumentsFieldset",
        key: "component_key",
        components: [],
      };
      const errorMessage = "failed to initialize";
      const spy = jest.spyOn(utilService, "handleFormIOInitError");

      jest
        .spyOn(
          formioSetupService as unknown as {
            initializeSmartDocumentsFieldsetComponent: jest.Mock;
          },
          "initializeSmartDocumentsFieldsetComponent",
        )
        .mockImplementation(() => {
          throw new Error(errorMessage);
        });

      expect(() => {
        formioSetupService.createFormioForm(
          { components: [component] } as FormioForm,
          taak,
        );
      }).not.toThrow();

      expect(spy).toHaveBeenCalledWith("component_key", errorMessage);
    });
  });

  describe(FormioSetupService.prototype.setFormioChangeData.name, () => {
    it("should update formioChangeData", async () => {
      const groepComponent: ExtendedComponentSchema = {
        key: "GroepKey",
        type: "select",
        input: true,
      };

      const medewerkerComponent: ExtendedComponentSchema = {
        key: "MedewerkerKey",
        type: "select",
        input: true,
      };

      formioSetupService.setFormioChangeData({ GroepKey: "group-uuid" });
      const identityServiceSpy = jest
        .spyOn(formioSetupService["identityService"], "listUsersInGroup")
        .mockReturnValue(of([]));

      formioSetupService.createFormioForm(
        {
          components: [
            {
              type: "groepMedewerkerFieldset",
              key: "fieldset",
              components: [groepComponent, medewerkerComponent],
            },
          ],
        } as FormioForm,
        taak,
      );

      await medewerkerComponent.data.custom();
      expect(identityServiceSpy).toHaveBeenCalledWith("group-uuid");
    });
  });
});
