/*
 * SPDX-FileCopyrightText: 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

import { ExtendedComponentSchema, FormioForm } from "@formio/angular";
import { testQueryClient } from "../../../../../setupJest";
import { FormioSetupService } from "./formio-setup-service";
import {
  configureFormioSetupServiceTestBed,
  smartDocumentsTemplateGroupsComponent,
  smartDocumentsTemplateGroupTemplatesComponent,
  taak,
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
});
