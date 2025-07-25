/*
 * SPDX-FileCopyrightText: 2024 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

import {
  provideHttpClient,
  withInterceptorsFromDi,
} from "@angular/common/http";
import { provideHttpClientTesting } from "@angular/common/http/testing";
import { TestBed } from "@angular/core/testing";
import { TranslateService } from "@ngx-translate/core";
import { fromPartial } from "@total-typescript/shoehorn";
import { FoutAfhandelingService } from "../fout-afhandeling/fout-afhandeling.service";
import { GeneratedType } from "../shared/utils/generated-types";
import {
  SmartDocumentsService,
  TemplateMapping,
} from "./smart-documents.service";

describe(SmartDocumentsService.name, () => {
  let smartDocumentsService: SmartDocumentsService;

  const mockTranslateService = {
    translate: jest.fn(),
  };
  const mockFoutAfhandelingService = {
    handleError: jest.fn(),
  };

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [SmartDocumentsService],
    });

    TestBed.configureTestingModule({
      imports: [],
      providers: [
        SmartDocumentsService,
        {
          provide: FoutAfhandelingService,
          useValue: mockFoutAfhandelingService,
        },
        { provide: TranslateService, useValue: mockTranslateService },
        provideHttpClient(withInterceptorsFromDi()),
        provideHttpClientTesting(),
      ],
    });

    smartDocumentsService = TestBed.inject(SmartDocumentsService);
  });

  it(SmartDocumentsService.prototype.addParentIdsToTemplates.name, () => {
    const input = fromPartial<
      GeneratedType<"RestMappedSmartDocumentsTemplateGroup">[]
    >([
      {
        id: "group-1",
        name: "Group 1",
        templates: [
          {
            id: "template-1",
            name: "Template 1",
            informatieObjectTypeUUID: "",
          },
          {
            id: "template-2",
            name: "Template 2",
            informatieObjectTypeUUID: "",
          },
        ],
        groups: [
          {
            id: "group-2",
            name: "Group 2",
            templates: [
              {
                id: "template-3",
                name: "Template 3",
                informatieObjectTypeUUID: "",
              },
            ],
          },
        ],
      },
    ]);

    const result = smartDocumentsService.addParentIdsToTemplates(input);

    expect(
      (result[0].templates![0] as unknown as { parentGroupId: string })
        .parentGroupId,
    ).toBe("group-1");
    expect(
      (result[0].templates![1] as unknown as { parentGroupId: string })
        .parentGroupId,
    ).toBe("group-1");
    expect(
      (
        result[0].groups![0].templates![0] as unknown as {
          parentGroupId: string;
        }
      ).parentGroupId,
    ).toBe("group-2");
  });

  it(SmartDocumentsService.prototype.getTemplateMappings.name, () => {
    const input = fromPartial<
      GeneratedType<"RestMappedSmartDocumentsTemplateGroup">[]
    >([
      {
        id: "group-1",
        name: "Group 1",
        templates: [
          {
            id: "template-1",
            name: "Template 1",
            informatieObjectTypeUUID: "uuid-1",
          },
        ],
        groups: [
          {
            id: "group-2",
            name: "Group 2",
            templates: [
              {
                id: "template-2",
                name: "Template 2",
                informatieObjectTypeUUID: "uuid-2",
              },
            ],
          },
        ],
      },
    ]);

    const result = smartDocumentsService.getTemplateMappings(input);

    expect(result).toHaveLength(2);
    expect(result[0]).toEqual({
      id: "template-1",
      parentGroupId: "group-1",
      informatieObjectTypeUUID: "uuid-1",
    });
    expect(result[1]).toEqual({
      id: "template-2",
      parentGroupId: "group-2",
      informatieObjectTypeUUID: "uuid-2",
    });
  });

  it(SmartDocumentsService.prototype.addTemplateMappings.name, () => {
    const groups = fromPartial<
      GeneratedType<"RestMappedSmartDocumentsTemplateGroup">[]
    >([
      {
        id: "group-1",
        name: "Group 1",
        templates: [{ id: "template-1", name: "Template 1" }],
      },
    ]);

    const mappings: TemplateMapping[] = [
      {
        id: "template-1",
        parentGroupId: "group-1",
        informatieObjectTypeUUID: "uuid-1",
      },
    ];

    const result = smartDocumentsService.addTemplateMappings(groups, mappings);

    expect(result[0].templates![0].informatieObjectTypeUUID).toBe("uuid-1");
  });

  it(SmartDocumentsService.prototype.flattenGroups.name, () => {
    const input = fromPartial<
      GeneratedType<"RestMappedSmartDocumentsTemplateGroup">[]
    >([
      {
        id: "group-1",
        name: "Group 1",
        templates: [
          {
            id: "template-1",
            name: "Template 1",
            informatieObjectTypeUUID: "uuid-1",
          },
        ],
        groups: [
          {
            id: "group-2",
            name: "Group 2",
            templates: [
              {
                id: "template-2",
                name: "Template 2",
                informatieObjectTypeUUID: "uuid-2",
              },
            ],
          },
        ],
      },
    ]);

    const result = smartDocumentsService.flattenGroups(input);

    expect(result).toHaveLength(2);
    expect(result[0].id).toBe("group-1");
    expect(result[1].id).toBe("group-2");
  });

  it(SmartDocumentsService.prototype.getOnlyMappedTemplates.name, () => {
    const input = fromPartial<
      GeneratedType<"RestMappedSmartDocumentsTemplateGroup">[]
    >([
      {
        id: "group-1",
        name: "Group 1",
        templates: [
          {
            id: "template-1",
            name: "Template 1",
            informatieObjectTypeUUID: "uuid-1",
          }, // mapped
          {
            id: "template-2",
            name: "Template 2",
            informatieObjectTypeUUID: "",
          }, // unmapped
        ],
        groups: [
          {
            id: "group-2",
            name: "Group 2",
            templates: [
              {
                id: "template-3",
                name: "Template 3",
                informatieObjectTypeUUID: "uuid-3",
              },
            ], // mapped
          },
        ],
      },
    ]);

    const result = smartDocumentsService.getOnlyMappedTemplates(input);

    expect(result[0].templates).toHaveLength(1); // only mapped template
    expect(result[0].templates![0].id).toBe("template-1");
    expect(result[0].groups![0].templates![0].id).toBe("template-3");
  });
});
