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
    [ZAC_FIELD_ATTRIBUTE]: KNOWN_ZAC_FIELDS.DOCUMENTEN_NIET_ONDERTEKEND,
  },
};

const selectedUnsignedDocumentsFieldset: ExtendedComponentSchema = {
  type: "datagrid",
  key: "ZAAK_Documenten_Te_Ondertekenen",
  input: true,
  refreshOn: "ZAAK_Documenten_Ondertekenen_Selectie",
  attributes: {
    [ZAC_FIELD_ATTRIBUTE]: KNOWN_ZAC_FIELDS.GEKOZEN_DOCUMENTEN_NIET_ONDERTEKEND,
  },
};

const regelLinkColumn: ExtendedComponentSchema = {
  type: "htmlelement",
  key: "openen",
  input: false,
  tableView: false,
  attributes: {
    [ZAC_FIELD_ATTRIBUTE]: KNOWN_ZAC_FIELDS.REGEL_LINK,
  },
};

const regelLinkViewIconColumn: ExtendedComponentSchema = {
  type: "htmlelement",
  key: "openen",
  input: false,
  tableView: false,
  attributes: {
    [ZAC_FIELD_ATTRIBUTE]: KNOWN_ZAC_FIELDS.REGEL_LINK_VIEW_ICON,
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
      jest.spyOn(testQueryClient, "fetchQuery").mockResolvedValue([]);

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

      // copies: the spies call through, and the initializers write to the component they are given
      const mockFormComponents: ExtendedComponentSchema[] = [
        { ...groepComponent },
        { ...medewerkerComponent },
        { ...referenceTableFieldset },
        { ...documentsFieldset },
        { ...smartDocumentsTemplateGroupsComponent },
        { ...smartDocumentsTemplateGroupTemplatesComponent },
        { ...unsignedDocumentsFieldset },
        { ...selectedUnsignedDocumentsFieldset },
      ];

      await formioSetupService.createFormioForm(
        { components: mockFormComponents } as FormioForm,
        taak,
      );

      expect(groepSpy).toHaveBeenCalledWith(mockFormComponents[0], taak);
      expect(medewerkerSpy).toHaveBeenCalledWith(mockFormComponents[1]);
      expect(referenceTableSpy).toHaveBeenCalledWith(mockFormComponents[2]);
      expect(availableDocumentsSpy).toHaveBeenCalledWith(
        mockFormComponents[3],
        taak,
      );
      expect(templateGroupsSpy).toHaveBeenCalledWith(
        mockFormComponents[4],
        taak,
      );
      expect(templateGroupTemplatesSpy).toHaveBeenCalledWith(
        mockFormComponents[5],
        taak,
      );
      expect(unsignedDocumentsSpy).toHaveBeenCalledWith(
        mockFormComponents[6],
        taak,
      );
      expect(selectedUnsignedDocumentsSpy).toHaveBeenCalledWith(
        mockFormComponents[7],
        taak,
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

    it("should render a red bordered message when a field carries an unknown ZAC_TYPE", async () => {
      const unknownComponent: ExtendedComponentSchema = {
        key: "ZAAK_Documenten_Typo",
        type: "select",
        attributes: { [ZAC_FIELD_ATTRIBUTE]: "ZAC_documentn" },
      };

      await formioSetupService.createFormioForm(
        { components: [unknownComponent] } as FormioForm,
        taak,
      );

      expect(unknownComponent.type).toBe("content");
      expect(unknownComponent.input).toBe(false);
      expect(unknownComponent.html).toContain(
        "Undefined ZAC_TYPE: 'ZAC_documentn'",
      );
      expect(unknownComponent.html).toContain("border: 1px solid red");
      expect(unknownComponent.html).toContain("color: red");
    });

    it("should leave a plain Form.io component without a ZAC_TYPE untouched", async () => {
      const components: ExtendedComponentSchema[] = [
        { key: "toelichting", type: "textfield" },
        { key: "submit", type: "button" },
      ];

      await formioSetupService.createFormioForm(
        { components } as FormioForm,
        taak,
      );

      expect(components).toEqual([
        { key: "toelichting", type: "textfield" },
        { key: "submit", type: "button" },
      ]);
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
        const fetchQuerySpy = jest
          .spyOn(testQueryClient, "fetchQuery")
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
        expect(fetchQuerySpy).toHaveBeenCalledWith(
          expect.objectContaining({
            queryKey: ["availableDocumentsQuery", taak.zaakUuid, undefined],
          }),
        );
      });

      it("should not filter out already-signed documents", async () => {
        jest
          .spyOn(testQueryClient, "fetchQuery")
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
      "initializeRowLinkColumn"
    ].name,
    () => {
      /** A link column takes its route from the grid it sits in, so it is always tested nested. */
      const initializeLinkInGrid = async (
        grid: ExtendedComponentSchema,
        column: ExtendedComponentSchema,
      ) => {
        jest
          .spyOn(testQueryClient, "fetchQuery")
          .mockResolvedValue([document1]);

        await formioSetupService.createFormioForm(
          { components: [{ ...grid, components: [column] }] } as FormioForm,
          {
            ...taak,
            taakdata: {
              ZAAK_Documenten_Ondertekenen_Selectie: [
                {
                  selected: true,
                  titel: document1.titel,
                  uuid: document1.uuid,
                },
              ],
            },
          },
        );
      };

      it("should link to the document with the zaak uuid of the task filled in", async () => {
        const column: ExtendedComponentSchema = { ...regelLinkColumn };

        await initializeLinkInGrid(unsignedDocumentsFieldset, column);

        expect(column.attrs).toEqual([
          {
            attr: "href",
            // the row uuid stays a template: only Form.io can resolve it, per row
            value: `/informatie-objecten/{{ row.uuid }}/${taak.zaakUuid}`,
          },
          { attr: "target", value: "_blank" },
          { attr: "rel", value: "noopener noreferrer" },
        ]);
      });

      it("should take the route from the grid holding the column, whichever grid that is", async () => {
        const column: ExtendedComponentSchema = { ...regelLinkColumn };

        await initializeLinkInGrid(selectedUnsignedDocumentsFieldset, column);

        expect(column.attrs).toEqual(
          expect.arrayContaining([
            {
              attr: "href",
              value: `/informatie-objecten/{{ row.uuid }}/${taak.zaakUuid}`,
            },
          ]),
        );
      });

      it("should render an anchor with translated link text", async () => {
        const column: ExtendedComponentSchema = { ...regelLinkColumn };

        await initializeLinkInGrid(unsignedDocumentsFieldset, column);

        expect(column.tag).toBe("a");
        expect(column.content).toBe("actie.document.openen-nieuw-tabblad");
      });

      it("should leave the tag and content defined by the form author untouched", async () => {
        const column: ExtendedComponentSchema = {
          ...regelLinkColumn,
          tag: "button",
          content: "Set by the form author",
        };

        await initializeLinkInGrid(unsignedDocumentsFieldset, column);

        expect(column.tag).toBe("button");
        expect(column.content).toBe("Set by the form author");
      });

      it("should leave attrs defined by the form author untouched", async () => {
        const authorAttrs = [
          { attr: "href", value: "/somewhere/else/{{ row.uuid }}" },
        ];
        const column: ExtendedComponentSchema = {
          ...regelLinkColumn,
          attrs: authorAttrs,
        };

        await initializeLinkInGrid(unsignedDocumentsFieldset, column);

        expect(column.attrs).toEqual(authorAttrs);
      });

      it("should render the view icon instead of link text, with the text as accessible name", async () => {
        const column: ExtendedComponentSchema = {
          ...regelLinkViewIconColumn,
        };

        await initializeLinkInGrid(unsignedDocumentsFieldset, column);

        expect(column.tag).toBe("a");
        expect(column.content).toBe(
          '<span class="material-symbols-outlined">visibility</span>',
        );
        expect(column.attrs).toEqual([
          {
            attr: "href",
            value: `/informatie-objecten/{{ row.uuid }}/${taak.zaakUuid}`,
          },
          { attr: "target", value: "_blank" },
          { attr: "rel", value: "noopener noreferrer" },
          {
            attr: "aria-label",
            value: "actie.document.openen-nieuw-tabblad",
          },
          { attr: "title", value: "actie.document.openen-nieuw-tabblad" },
        ]);
      });

      it("should leave the icon content defined by the form author untouched", async () => {
        const column: ExtendedComponentSchema = {
          ...regelLinkViewIconColumn,
          content: "Set by the form author",
        };

        await initializeLinkInGrid(unsignedDocumentsFieldset, column);

        expect(column.content).toBe("Set by the form author");
      });

      it("should report an icon link column that sits outside a grid with a registered route", async () => {
        const handleFormIOInitErrorSpy = jest.spyOn(
          utilService,
          "handleFormIOInitError",
        );
        const column: ExtendedComponentSchema = {
          ...regelLinkViewIconColumn,
        };

        await formioSetupService.createFormioForm(
          { components: [column] } as FormioForm,
          taak,
        );

        expect(column.attrs).toBeUndefined();
        expect(handleFormIOInitErrorSpy).toHaveBeenCalledWith(
          KNOWN_ZAC_FIELDS.REGEL_LINK_VIEW_ICON,
          expect.stringContaining(
            `A ${KNOWN_ZAC_FIELDS.REGEL_LINK_VIEW_ICON} column takes its route`,
          ),
        );
      });

      it("should report a link column that sits outside a grid with a registered route", async () => {
        const handleFormIOInitErrorSpy = jest.spyOn(
          utilService,
          "handleFormIOInitError",
        );
        const column: ExtendedComponentSchema = { ...regelLinkColumn };

        await formioSetupService.createFormioForm(
          { components: [column] } as FormioForm,
          taak,
        );

        expect(column.attrs).toBeUndefined();
        expect(handleFormIOInitErrorSpy).toHaveBeenCalledWith(
          KNOWN_ZAC_FIELDS.REGEL_LINK,
          expect.stringContaining(
            'No row link registered for parent "undefined"',
          ),
        );
      });
    },
  );

  describe(
    (FormioSetupService.prototype as unknown as Record<string, () => unknown>)[
      "initializeUnsignedDocumentsDatagrid"
    ].name,
    () => {
      /** Runs the expression the way Form.io does: the value as `input`, reading back `valid`. */
      const runCustomValidation = (
        custom: string,
        input: { selected: boolean }[],
      ) =>
        new Function("input", `let valid = true; ${custom}; return valid;`)(
          input,
        ) as boolean | string;

      it("should reject a grid without a single row ticked, so the submit button stays disabled", async () => {
        jest
          .spyOn(testQueryClient, "fetchQuery")
          .mockResolvedValue([document1, document2]);

        const component: ExtendedComponentSchema = {
          ...unsignedDocumentsFieldset,
        };
        await formioSetupService.createFormioForm(
          { components: [component] } as FormioForm,
          taak,
        );

        expect(
          runCustomValidation(component.validate.custom, [
            { selected: false },
            { selected: false },
          ]),
        ).toBe("msg.selecteer-minimaal-een-document");
      });

      it("should accept a grid with at least one row ticked", async () => {
        jest
          .spyOn(testQueryClient, "fetchQuery")
          .mockResolvedValue([document1, document2]);

        const component: ExtendedComponentSchema = {
          ...unsignedDocumentsFieldset,
        };
        await formioSetupService.createFormioForm(
          { components: [component] } as FormioForm,
          taak,
        );

        expect(
          runCustomValidation(component.validate.custom, [
            { selected: false },
            { selected: true },
          ]),
        ).toBe(true);
      });

      it("should keep the required rule the form author set alongside the custom one", async () => {
        jest
          .spyOn(testQueryClient, "fetchQuery")
          .mockResolvedValue([document1]);

        const component: ExtendedComponentSchema = {
          ...unsignedDocumentsFieldset,
          validate: { required: true },
        };
        await formioSetupService.createFormioForm(
          { components: [component] } as FormioForm,
          taak,
        );

        expect(component.validate.required).toBe(true);
        expect(component.validate.custom).toContain("row.selected");
      });

      it("should leave a custom validation defined by the form author untouched", async () => {
        jest
          .spyOn(testQueryClient, "fetchQuery")
          .mockResolvedValue([document1]);

        const component: ExtendedComponentSchema = {
          ...unsignedDocumentsFieldset,
          validate: { custom: "valid = true" },
        };
        await formioSetupService.createFormioForm(
          { components: [component] } as FormioForm,
          taak,
        );

        expect(component.validate.custom).toBe("valid = true");
      });

      it("should populate the datagrid with all zaak documents, unselected", async () => {
        jest
          .spyOn(testQueryClient, "fetchQuery")
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

      it("should write the rows into the task data, which Form.io prefers over defaultValue", async () => {
        jest
          .spyOn(testQueryClient, "fetchQuery")
          .mockResolvedValue([document1]);

        const component: ExtendedComponentSchema = {
          ...unsignedDocumentsFieldset,
        };
        const taakWithExistingKey: GeneratedType<"RestTask"> = {
          ...taak,
          taakdata: { ZAAK_Documenten_Ondertekenen_Selectie: [] },
        };

        await formioSetupService.createFormioForm(
          { components: [component] } as FormioForm,
          taakWithExistingKey,
        );

        expect(
          taakWithExistingKey.taakdata?.ZAAK_Documenten_Ondertekenen_Selectie,
        ).toEqual([
          { selected: false, titel: document1.titel, uuid: document1.uuid },
        ]);
      });

      it("should keep a previously made selection when the task is reopened", async () => {
        jest
          .spyOn(testQueryClient, "fetchQuery")
          .mockResolvedValue([document1, document2]);

        const component: ExtendedComponentSchema = {
          ...unsignedDocumentsFieldset,
        };
        await formioSetupService.createFormioForm(
          { components: [component] } as FormioForm,
          {
            ...taak,
            taakdata: {
              ZAAK_Documenten_Ondertekenen_Selectie: [
                {
                  selected: true,
                  titel: document2.titel,
                  uuid: document2.uuid,
                },
              ],
            },
          },
        );

        expect(component.defaultValue).toEqual([
          { selected: false, titel: document1.titel, uuid: document1.uuid },
          { selected: true, titel: document2.titel, uuid: document2.uuid },
        ]);
      });

      it("should render no rows at all when every document is already signed", async () => {
        jest
          .spyOn(testQueryClient, "fetchQuery")
          .mockResolvedValue([signedDocument]);

        const component: ExtendedComponentSchema = {
          ...unsignedDocumentsFieldset,
        };
        await formioSetupService.createFormioForm(
          { components: [component] } as FormioForm,
          taak,
        );

        expect(component.defaultValue).toEqual([]);
        // Form.io renders a single blank row for an empty datagrid unless initEmpty is set
        expect(component.initEmpty).toBe(true);
      });

      it("should hide the table and explain why when there is nothing to sign", async () => {
        jest
          .spyOn(testQueryClient, "fetchQuery")
          .mockResolvedValue([signedDocument]);

        const component: ExtendedComponentSchema = {
          ...unsignedDocumentsFieldset,
        };
        await formioSetupService.createFormioForm(
          { components: [component] } as FormioForm,
          taak,
        );

        expect(component.customClass).toBe("zac-empty-input-field");
        expect(component.description).toBe(
          "msg.geen-documenten-te-ondertekenen",
        );
      });

      it("should show the table without a message when there is something to sign", async () => {
        jest
          .spyOn(testQueryClient, "fetchQuery")
          .mockResolvedValue([document1]);

        const component: ExtendedComponentSchema = {
          ...unsignedDocumentsFieldset,
        };
        await formioSetupService.createFormioForm(
          { components: [component] } as FormioForm,
          taak,
        );

        expect(component.customClass).toBe("");
        expect(component.description).toBe("");
      });

      it("should keep the class and description set by the form author", async () => {
        jest
          .spyOn(testQueryClient, "fetchQuery")
          .mockResolvedValue([signedDocument]);

        const component: ExtendedComponentSchema = {
          ...unsignedDocumentsFieldset,
          customClass: "author-class",
          description: "Set by the form author",
        };
        await formioSetupService.createFormioForm(
          { components: [component] } as FormioForm,
          taak,
        );

        expect(component.customClass).toBe(
          "author-class zac-empty-input-field",
        );
      });

      it("should restore the description set by the form author once the grid fills up", async () => {
        jest
          .spyOn(testQueryClient, "fetchQuery")
          .mockResolvedValue([document1]);

        const component: ExtendedComponentSchema = {
          ...unsignedDocumentsFieldset,
          customClass: "author-class",
          description: "Set by the form author",
        };
        await formioSetupService.createFormioForm(
          { components: [component] } as FormioForm,
          taak,
        );

        expect(component.customClass).toBe("author-class");
        expect(component.description).toBe("Set by the form author");
      });

      it("should not accumulate the marker class when the task is initialized twice", async () => {
        jest
          .spyOn(testQueryClient, "fetchQuery")
          .mockResolvedValue([signedDocument]);

        const component: ExtendedComponentSchema = {
          ...unsignedDocumentsFieldset,
        };
        const form = { components: [component] } as FormioForm;
        await formioSetupService.createFormioForm(form, taak);
        await formioSetupService.createFormioForm(form, taak);

        expect(component.customClass).toBe("zac-empty-input-field");
        expect(component.description).toBe(
          "msg.geen-documenten-te-ondertekenen",
        );
      });

      it("should exclude already-signed documents", async () => {
        jest
          .spyOn(testQueryClient, "fetchQuery")
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

      it("should show only the previously selected documents, unticked", async () => {
        jest
          .spyOn(testQueryClient, "fetchQuery")
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
          { selected: false, titel: document1.titel, uuid: document1.uuid },
        ]);
      });

      it("should re-fetch only the selected documents, keyed on their uuids", async () => {
        const fetchQuerySpy = jest
          .spyOn(testQueryClient, "fetchQuery")
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

        expect(fetchQuerySpy).toHaveBeenCalledWith(
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
          .spyOn(testQueryClient, "fetchQuery")
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
          { selected: false, titel: "Renamed Document", uuid: "doc-1" },
        ]);
      });

      it("should exclude documents that were signed after the selection was made", async () => {
        jest
          .spyOn(testQueryClient, "fetchQuery")
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
          { selected: false, titel: document2.titel, uuid: document2.uuid },
        ]);
      });

      it("should hide the table and explain why when every selected document was signed in the meantime", async () => {
        jest
          .spyOn(testQueryClient, "fetchQuery")
          .mockResolvedValue([
            { ...document1, ondertekening: signedDocument.ondertekening },
          ]);

        const component: ExtendedComponentSchema = {
          ...selectedUnsignedDocumentsFieldset,
        };
        await formioSetupService.createFormioForm(
          { components: [component] } as FormioForm,
          taakWithSelection([
            { selected: true, titel: document1.titel, uuid: document1.uuid },
          ]),
        );

        expect(component.defaultValue).toEqual([]);
        expect(component.customClass).toBe("zac-empty-input-field");
        expect(component.description).toBe(
          "msg.geen-documenten-te-ondertekenen",
        );
      });

      it("should default to an empty list when the refreshOn field has no prior data", async () => {
        const fetchQuerySpy = jest.spyOn(testQueryClient, "fetchQuery");

        const component: ExtendedComponentSchema = {
          ...selectedUnsignedDocumentsFieldset,
        };
        await formioSetupService.createFormioForm(
          { components: [component] } as FormioForm,
          taak,
        );

        expect(component.defaultValue).toEqual([]);
        expect(fetchQuerySpy).not.toHaveBeenCalled();
      });

      it("should default to an empty list when nothing was selected", async () => {
        const fetchQuerySpy = jest.spyOn(testQueryClient, "fetchQuery");

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
        expect(fetchQuerySpy).not.toHaveBeenCalled();
      });
    },
  );

  describe("initializing two tasks", () => {
    const otherTaak: GeneratedType<"RestTask"> = {
      ...taak,
      id: "other-id",
      zaakUuid: "other-zaakUuid",
      taakdata: {},
    };

    it("should give each form the documents and links of its own task, whichever finishes first", async () => {
      const documentsPerZaak: Record<string, (typeof document1)[]> = {
        [taak.zaakUuid]: [document1],
        [otherTaak.zaakUuid]: [document2],
      };
      const resolvers: (() => void)[] = [];
      jest.spyOn(testQueryClient, "fetchQuery").mockImplementation(
        ((options: { queryKey: [string, string, string[] | undefined] }) =>
          new Promise<(typeof document1)[]>((resolve) =>
            // held back so both setups are in flight at once, then released in reverse order
            resolvers.push(() =>
              resolve(documentsPerZaak[options.queryKey[1]]),
            ),
          )) as typeof testQueryClient.fetchQuery,
      );

      const column: ExtendedComponentSchema = { ...regelLinkColumn };
      const otherColumn: ExtendedComponentSchema = { ...regelLinkColumn };
      const grid: ExtendedComponentSchema = {
        ...unsignedDocumentsFieldset,
        components: [column],
      };
      const otherGrid: ExtendedComponentSchema = {
        ...unsignedDocumentsFieldset,
        components: [otherColumn],
      };

      const initializations = Promise.all([
        formioSetupService.createFormioForm(
          { components: [grid] } as FormioForm,
          taak,
        ),
        formioSetupService.createFormioForm(
          { components: [otherGrid] } as FormioForm,
          otherTaak,
        ),
      ]);
      while (resolvers.length < 2) await Promise.resolve();
      resolvers.reverse().forEach((resolve) => resolve());
      await initializations;

      expect(grid.defaultValue).toEqual([
        { selected: false, titel: document1.titel, uuid: document1.uuid },
      ]);
      expect(otherGrid.defaultValue).toEqual([
        { selected: false, titel: document2.titel, uuid: document2.uuid },
      ]);
      expect(column.attrs).toEqual(
        expect.arrayContaining([
          {
            attr: "href",
            value: `/informatie-objecten/{{ row.uuid }}/${taak.zaakUuid}`,
          },
        ]),
      );
      expect(otherColumn.attrs).toEqual(
        expect.arrayContaining([
          {
            attr: "href",
            value: `/informatie-objecten/{{ row.uuid }}/${otherTaak.zaakUuid}`,
          },
        ]),
      );
    });

    it("should fetch for its own task from a data source called after another task was initialized", async () => {
      const fetchQuerySpy = jest
        .spyOn(testQueryClient, "fetchQuery")
        .mockResolvedValue([document1]);
      const component: ExtendedComponentSchema = { ...documentsFieldset };

      await formioSetupService.createFormioForm(
        { components: [component] } as FormioForm,
        taak,
      );
      await formioSetupService.createFormioForm(
        { components: [{ ...documentsFieldset }] } as FormioForm,
        otherTaak,
      );

      // Form.io calls the data source on render and refresh, long after setup
      fetchQuerySpy.mockClear();
      await component.data.custom();

      expect(fetchQuerySpy).toHaveBeenCalledWith(
        expect.objectContaining({
          queryKey: ["availableDocumentsQuery", taak.zaakUuid, undefined],
        }),
      );
    });
  });

  describe("fetching the zaak documents", () => {
    it("should always refetch, so a cached list cannot offer documents that have since been signed or unlinked", async () => {
      const fetchQuerySpy = jest
        .spyOn(testQueryClient, "fetchQuery")
        .mockResolvedValue([document1]);
      const component: ExtendedComponentSchema = {
        ...unsignedDocumentsFieldset,
      };

      await formioSetupService.createFormioForm(
        { components: [component] } as FormioForm,
        taak,
      );

      expect(fetchQuerySpy).toHaveBeenCalledWith(
        expect.objectContaining({ staleTime: 0 }),
      );
    });
  });

  describe("a finished task", () => {
    const storedRows = [
      {
        selected: true,
        titel: signedDocument.titel,
        uuid: signedDocument.uuid,
      },
      { selected: false, titel: document1.titel, uuid: document1.uuid },
    ];

    const afgerondTaak = (
      taakdata: GeneratedType<"RestTask">["taakdata"],
    ): GeneratedType<"RestTask"> => ({
      ...taak,
      status: "AFGEROND",
      taakdata,
    });

    it("should show the stored rows of the selection grid, ticks and signed documents included", async () => {
      const fetchQuerySpy = jest.spyOn(testQueryClient, "fetchQuery");
      const component: ExtendedComponentSchema = {
        ...unsignedDocumentsFieldset,
      };

      await formioSetupService.createFormioForm(
        { components: [component] } as FormioForm,
        afgerondTaak({ ZAAK_Documenten_Ondertekenen_Selectie: storedRows }),
      );

      expect(component.defaultValue).toEqual(storedRows);
      expect(fetchQuerySpy).not.toHaveBeenCalled();
    });

    it("should tick a document this task submitted that carries a signature now", async () => {
      jest
        .spyOn(testQueryClient, "fetchQuery")
        .mockResolvedValue([signedDocument, document1]);
      const component: ExtendedComponentSchema = {
        ...selectedUnsignedDocumentsFieldset,
      };
      const finishedTaak = afgerondTaak({
        ZAAK_Documenten_Te_Ondertekenen: [
          {
            selected: true,
            titel: signedDocument.titel,
            uuid: signedDocument.uuid,
          },
          { selected: true, titel: document1.titel, uuid: document1.uuid },
        ],
      });

      await formioSetupService.createFormioForm(
        { components: [component] } as FormioForm,
        finishedTaak,
      );

      const rows = [
        {
          selected: true,
          titel: signedDocument.titel,
          uuid: signedDocument.uuid,
        },
        { selected: false, titel: document1.titel, uuid: document1.uuid },
      ];
      expect(component.defaultValue).toEqual(rows);
      expect(finishedTaak.taakdata?.ZAAK_Documenten_Te_Ondertekenen).toEqual(
        rows,
      );
    });

    it("should leave a document this task did not submit unticked, however it was signed since", async () => {
      jest
        .spyOn(testQueryClient, "fetchQuery")
        .mockResolvedValue([signedDocument]);
      const component: ExtendedComponentSchema = {
        ...selectedUnsignedDocumentsFieldset,
      };

      await formioSetupService.createFormioForm(
        { components: [component] } as FormioForm,
        afgerondTaak({
          ZAAK_Documenten_Te_Ondertekenen: [
            {
              selected: false,
              titel: signedDocument.titel,
              uuid: signedDocument.uuid,
            },
          ],
        }),
      );

      expect(component.defaultValue).toEqual([
        {
          selected: false,
          titel: signedDocument.titel,
          uuid: signedDocument.uuid,
        },
      ]);
    });

    it("should show the signing grid with the current titles", async () => {
      jest
        .spyOn(testQueryClient, "fetchQuery")
        .mockResolvedValue([{ ...signedDocument, titel: "Renamed Document" }]);
      const component: ExtendedComponentSchema = {
        ...selectedUnsignedDocumentsFieldset,
      };

      await formioSetupService.createFormioForm(
        { components: [component] } as FormioForm,
        afgerondTaak({
          ZAAK_Documenten_Te_Ondertekenen: [
            {
              selected: true,
              titel: "Stale Title",
              uuid: signedDocument.uuid,
            },
          ],
        }),
      );

      expect(component.defaultValue).toEqual([
        {
          selected: true,
          titel: "Renamed Document",
          uuid: signedDocument.uuid,
        },
      ]);
    });

    it("should keep a document that was removed since, under the title it was submitted with", async () => {
      jest.spyOn(testQueryClient, "fetchQuery").mockResolvedValue([]);
      const component: ExtendedComponentSchema = {
        ...selectedUnsignedDocumentsFieldset,
      };

      await formioSetupService.createFormioForm(
        { components: [component] } as FormioForm,
        afgerondTaak({
          ZAAK_Documenten_Te_Ondertekenen: [
            { selected: true, titel: document1.titel, uuid: document1.uuid },
          ],
        }),
      );

      expect(component.defaultValue).toEqual([
        { selected: false, titel: document1.titel, uuid: document1.uuid },
      ]);
    });

    it("should leave the stored task data untouched", async () => {
      const component: ExtendedComponentSchema = {
        ...unsignedDocumentsFieldset,
      };
      const finishedTaak = afgerondTaak({
        ZAAK_Documenten_Ondertekenen_Selectie: storedRows,
      });

      await formioSetupService.createFormioForm(
        { components: [component] } as FormioForm,
        finishedTaak,
      );

      expect(
        finishedTaak.taakdata?.ZAAK_Documenten_Ondertekenen_Selectie,
      ).toEqual(storedRows);
    });

    it("should not validate a grid that can no longer be filled in", async () => {
      const component: ExtendedComponentSchema = {
        ...unsignedDocumentsFieldset,
      };

      await formioSetupService.createFormioForm(
        { components: [component] } as FormioForm,
        afgerondTaak({ ZAAK_Documenten_Ondertekenen_Selectie: storedRows }),
      );

      expect(component.validate?.custom).toBeUndefined();
    });

    it("should explain an empty grid rather than render a bare frame of headers", async () => {
      const component: ExtendedComponentSchema = {
        ...unsignedDocumentsFieldset,
      };

      await formioSetupService.createFormioForm(
        { components: [component] } as FormioForm,
        afgerondTaak({ ZAAK_Documenten_Ondertekenen_Selectie: [] }),
      );

      expect(component.defaultValue).toEqual([]);
      expect(component.initEmpty).toBe(true);
      expect(component.customClass).toBe("zac-empty-input-field");
      expect(component.description).toBe("msg.geen-documenten-te-ondertekenen");
    });
  });

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
