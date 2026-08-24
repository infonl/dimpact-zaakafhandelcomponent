/*
 * SPDX-FileCopyrightText: 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

import { ExtendedComponentSchema, FormioForm } from "@formio/angular";
import { testQueryClient } from "../../../../../setupJest";
import { GeneratedType } from "../../../shared/utils/generated-types";
import { FormioSetupService } from "./formio-setup-service";
import {
  configureFormioSetupServiceTestBed,
  document1,
  document2,
  selectedUnsignedDocumentsFieldset,
  signedDocument,
  taak,
  unsignedDocumentsFieldset,
  zaak,
} from "./formio-setup-service.test-fixtures";

describe(FormioSetupService.name, () => {
  let formioSetupService: FormioSetupService;

  beforeEach(() => {
    ({ formioSetupService } = configureFormioSetupServiceTestBed());
  });

  // `testQueryClient` is shared by every test here, and the global `clearAllMocks` leaves
  // spy implementations in place: without this, one test's mocked documents feed the next.
  afterEach(() => jest.restoreAllMocks());

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
          zaak,
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
          zaak,
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
          zaak,
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
          zaak,
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
          zaak,
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
          zaak,
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
          zaak,
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
          zaak,
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
          zaak,
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
          zaak,
        );

        expect(component.customClass).toBe("");
        expect(component.description).toBe("");
      });

      it("should append the marker class to the class set by the form author", async () => {
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
          zaak,
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
          zaak,
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
        await formioSetupService.createFormioForm(form, taak, zaak);
        await formioSetupService.createFormioForm(form, taak, zaak);

        expect(component.customClass).toBe("zac-empty-input-field");
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
          zaak,
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
          zaak,
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
          zaak,
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
          zaak,
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
          zaak,
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
          zaak,
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
          zaak,
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
          zaak,
        );

        expect(component.defaultValue).toEqual([]);
        expect(fetchQuerySpy).not.toHaveBeenCalled();
      });
    },
  );
});
