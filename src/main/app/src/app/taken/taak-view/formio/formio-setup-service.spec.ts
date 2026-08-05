/*
 * SPDX-FileCopyrightText: 2025 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

import { ExtendedComponentSchema, FormioForm } from "@formio/angular";
import { testQueryClient } from "../../../../../setupJest";
import { UtilService } from "../../../core/service/util.service";
import {
  FormioSetupService,
  KNOWN_ZAC_FIELDS,
  ZAC_FIELD_ATTRIBUTE,
} from "./formio-setup-service";
import {
  configureFormioSetupServiceTestBed,
  documentsFieldset,
  groepComponent,
  medewerkerComponent,
  referenceTableFieldset,
  selectedUnsignedDocumentsFieldset,
  smartDocumentsTemplateGroupsComponent,
  smartDocumentsTemplateGroupTemplatesComponent,
  taak,
  unsignedDocumentsFieldset,
} from "./formio-setup-service.test-fixtures";

describe(FormioSetupService.name, () => {
  let formioSetupService: FormioSetupService;
  let utilService: UtilService;

  beforeEach(() => {
    ({ formioSetupService, utilService } =
      configureFormioSetupServiceTestBed());
  });

  // `testQueryClient` is shared by every test here, and the global `clearAllMocks` leaves
  // spy implementations in place: without this, one test's mocked documents feed the next.
  afterEach(() => jest.restoreAllMocks());

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

    it("should report no error for components with no children or properties", async () => {
      const handleFormIOInitErrorSpy = jest.spyOn(
        utilService,
        "handleFormIOInitError",
      );
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

      expect(handleFormIOInitErrorSpy).not.toHaveBeenCalled();
    });

    it("should render a message when a field carries an unknown ZAC_TYPE", async () => {
      const unknownComponent: ExtendedComponentSchema = {
        key: "ZAAK_Documenten_Typo",
        type: "select",
        attributes: { [ZAC_FIELD_ATTRIBUTE]: "ZAC_documentn" },
      };

      await formioSetupService.createFormioForm(
        { components: [unknownComponent] } as FormioForm,
        taak,
      );

      expect(unknownComponent.html).toContain('class="zac-unknown-zac-type"');
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
