/*
 * SPDX-FileCopyrightText: 2024 Lifely
 * SPDX-License-Identifier: EUPL-1.2+
 */

import { HttpClientTestingModule } from "@angular/common/http/testing";
import { TestBed } from "@angular/core/testing";
import { TranslateService } from "@ngx-translate/core";
import isEqual from "lodash/isEqual";
import { FoutAfhandelingService } from "../fout-afhandeling/fout-afhandeling.service";
import { SmartDocumentsService } from "./smart-documents.service";
import {
  SOME_UNMAPPED_SMARTDOCUMENTS_TEMPLATE_GROUPS_WITH_PARENT_IDS,
  MAPPED_SMARTDOCUMENTS_TEMPLATE_GROUPS_WITH_PARENT_IDS,
  MAPPINGS_ONLY_FLAT_ARRAY,
  ALL_GROUPS_FLATTENED,
  PREPPED_FOR_REST_REQUEST_MAPPED_SMARTDOCUMENTS,
  SMARTDOCUMENTS_TEMPLATE_GROUPS_WITH_PARENT_IDS,
  SMARTDOCUMENTS_TEMPLATE_GROUPS,
} from "./smart-documents.service.test-data";

const log = (test, result, expected) => {
  const _isEqual = isEqual(result, expected);

  if (!_isEqual) {
    const resultStr = JSON.stringify(result);
    const expectedStr = JSON.stringify(expected);
    for (let i = 0; i < Math.max(resultStr.length, expectedStr.length); i++) {
      if (resultStr[i] !== expectedStr[i]) {
        console.log(`Difference at index ${i}:`);
        console.log(`Result: ${resultStr.substring(i, i + 40)}`);
        console.log(`Expected: ${expectedStr.substring(i, i + 40)}`);
        break;
      }
    }
  }
  if (!_isEqual) {
    console.log("RESULT: ", test, JSON.stringify(result));
    console.log("EXPECT: ", test, JSON.stringify(expected));
  }
};

describe("SmartDocumentsService.flattenDocumentsTemplateGroup", () => {
  let smartDocumentsService: SmartDocumentsService;

  const mockTranslateService = {
    translate: jest.fn(),
  };
  const mockFoutAfhandelingService = {
    handleError: jest.fn(),
  };

  const objInput = {
    id: "id-group-level-root",
    name: "Dimpact",
    groups: [
      {
        id: "id-group-level-1",
        name: "level1 group",
        groups: [
          {
            id: "id-group-level-2",
            name: "level2 group",
            groups: [
              {
                id: "id-group-level-3",
                name: "level3 group",
                groups: [],
                templates: [
                  {
                    id: "id-template-level-3",
                    name: "level 1 template",
                    informatieObjectTypeUUID: "info-object-type-id-level-3",
                  },
                ],
              },
            ],
            templates: [
              {
                id: "id-template-level-2",
                name: "level 1 template",
                informatieObjectTypeUUID: "info-object-type-id-level-2",
              },
            ],
          },
        ],
        templates: [
          {
            id: "id-template-level-1",
            name: "level 1 template",
            informatieObjectTypeUUID: "info-object-type-id-level-1",
          },
        ],
      },
    ],
    templates: [
      {
        id: "id-template-level-root",
        name: "root level template",
        informatieObjectTypeUUID: "info-object-type-id-level-3",
      },
    ],
  };

  // Expected flattened output
  const expectedOutput = [
    {
      id: "id-group-level-root",
      name: "Dimpact",
      templates: [
        {
          id: "id-template-level-root",
          name: "root level template",
          informatieObjectTypeUUID: "info-object-type-id-level-3",
        },
      ],
    },
    {
      id: "id-group-level-1",
      name: "level1 group",
      templates: [
        {
          id: "id-template-level-1",
          name: "level 1 template",
          informatieObjectTypeUUID: "info-object-type-id-level-1",
        },
      ],
    },
    {
      id: "id-group-level-2",
      name: "level2 group",
      templates: [
        {
          id: "id-template-level-2",
          name: "level 1 template",
          informatieObjectTypeUUID: "info-object-type-id-level-2",
        },
      ],
    },
    {
      id: "id-group-level-3",
      name: "level3 group",
      templates: [
        {
          id: "id-template-level-3",
          name: "level 1 template",
          informatieObjectTypeUUID: "info-object-type-id-level-3",
        },
      ],
    },
  ];

  beforeEach(() => {
    TestBed.configureTestingModule({
      imports: [HttpClientTestingModule],
      providers: [
        SmartDocumentsService,
        {
          provide: FoutAfhandelingService,
          useValue: mockFoutAfhandelingService,
        },
        { provide: TranslateService, useValue: mockTranslateService },
      ],
    });

    smartDocumentsService = TestBed.inject(SmartDocumentsService);
  });

  it("should flatten the document groups", () => {
    const result =
      smartDocumentsService.flattenDocumentsTemplateGroup(objInput);

    expect(result).toEqual(expectedOutput);
  });
});

describe("SmartDocumentsService service functions tests", () => {
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
      imports: [HttpClientTestingModule],
      providers: [
        SmartDocumentsService,
        {
          provide: FoutAfhandelingService,
          useValue: mockFoutAfhandelingService,
        },
        { provide: TranslateService, useValue: mockTranslateService },
      ],
    });

    smartDocumentsService = TestBed.inject(SmartDocumentsService);
  });

  it("addParentIdsToMakeTemplatesUnique - Should add parentGroupId to all templates", () => {
    const result = smartDocumentsService.addParentIdsToMakeTemplatesUnique(
      SMARTDOCUMENTS_TEMPLATE_GROUPS,
    );
    log(
      "addParentIdsToMakeTemplatesUnique",
      result,
      SMARTDOCUMENTS_TEMPLATE_GROUPS_WITH_PARENT_IDS,
    );

    expect(
      isEqual(result, SMARTDOCUMENTS_TEMPLATE_GROUPS_WITH_PARENT_IDS),
    ).toBe(true);
  });

  it("getTemplateMappings - Should put all mappings from mapped SmartDocuments with parent id's into a flat array", () => {
    const result = smartDocumentsService.getTemplateMappings(
      MAPPED_SMARTDOCUMENTS_TEMPLATE_GROUPS_WITH_PARENT_IDS,
    );
    log("getTemplateMappings", result, MAPPINGS_ONLY_FLAT_ARRAY);

    expect(isEqual(result, MAPPINGS_ONLY_FLAT_ARRAY)).toBe(true);
  });

  it("addTemplateMappings - Should add all the mappings from flat array into SmartDocuments with parent id's", () => {
    const result = smartDocumentsService.addTemplateMappings(
      SMARTDOCUMENTS_TEMPLATE_GROUPS_WITH_PARENT_IDS,
      MAPPINGS_ONLY_FLAT_ARRAY,
    );
    log(
      "addTemplateMappings",
      result,
      MAPPED_SMARTDOCUMENTS_TEMPLATE_GROUPS_WITH_PARENT_IDS,
    );

    expect(
      isEqual(result, MAPPED_SMARTDOCUMENTS_TEMPLATE_GROUPS_WITH_PARENT_IDS),
    ).toBe(true);
  });

  it("flattenNestedGroupsToRootGroups - Should flatten mapped SmartDocuments with parents id's nested array into flat groups array", () => {
    const result = smartDocumentsService.flattenNestedGroups(
      MAPPED_SMARTDOCUMENTS_TEMPLATE_GROUPS_WITH_PARENT_IDS,
    );
    log("flattenDocumentsTemplateGroup", result, ALL_GROUPS_FLATTENED);

    expect(isEqual(result, ALL_GROUPS_FLATTENED)).toBe(true);
  });

  it("getOnlyMappedTemplates - Should strip all unmapped branches from the mapped SmartDocuments with parents id's", () => {
    const result = smartDocumentsService.getOnlyMappedTemplates(
      SOME_UNMAPPED_SMARTDOCUMENTS_TEMPLATE_GROUPS_WITH_PARENT_IDS,
    );
    log(
      "getOnlyMappedTemplates",
      result,
      PREPPED_FOR_REST_REQUEST_MAPPED_SMARTDOCUMENTS,
    );

    expect(
      isEqual(result, PREPPED_FOR_REST_REQUEST_MAPPED_SMARTDOCUMENTS),
    ).toBe(true);
  });
});
