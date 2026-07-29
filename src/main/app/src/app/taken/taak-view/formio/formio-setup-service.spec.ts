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
import {
  provideQueryClient,
  QueryClient,
} from "@tanstack/angular-query-experimental";
import { of } from "rxjs";
import { testQueryClient } from "../../../../../setupJest";
import { ZaakafhandelParametersService } from "../../../admin/zaakafhandel-parameters.service";
import { UtilService } from "../../../core/service/util.service";
import { IdentityService } from "../../../identity/identity.service";
import { GeneratedType } from "../../../shared/utils/generated-types";
import {
  FormioSetupService,
  KNOWN_ZAC_FIELDS,
  ZAC_FIELD_ATTRIBUTE,
} from "./formio-setup-service";

const groepComponent: ExtendedComponentSchema = {
  type: "select",
  key: "AM_TeamBehandelaar_Groep",
  input: true,
  attributes: {
    [ZAC_FIELD_ATTRIBUTE]: KNOWN_ZAC_FIELDS.GROEP,
  },
};

const medewerkerComponent: ExtendedComponentSchema = {
  type: "select",
  key: "AM_TeamBehandelaar_Medewerker",
  input: true,
  attributes: {
    [ZAC_FIELD_ATTRIBUTE]: KNOWN_ZAC_FIELDS.MEDEWERKER,
  },
};

const smartDocumentsTemplateGroupsComponent: ExtendedComponentSchema = {
  type: "select",
  key: "Fake_Smart_Documents_Template_Groups",
  input: true,
  attributes: {
    [ZAC_FIELD_ATTRIBUTE]: KNOWN_ZAC_FIELDS.SMART_DOCUMENTS_TEMPLATE_GROUPS,
  },
};

const smartDocumentsTemplateGroupTemplatesComponent: ExtendedComponentSchema = {
  type: "select",
  key: "SD_SmartDocuments_TemplateGroupTemplates",
  input: true,
  refreshOn: "Fake_Smart_Documents_Template_Group_Templates",
  attributes: {
    [ZAC_FIELD_ATTRIBUTE]:
      KNOWN_ZAC_FIELDS.SMART_DOCUMENTS_TEMPLATE_GROUP_TEMPLATES,
  },
};

const documentsFieldset: ExtendedComponentSchema = {
  type: "select",
  key: "ZAAK_Documents_Select",
  input: true,
  multiple: true,
  attributes: {
    [ZAC_FIELD_ATTRIBUTE]: KNOWN_ZAC_FIELDS.DOCUMENTEN,
  },
};

const unsignedDocumentsFieldset: ExtendedComponentSchema = {
  type: "datagrid",
  key: "ZAAK_Documenten_Ondertekenen_Selectie",
  input: true,
  attributes: {
    [ZAC_FIELD_ATTRIBUTE]: KNOWN_ZAC_FIELDS.DOCUMENTEN_UNSIGNED,
  },
};

const selectedUnsignedDocumentsFieldset: ExtendedComponentSchema = {
  type: "datagrid",
  key: "ZAAK_Documenten_Te_Ondertekenen",
  input: true,
  refreshOn: "ZAAK_Documenten_Ondertekenen_Selectie",
  attributes: {
    [ZAC_FIELD_ATTRIBUTE]: KNOWN_ZAC_FIELDS.DOCUMENTEN_UNSIGNED_SELECTED,
  },
};

const referenceTableFieldset: ExtendedComponentSchema = {
  type: "select",
  key: "RT_ReferenceTable_Values",
  input: true,
  properties: {
    ReferenceTable_Code: "COMMUNICATIEKANAAL",
  },
  attributes: {
    [ZAC_FIELD_ATTRIBUTE]: KNOWN_ZAC_FIELDS.REFERENTIE_TABEL,
  },
};

const document1 = { uuid: "doc-1", titel: "Document One" };
const document2 = { uuid: "doc-2", titel: "Document Two" };
const signedDocument = {
  uuid: "doc-3",
  titel: "Document Three",
  ondertekening: { soort: "Digitaal", datum: "2026-01-01" },
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
        provideQueryClient(testQueryClient),
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
    FormioSetupService.prototype.extractSmartDocumentsGroupId.name,
    () => {
      it("should extract the smart documents group id from a sub-component name", () => {
        const smartDocumentsGroupId =
          formioSetupService.extractSmartDocumentsGroupId({
            event: undefined,
            type: "unknown",
            component: {
              key: "AM_SmartDocuments_Create",
            },
            data: {
              AM_SmartDocuments_Group: "SmartDocuments group id",
            },
          });
        expect(smartDocumentsGroupId).toBe("SmartDocuments group id");
      });
    },
  );

  describe(
    FormioSetupService.prototype.extractSmartDocumentsTemplateId.name,
    () => {
      it("should extract the smart documents template id from a sub-component name", () => {
        const smartDocumentsTemplateId =
          formioSetupService.extractSmartDocumentsTemplateId({
            event: undefined,
            type: "unknown",
            component: {
              key: "AM_SmartDocuments_Create",
            },
            data: {
              AM_SmartDocuments_Template: "SmartDocuments template id",
            },
          });
        expect(smartDocumentsTemplateId).toBe("SmartDocuments template id");
      });
    },
  );

  describe(FormioSetupService.prototype.createFormioForm.name, () => {
    it("should initialize components for all defined component types", async () => {
      // the datagrid initializers fetch eagerly, so without this the spies call through to http
      jest.spyOn(testQueryClient, "ensureQueryData").mockResolvedValue([]);

      const mockedComponentsService = formioSetupService as unknown as {
        initializeGroepField: jest.Mock;
        initializeMedewerkerField: jest.Mock;
        initializeProcessDataField: jest.Mock;
        initializeReferenceTableField: jest.Mock;
        initializeDocumentsField: jest.Mock;
        initializeUnsignedDocumentsDatagrid: jest.Mock;
        initializeSelectedUnsignedDocumentsDatagrid: jest.Mock;
        initializeSmartDocumentsTemplateGroupsField: jest.Mock;
        initializeSmartDocumentsTemplateGroupTemplatesField: jest.Mock;
      };

      const groepSpy = jest.spyOn(
        mockedComponentsService,
        "initializeGroepField",
      );

      const medewerkerSpy = jest.spyOn(
        mockedComponentsService,
        "initializeMedewerkerField",
      );

      const referenceTableSpy = jest.spyOn(
        mockedComponentsService,
        "initializeReferenceTableField",
      );
      const availableDocumentsSpy = jest.spyOn(
        mockedComponentsService,
        "initializeDocumentsField",
      );
      const unsignedDocumentsSpy = jest.spyOn(
        mockedComponentsService,
        "initializeUnsignedDocumentsDatagrid",
      );
      const selectedUnsignedDocumentsSpy = jest.spyOn(
        mockedComponentsService,
        "initializeSelectedUnsignedDocumentsDatagrid",
      );
      const templateGroupsSpy = jest.spyOn(
        mockedComponentsService,
        "initializeSmartDocumentsTemplateGroupsField",
      );
      const templateGroupTemplatesSpy = jest.spyOn(
        mockedComponentsService,
        "initializeSmartDocumentsTemplateGroupTemplatesField",
      );

      const mockFormComponents: ExtendedComponentSchema[] = [
        groepComponent,
        medewerkerComponent,
        referenceTableFieldset,
        documentsFieldset,
        smartDocumentsTemplateGroupsComponent,
        smartDocumentsTemplateGroupTemplatesComponent,
        unsignedDocumentsFieldset,
        selectedUnsignedDocumentsFieldset,
      ];

      await formioSetupService.createFormioForm(
        { components: mockFormComponents } as FormioForm,
        taak,
      );

      expect(groepSpy).toHaveBeenCalledWith(mockFormComponents[0]);
      expect(medewerkerSpy).toHaveBeenCalledWith(mockFormComponents[1]);
      expect(referenceTableSpy).toHaveBeenCalledWith(mockFormComponents[2]);
      expect(availableDocumentsSpy).toHaveBeenCalledWith(mockFormComponents[3]);
      expect(templateGroupsSpy).toHaveBeenCalledWith(mockFormComponents[4]);
      expect(templateGroupTemplatesSpy).toHaveBeenCalledWith(
        mockFormComponents[5],
      );
      expect(unsignedDocumentsSpy).toHaveBeenCalledWith(mockFormComponents[6]);
      expect(selectedUnsignedDocumentsSpy).toHaveBeenCalledWith(
        mockFormComponents[7],
      );
    });

    it("handle cases for components with no children or properties", async () => {
      const components: ExtendedComponentSchema[] = [
        {
          key: "RT_Fail_Values",
          type: "select",
          properties: {
            ReferenceTable_Code: "dummy",
          },
          attributes: {
            [ZAC_FIELD_ATTRIBUTE]: KNOWN_ZAC_FIELDS.REFERENTIE_TABEL,
          },
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

      await formioSetupService.createFormioForm(
        { components } as FormioForm,
        taak,
      );
    });

    it("should invoke behandelaar groups for zaaktype description endpoint", async () => {
      const clientQuerySpy = jest
        .spyOn(testQueryClient, "ensureQueryData")
        .mockResolvedValue([]);

      const groepComponent: ExtendedComponentSchema = {
        key: "groep",
        type: "select",
        attributes: {
          [ZAC_FIELD_ATTRIBUTE]: KNOWN_ZAC_FIELDS.GROEP,
        },
        input: true,
      };

      const medewerkerComponent: ExtendedComponentSchema = {
        key: "medewerker",
        type: "select",
        attributes: {
          [ZAC_FIELD_ATTRIBUTE]: KNOWN_ZAC_FIELDS.MEDEWERKER,
        },
        input: true,
      };

      await formioSetupService.createFormioForm(
        {
          components: [groepComponent, medewerkerComponent],
        } as FormioForm,
        taak,
      );

      await groepComponent.data.custom();

      expect(clientQuerySpy).toHaveBeenCalledWith(
        expect.objectContaining({
          queryKey: [
            "/rest/identity/zaaktype/{zaaktypeDescription}/behandelaar-groups",
            { path: { zaaktypeDescription: "test-zaaktypeOmschrijving" } },
          ],
        }),
      );
    });

    it("should catch errors from component initializers and call handleFormIOInitError", async () => {
      const component: ExtendedComponentSchema = {
        type: "select",
        key: "component_key",
        attributes: {
          [ZAC_FIELD_ATTRIBUTE]:
            KNOWN_ZAC_FIELDS.SMART_DOCUMENTS_TEMPLATE_GROUPS,
        },
      };
      const errorMessage = "failed to initialize";
      const handleFormIOInitErrorSpy = jest.spyOn(
        utilService,
        "handleFormIOInitError",
      );

      jest
        .spyOn(
          formioSetupService as unknown as {
            initializeSmartDocumentsTemplateGroupsField: jest.Mock;
          },
          "initializeSmartDocumentsTemplateGroupsField",
        )
        .mockImplementation(() => {
          throw new Error(errorMessage);
        });

      await formioSetupService.createFormioForm(
        { components: [component] } as FormioForm,
        taak,
      );

      expect(handleFormIOInitErrorSpy).toHaveBeenCalledWith(
        "ZAC_smart_documents_template_groups",
        errorMessage,
      );
    });
  });

  describe(
    (FormioSetupService.prototype as unknown as Record<string, () => unknown>)[
      "initializeSmartDocumentsTemplateGroupsField"
    ].name,
    () => {
      const mockFlattenedGroups = [
        { id: "group-2", name: "Zebra Group", templates: [] },
        { id: "group-1", name: "Alpha Group", templates: [] },
      ];

      it("should set valueProperty and template on the component", async () => {
        jest.spyOn(testQueryClient, "ensureQueryData").mockResolvedValue([]);

        const component: ExtendedComponentSchema = {
          ...smartDocumentsTemplateGroupsComponent,
        };
        await formioSetupService.createFormioForm(
          { components: [component] } as FormioForm,
          taak,
        );

        expect(component.valueProperty).toBe("id");
        expect(component.template).toBe("{{ item.naam }}");
      });

      it("should query smartdocuments templates mapping with zaaktype UUID", async () => {
        const ensureQueryDataSpy = jest
          .spyOn(testQueryClient, "ensureQueryData")
          .mockResolvedValue([]);

        const component: ExtendedComponentSchema = {
          ...smartDocumentsTemplateGroupsComponent,
        };
        await formioSetupService.createFormioForm(
          { components: [component] } as FormioForm,
          taak,
        );

        await component.data.custom();

        expect(ensureQueryDataSpy).toHaveBeenCalledWith(
          expect.objectContaining({
            queryKey: ["smartDocumentsTemplatesMapping", "test-zaaktype-uuid"],
          }),
        );
      });

      it("should return groups mapped to {id, naam, active} sorted by naam", async () => {
        jest
          .spyOn(testQueryClient, "ensureQueryData")
          .mockResolvedValue(mockFlattenedGroups);

        const component: ExtendedComponentSchema = {
          ...smartDocumentsTemplateGroupsComponent,
        };
        await formioSetupService.createFormioForm(
          { components: [component] } as FormioForm,
          taak,
        );

        const result = await component.data.custom();

        expect(result).toEqual([
          { id: "group-1", naam: "Alpha Group", active: false },
          { id: "group-2", naam: "Zebra Group", active: false },
        ]);
      });
    },
  );

  describe(
    (FormioSetupService.prototype as unknown as Record<string, () => unknown>)[
      "initializeSmartDocumentsTemplateGroupTemplatesField"
    ].name,
    () => {
      const templateGroupId = "group-1";
      const mockTemplates = [
        {
          id: "tmpl-1",
          name: "Template A",
          informatieObjectTypeUUID: "uuid-1",
        },
        {
          id: "tmpl-2",
          name: "Template B",
          informatieObjectTypeUUID: "uuid-2",
        },
      ];
      const mockFlattenedGroups = [
        { id: templateGroupId, name: "Group 1", templates: mockTemplates },
        { id: "group-2", name: "Group 2", templates: [] },
      ];

      it("should set valueProperty and template on the component", async () => {
        jest.spyOn(testQueryClient, "ensureQueryData").mockResolvedValue([]);

        const component: ExtendedComponentSchema = {
          ...smartDocumentsTemplateGroupTemplatesComponent,
        };
        await formioSetupService.createFormioForm(
          { components: [component] } as FormioForm,
          taak,
        );

        expect(component.valueProperty).toBe("id");
        expect(component.template).toBe("{{ item.naam }}");
      });

      it("should return templates for the matching group from formioChangeData", async () => {
        jest
          .spyOn(testQueryClient, "ensureQueryData")
          .mockResolvedValue(mockFlattenedGroups);

        const component: ExtendedComponentSchema = {
          ...smartDocumentsTemplateGroupTemplatesComponent,
        };
        await formioSetupService.createFormioForm(
          { components: [component] } as FormioForm,
          taak,
        );
        formioSetupService.setFormioChangeData({
          [smartDocumentsTemplateGroupTemplatesComponent.refreshOn]:
            templateGroupId,
        });

        const result = await component.data.custom();

        expect(result).toEqual([
          { id: "tmpl-1", naam: "Template A", active: false },
          { id: "tmpl-2", naam: "Template B", active: false },
        ]);
      });

      it("should return templates mapped to {id, naam, active} sorted by naam", async () => {
        const unsortedTemplates = [
          {
            id: "tmpl-2",
            name: "Zebra Template",
            informatieObjectTypeUUID: "uuid-2",
          },
          {
            id: "tmpl-1",
            name: "Alpha Template",
            informatieObjectTypeUUID: "uuid-1",
          },
        ];
        const flattenedGroupsWithUnsortedTemplates = [
          {
            id: templateGroupId,
            name: "Group 1",
            templates: unsortedTemplates,
          },
        ];
        jest
          .spyOn(testQueryClient, "ensureQueryData")
          .mockResolvedValue(flattenedGroupsWithUnsortedTemplates);

        const component: ExtendedComponentSchema = {
          ...smartDocumentsTemplateGroupTemplatesComponent,
        };
        await formioSetupService.createFormioForm(
          { components: [component] } as FormioForm,
          taak,
        );
        formioSetupService.setFormioChangeData({
          [smartDocumentsTemplateGroupTemplatesComponent.refreshOn]:
            templateGroupId,
        });

        const result = await component.data.custom();

        expect(result).toEqual([
          { id: "tmpl-1", naam: "Alpha Template", active: false },
          { id: "tmpl-2", naam: "Zebra Template", active: false },
        ]);
      });

      it("should return empty array when no group matches formioChangeData", async () => {
        jest
          .spyOn(testQueryClient, "ensureQueryData")
          .mockResolvedValue(mockFlattenedGroups);

        const component: ExtendedComponentSchema = {
          ...smartDocumentsTemplateGroupTemplatesComponent,
        };
        await formioSetupService.createFormioForm(
          { components: [component] } as FormioForm,
          taak,
        );
        formioSetupService.setFormioChangeData({
          [smartDocumentsTemplateGroupTemplatesComponent.refreshOn]:
            "non-existent-group",
        });

        const result = await component.data.custom();

        expect(result).toEqual([]);
      });

      it("should return empty array when formioChangeData is not set", async () => {
        jest.spyOn(testQueryClient, "ensureQueryData").mockResolvedValue([]);

        const component: ExtendedComponentSchema = {
          ...smartDocumentsTemplateGroupTemplatesComponent,
        };
        await formioSetupService.createFormioForm(
          { components: [component] } as FormioForm,
          taak,
        );

        const result = await component.data.custom();

        expect(result).toEqual([]);
      });
    },
  );

  describe(
    (FormioSetupService.prototype as unknown as Record<string, () => unknown>)[
      "initializeDocumentsField"
    ].name,
    () => {
      it("should set valueProperty, template and a custom data source for a select component", async () => {
        const ensureQueryDataSpy = jest
          .spyOn(testQueryClient, "ensureQueryData")
          .mockResolvedValue([document1, document2]);

        const component: ExtendedComponentSchema = { ...documentsFieldset };
        await formioSetupService.createFormioForm(
          { components: [component] } as FormioForm,
          taak,
        );

        expect(component.valueProperty).toBe("uuid");
        expect(component.template).toBe("{{ item.titel }}");
        await expect(component.data.custom()).resolves.toEqual([
          document1,
          document2,
        ]);
        expect(ensureQueryDataSpy).toHaveBeenCalledWith(
          expect.objectContaining({
            queryKey: ["availableDocumentsQuery", taak.zaakUuid, undefined],
          }),
        );
      });

      it("should not filter out already-signed documents", async () => {
        jest
          .spyOn(testQueryClient, "ensureQueryData")
          .mockResolvedValue([document1, signedDocument]);

        const component: ExtendedComponentSchema = { ...documentsFieldset };
        await formioSetupService.createFormioForm(
          { components: [component] } as FormioForm,
          taak,
        );

        await expect(component.data.custom()).resolves.toEqual([
          document1,
          signedDocument,
        ]);
      });
    },
  );

  describe(
    (FormioSetupService.prototype as unknown as Record<string, () => unknown>)[
      "initializeUnsignedDocumentsDatagrid"
    ].name,
    () => {
      it("should populate the datagrid with all zaak documents, unselected", async () => {
        jest
          .spyOn(testQueryClient, "ensureQueryData")
          .mockResolvedValue([document1, document2]);

        const component: ExtendedComponentSchema = {
          ...unsignedDocumentsFieldset,
        };
        await formioSetupService.createFormioForm(
          { components: [component] } as FormioForm,
          taak,
        );

        expect(component.defaultValue).toEqual([
          { selected: false, titel: document1.titel, uuid: document1.uuid },
          { selected: false, titel: document2.titel, uuid: document2.uuid },
        ]);
      });

      it("should exclude already-signed documents", async () => {
        jest
          .spyOn(testQueryClient, "ensureQueryData")
          .mockResolvedValue([document1, document2, signedDocument]);

        const component: ExtendedComponentSchema = {
          ...unsignedDocumentsFieldset,
        };
        await formioSetupService.createFormioForm(
          { components: [component] } as FormioForm,
          taak,
        );

        expect(component.defaultValue).toEqual([
          { selected: false, titel: document1.titel, uuid: document1.uuid },
          { selected: false, titel: document2.titel, uuid: document2.uuid },
        ]);
      });
    },
  );

  describe(
    (FormioSetupService.prototype as unknown as Record<string, () => unknown>)[
      "initializeSelectedUnsignedDocumentsDatagrid"
    ].name,
    () => {
      const taakWithSelection = (
        rows: { selected: boolean; titel: string; uuid: string }[],
      ): GeneratedType<"RestTask"> => ({
        ...taak,
        taakdata: { ZAAK_Documenten_Ondertekenen_Selectie: rows },
      });

      it("should show only the previously selected documents, pre-selected", async () => {
        jest
          .spyOn(testQueryClient, "ensureQueryData")
          .mockResolvedValue([document1]);

        const component: ExtendedComponentSchema = {
          ...selectedUnsignedDocumentsFieldset,
        };
        await formioSetupService.createFormioForm(
          { components: [component] } as FormioForm,
          taakWithSelection([
            { selected: true, titel: document1.titel, uuid: document1.uuid },
            { selected: false, titel: document2.titel, uuid: document2.uuid },
          ]),
        );

        expect(component.defaultValue).toEqual([
          { selected: true, titel: document1.titel, uuid: document1.uuid },
        ]);
      });

      it("should re-fetch only the selected documents, keyed on their uuids", async () => {
        const ensureQueryDataSpy = jest
          .spyOn(testQueryClient, "ensureQueryData")
          .mockResolvedValue([document1]);

        const component: ExtendedComponentSchema = {
          ...selectedUnsignedDocumentsFieldset,
        };
        await formioSetupService.createFormioForm(
          { components: [component] } as FormioForm,
          taakWithSelection([
            { selected: true, titel: document1.titel, uuid: document1.uuid },
          ]),
        );

        expect(ensureQueryDataSpy).toHaveBeenCalledWith(
          expect.objectContaining({
            queryKey: [
              "availableDocumentsQuery",
              taak.zaakUuid,
              [document1.uuid],
            ],
          }),
        );
      });

      it("should use the freshly fetched title instead of the stored one", async () => {
        jest
          .spyOn(testQueryClient, "ensureQueryData")
          .mockResolvedValue([{ uuid: "doc-1", titel: "Renamed Document" }]);

        const component: ExtendedComponentSchema = {
          ...selectedUnsignedDocumentsFieldset,
        };
        await formioSetupService.createFormioForm(
          { components: [component] } as FormioForm,
          taakWithSelection([
            { selected: true, titel: "Stale Title", uuid: "doc-1" },
          ]),
        );

        expect(component.defaultValue).toEqual([
          { selected: true, titel: "Renamed Document", uuid: "doc-1" },
        ]);
      });

      it("should exclude documents that were signed after the selection was made", async () => {
        jest
          .spyOn(testQueryClient, "ensureQueryData")
          .mockResolvedValue([
            { ...document1, ondertekening: signedDocument.ondertekening },
            document2,
          ]);

        const component: ExtendedComponentSchema = {
          ...selectedUnsignedDocumentsFieldset,
        };
        await formioSetupService.createFormioForm(
          { components: [component] } as FormioForm,
          taakWithSelection([
            { selected: true, titel: document1.titel, uuid: document1.uuid },
            { selected: true, titel: document2.titel, uuid: document2.uuid },
          ]),
        );

        expect(component.defaultValue).toEqual([
          { selected: true, titel: document2.titel, uuid: document2.uuid },
        ]);
      });

      it("should default to an empty list when the refreshOn field has no prior data", async () => {
        const ensureQueryDataSpy = jest.spyOn(
          testQueryClient,
          "ensureQueryData",
        );

        const component: ExtendedComponentSchema = {
          ...selectedUnsignedDocumentsFieldset,
        };
        await formioSetupService.createFormioForm(
          { components: [component] } as FormioForm,
          taak,
        );

        expect(component.defaultValue).toEqual([]);
        expect(ensureQueryDataSpy).not.toHaveBeenCalled();
      });

      it("should default to an empty list when nothing was selected", async () => {
        const ensureQueryDataSpy = jest.spyOn(
          testQueryClient,
          "ensureQueryData",
        );

        const component: ExtendedComponentSchema = {
          ...selectedUnsignedDocumentsFieldset,
        };
        await formioSetupService.createFormioForm(
          { components: [component] } as FormioForm,
          taakWithSelection([
            { selected: false, titel: document1.titel, uuid: document1.uuid },
          ]),
        );

        expect(component.defaultValue).toEqual([]);
        expect(ensureQueryDataSpy).not.toHaveBeenCalled();
      });
    },
  );

  describe(FormioSetupService.prototype.setFormioChangeData.name, () => {
    it("should update formioChangeData", async () => {
      const groepComponent: ExtendedComponentSchema = {
        key: "GroepKey",
        type: "select",
        input: true,
        attributes: {
          [ZAC_FIELD_ATTRIBUTE]: KNOWN_ZAC_FIELDS.GROEP,
        },
      };

      const medewerkerComponent: ExtendedComponentSchema = {
        key: "MedewerkerKey",
        type: "select",
        input: true,
        refreshOn: "GroepKey",
        attributes: {
          [ZAC_FIELD_ATTRIBUTE]: KNOWN_ZAC_FIELDS.MEDEWERKER,
        },
      };

      await formioSetupService.createFormioForm(
        {
          components: [groepComponent, medewerkerComponent],
        } as FormioForm,
        taak,
      );

      formioSetupService.setFormioChangeData({ GroepKey: "group-uuid" });
      const queryClientSpy = jest
        .spyOn(testQueryClient, "ensureQueryData")
        .mockResolvedValue([]);

      await medewerkerComponent.data.custom();
      expect(queryClientSpy).toHaveBeenCalledWith(
        expect.objectContaining({
          queryKey: [
            "/rest/identity/groups/{groupId}/users",
            { path: { groupId: "group-uuid" } },
          ],
        }),
      );
    });
  });
});
