/*
 * SPDX-FileCopyrightText: 2025 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

import { ExtendedComponentSchema, FormioForm } from "@formio/angular";
import { testQueryClient } from "../../../../../setupJest";
import { UtilService } from "../../../core/service/util.service";
import { GeneratedType } from "../../../shared/utils/generated-types";
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
  zaak,
  zaakGegevensComponent,
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
        zaak,
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
        zaak,
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
        zaak,
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
        zaak,
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
        zaak,
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
        zaak,
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
        zaak,
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

  describe(`a ${KNOWN_ZAC_FIELDS.ZAAK_GEGEVENS} field`, () => {
    async function render(
      veld: string,
      options?: { label?: string; formaat?: string },
    ) {
      const component = zaakGegevensComponent(veld, options);
      await formioSetupService.createFormioForm(
        { components: [component] },
        taak,
        zaak,
      );
      return component;
    }

    it.each([
      ["uuid", "test-zaakUuid"],
      ["identificatie", "ZAAK-2026-0000000835"],
      ["omschrijving", "test-zaak-omschrijving"],
      ["communicatiekanaal", "Medewerkersportaal"],
      ["zaaktype.uuid", "test-zaaktype-uuid"],
      ["zaaktype.omschrijving", "test-zaaktypeOmschrijving"],
      ["groep.naam", "fakeGroupName"],
      ["behandelaar.naam", "fakeUserName"],
      ["status.naam", "In behandeling"],
    ])("should render the zaak property %s", async (veld, value) => {
      const component = await render(veld);

      expect(component.html).toContain(value);
    });

    it("should separate the label from the value with a colon", async () => {
      const component = await render("identificatie", { label: "Zaaknummer" });

      expect(component.html).toContain("Zaaknummer: ");
      expect(component.label).toBe("");
    });

    it("should not render a stray colon when the field has no label", async () => {
      const component = await render("identificatie", { label: "" });

      expect(component.html).toContain('class="zac-zaak-object-label"></span>');
    });

    it("should render a value as the zaak holds it when no format is asked for", async () => {
      const startdatum = await render("startdatum");
      const isOpgeschort = await render("isOpgeschort");

      expect(startdatum.html).toContain("2026-08-24");
      expect(isOpgeschort.html).toContain("false");
    });

    it.each([
      ["datum", "startdatum", "24-08-2026"],
      ["jaNee", "isOpgeschort", "actie.nee"],
      ["jaNee", "isOpen", "actie.ja"],
    ])(
      "should wrap the %s format function around %s",
      async (formaat, veld, expected) => {
        const component = await render(veld, { formaat });

        expect(component.html).toContain(expected);
      },
    );

    it("should report the available format functions when the requested one does not exist", async () => {
      const component = await render("startdatum", { formaat: "dutchDate" });

      expect(component.html).toContain(
        "Unknown ZAC_FORMAAT &quot;dutchDate&quot;. Available: datum, jaNee",
      );
    });

    it("should mark a property that holds no value, rather than leave the field blank", async () => {
      const component = zaakGegevensComponent("omschrijving");
      await formioSetupService.createFormioForm(
        { components: [component] },
        taak,
        {
          ...zaak,
          omschrijving: undefined,
        } as unknown as GeneratedType<"RestZaak">,
      );

      expect(component.html).toContain(
        '<span class="zac-zaak-object-value">—</span>',
      );
    });

    it("should not become part of the submission, so completing the task cannot write it back", async () => {
      const component = await render("identificatie");

      expect(component.input).toBe(false);
      expect(component.type).toBe("content");
      expect(taak.taakdata).not.toHaveProperty(component.key);
    });

    it("should render nothing for a misspelled property, because the api omits empty ones", async () => {
      const component = await render("kommunikatiekanaal");

      expect(component.html).toContain(
        '<span class="zac-zaak-object-value">—</span>',
      );
    });

    it("should report a path that reads on through a single value, in place of the field", async () => {
      const errorSpy = jest.spyOn(utilService, "handleFormIOInitError");

      const component = await render("identificatie.jaar");

      expect(component.html).toContain(
        "&quot;identificatie&quot; holds a single value",
      );
      expect(component.html).toContain("&quot;jaar&quot; cannot be read");
      expect(errorSpy).not.toHaveBeenCalled();
    });

    it("should say that a path stopping at an object holds no single value", async () => {
      const component = await render("zaaktype");

      expect(component.html).toContain("holds an object, not a single value.");
    });

    it.each([
      [
        "a table cell",
        {
          type: "table",
          key: "tabel",
          input: false,
          rows: [[{ components: [zaakGegevensComponent("identificatie")] }]],
        },
      ],
      [
        "a column",
        {
          type: "columns",
          key: "kolommen",
          input: false,
          columns: [{ components: [zaakGegevensComponent("identificatie")] }],
        },
      ],
    ])("should initialize a field nested in %s", async (_name, layout) => {
      await formioSetupService.createFormioForm(
        { components: [layout as ExtendedComponentSchema] },
        taak,
        zaak,
      );

      const nested = ((
        layout as { rows?: { components: ExtendedComponentSchema[] }[][] }
      ).rows?.[0] ??
        (layout as { columns: { components: ExtendedComponentSchema[] }[] })
          .columns)[0].components[0];
      expect(nested.html).toContain("ZAAK-2026-0000000835");
    });

    it("should join a list of values", async () => {
      const component = await render("indicaties");

      expect(component.html).toContain("OPSCHORTING, VERLENGD");
    });

    it("should read a property of every element of a list", async () => {
      const component = await render("kenmerken[].kenmerk");

      expect(component.html).toContain("fakeKenmerk1, fakeKenmerk2");
    });

    it("should format every element of a list", async () => {
      const component = zaakGegevensComponent("kenmerken[].datum", {
        formaat: "datum",
      });
      await formioSetupService.createFormioForm(
        { components: [component] },
        taak,
        {
          ...zaak,
          kenmerken: [{ datum: "2026-08-24" }, { datum: "2026-01-02" }],
        } as unknown as GeneratedType<"RestZaak">,
      );

      expect(component.html).toContain("24-08-2026, 02-01-2026");
    });

    it("should mark an empty list as holding no value", async () => {
      const component = await render("besluiten[].identificatie");

      expect(component.html).toContain(
        '<span class="zac-zaak-object-value">—</span>',
      );
    });

    it("should suggest a property of the element for a list of objects", async () => {
      const component = await render("kenmerken");

      expect(component.html).toContain("holds a list of objects");
      expect(component.html).toContain("kenmerken[].bron");
    });

    it("should report list syntax used on something that is not a list", async () => {
      const component = await render("identificatie[]");

      expect(component.html).toContain("is not a list");
    });

    it("should escape a zaak value so it cannot inject markup", async () => {
      const component = zaakGegevensComponent("omschrijving");
      await formioSetupService.createFormioForm(
        { components: [component] },
        taak,
        {
          ...zaak,
          omschrijving: "<img src=x onerror=alert(1)>",
        } as unknown as GeneratedType<"RestZaak">,
      );

      expect(component.html).not.toContain("<img");
      expect(component.html).toContain("&lt;img");
    });
  });
});
