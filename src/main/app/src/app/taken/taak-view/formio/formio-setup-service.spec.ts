/*
 * SPDX-FileCopyrightText: 2025 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

import { ExtendedComponentSchema, FormioForm } from "@formio/angular";
import { testQueryClient } from "../../../../../setupJest";
import { UtilService } from "../../../core/service/util.service";
import { GeneratedType } from "../../../shared/utils/generated-types";

const NO_VALUE_MARK = "\u2014";
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
  gegevensInputComponent,
  taakGegevensComponent,
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

  describe(`a gegevens field with ${"ZAC_INVOER"}`, () => {
    async function seed(
      component: ExtendedComponentSchema,
      taakdata: Record<string, unknown> = {},
    ) {
      const seededTaak = {
        ...taak,
        taakdata,
        groep: { id: "fakeGroupId", naam: "fakeGroupName" },
      } as GeneratedType<"RestTask">;
      await formioSetupService.createFormioForm(
        { components: [component] },
        seededTaak,
        zaak,
      );
      return { component, taakdata: seededTaak.taakdata };
    }

    it("should leave the field editable instead of rendering it as text", async () => {
      const { component } = await seed(gegevensInputComponent("identificatie"));

      expect(component.type).toBe("textfield");
      expect(component.input).toBe(true);
      expect(component.html).toBeUndefined();
      expect(component.defaultValue).toBe("ZAAK-2026-0000000835");
    });

    it("should put the value in the task data, because Form.io prefers that over a default", async () => {
      const { component, taakdata } = await seed(
        gegevensInputComponent("identificatie"),
      );

      expect(taakdata![component.key]).toBe("ZAAK-2026-0000000835");
    });

    it("should not overwrite an answer the user already stored", async () => {
      const { component, taakdata } = await seed(
        gegevensInputComponent("identificatie"),
        { IN_Seeded: "edited by the user" },
      );

      expect(taakdata![component.key]).toBe("edited by the user");
      expect(component.defaultValue).toBe("ZAAK-2026-0000000835");
    });

    it("should apply the format function to the seeded value", async () => {
      const { component } = await seed(
        gegevensInputComponent("startdatum", { formaat: "datum" }),
      );

      expect(component.defaultValue).toBe("24-08-2026");
    });

    it("should seed from the taak as well as from the zaak", async () => {
      const { component } = await seed(
        gegevensInputComponent("groep.naam", {
          zacType: KNOWN_ZAC_FIELDS.TAAK_GEGEVENS,
        }),
      );

      expect(component.defaultValue).toBe("fakeGroupName");
    });

    it.each([
      ["an absent property", "einddatum"],
      ["a misspelled property", "eindatum"],
      ["an empty list", "besluiten[].identificatie"],
    ])("should leave the input untouched for %s", async (_name, veld) => {
      const { component, taakdata } = await seed(gegevensInputComponent(veld));

      expect(component.defaultValue).toBeUndefined();
      expect(taakdata).toEqual({});
    });

    it("should never seed the no-value marker, which a date picker cannot parse", async () => {
      const { component, taakdata } = await seed(
        gegevensInputComponent("einddatum", { formaat: "datum" }),
      );

      expect(component.defaultValue).not.toBe("\u2014");
      expect(JSON.stringify(taakdata)).not.toContain("\u2014");
    });

    it("should refuse a table format, which cannot go into an input", async () => {
      const { component } = await seed(
        gegevensInputComponent("zaakdata", { formaat: "tabel" }),
      );

      expect(component.html).toContain("cannot be put into an input");
    });

    it.each([
      ["a datetime field", { type: "datetime" }],
      ["a field carrying a calendar widget", { widget: { type: "calendar" } }],
    ])(
      "should refuse a format on %s, which parses its own value",
      async (_name, shape) => {
        const { component } = await seed({
          ...gegevensInputComponent("startdatum", { formaat: "datum" }),
          ...shape,
        });

        expect(component.html).toContain("parses its own value");
        expect(component.defaultValue).toBeUndefined();
      },
    );

    it("should seed a date picker with the raw value", async () => {
      const { component } = await seed({
        ...gegevensInputComponent("startdatum"),
        type: "datetime",
      });

      expect(component.defaultValue).toBe("2026-08-24");
    });

    it("should report an unresolvable path in place of the field", async () => {
      const { component } = await seed(
        gegevensInputComponent("identificatie.jaar"),
      );

      expect(component.html).toContain("holds a single value");
    });
  });

  describe(`a ${KNOWN_ZAC_FIELDS.TAAK_GEGEVENS} field`, () => {
    async function render(veld: string, options?: { formaat?: string }) {
      const component = taakGegevensComponent(veld, options);
      await formioSetupService.createFormioForm(
        { components: [component] },
        {
          ...taak,
          groep: { id: "fakeGroupId", naam: "fakeGroupName" },
          behandelaar: { id: "fakeUserId", naam: "fakeUserName" },
        },
        zaak,
      );
      return component;
    }

    it.each([
      ["naam", "test-taak"],
      ["status", "TOEGEKEND"],
      ["groep.naam", "fakeGroupName"],
      ["behandelaar.naam", "fakeUserName"],
      ["behandelaar.id", "fakeUserId"],
    ])("should render the taak property %s", async (veld, value) => {
      const component = await render(veld);

      expect(component.html).toContain(value);
    });

    it("should read the taak, not the zaak, for a property both objects have", async () => {
      const component = await render("zaaktypeOmschrijving");

      expect(component.html).toContain("test-zaaktypeOmschrijving");
    });

    it("should name the taak as the source in an error", async () => {
      const component = await render("naam.eerste");

      expect(component.html).toContain("Taak property &quot;naam&quot;");
    });

    it("should format a taak date with the shared format function", async () => {
      const component = await render("fataledatum", { formaat: "datum" });

      expect(component.html).toMatch(/\d{2}-\d{2}-\d{4}/);
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

      expect(component.html).toContain('class="zac-gegevens-label"></span>');
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
        '<span class="zac-gegevens-value">—</span>',
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
        '<span class="zac-gegevens-value">—</span>',
      );
    });

    it("should report a path that reads on through a single value, in place of the field", async () => {
      const errorSpy = jest.spyOn(utilService, "handleFormIOInitError");

      const component = await render("identificatie.jaar");

      expect(component.html).toContain(
        "Zaak property &quot;identificatie&quot; holds a single value",
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
        '<span class="zac-gegevens-value">—</span>',
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

    describe("the tabel format", () => {
      async function renderTable(veld: string, source?: object) {
        const component = zaakGegevensComponent(veld, { formaat: "tabel" });
        await formioSetupService.createFormioForm(
          { components: [component] },
          taak,
          (source ?? zaak) as GeneratedType<"RestZaak">,
        );
        return component;
      }

      it("should render every key of an object as a row", async () => {
        const component = await renderTable("zaakdata", {
          ...zaak,
          zaakdata: { NF_Uren: "8", TA_Toelichting: "spoed" },
        });

        expect(component.html).toContain("<td>8</td>");
        expect(component.html).toContain("<td>spoed</td>");
        expect(component.html).toContain("<code>NF_Uren</code>");
      });

      it("should sort the keys so the same object always reads the same way", async () => {
        const component = await renderTable("zaakdata", {
          ...zaak,
          zaakdata: { zebra: "1", alpha: "2" },
        });

        expect(component.html!.indexOf("alpha")).toBeLessThan(
          component.html!.indexOf("zebra"),
        );
      });

      it("should count the entries of a nested object and the elements of a list", async () => {
        const component = await renderTable("zaakdata", {
          ...zaak,
          zaakdata: { rows: [1, 2, 3], nested: { a: 1, b: 2 } },
        });

        expect(component.html).toContain("<td>[3]</td>");
        expect(component.html).toContain("<td>{2}</td>");
      });

      it("should mark an object with no keys as holding no value", async () => {
        const component = await renderTable("zaakdata", {
          ...zaak,
          zaakdata: {},
        });

        expect(component.html).toContain(NO_VALUE_MARK);
        expect(component.html).not.toContain("<table");
      });

      it("should render the label as a heading above the table", async () => {
        const component = zaakGegevensComponent("zaakdata", {
          formaat: "tabel",
          label: "Process data",
        });
        await formioSetupService.createFormioForm(
          { components: [component] },
          taak,
          { ...zaak, zaakdata: { a: "1" } } as GeneratedType<"RestZaak">,
        );

        expect(component.html).toContain(
          '<h4 class="zac-gegevens-heading">Process data</h4>',
        );
      });

      it("should escape a key and a value so neither can inject markup", async () => {
        const component = await renderTable("zaakdata", {
          ...zaak,
          zaakdata: { "<b>k</b>": "<img src=x onerror=alert(1)>" },
        });

        expect(component.html).not.toContain("<img");
        expect(component.html).not.toContain("<b>");
        expect(component.html).toContain("<code>&lt;b&gt;k&lt;/b&gt;</code>");
        expect(component.html).toContain("&lt;img");
      });

      it("should refuse to tabulate a single value, and say what to drop", async () => {
        const component = await renderTable("identificatie");

        expect(component.html).toContain("has no keys to tabulate");
        expect(component.html).toContain("ZAC_FORMAAT");
      });
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
