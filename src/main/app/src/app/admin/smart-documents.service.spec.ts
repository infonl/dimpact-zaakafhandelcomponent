/*
 * SPDX-FileCopyrightText: 2024 Lifely
 * SPDX-License-Identifier: EUPL-1.2+
 */

import { HttpClientTestingModule } from "@angular/common/http/testing";
import { TestBed } from "@angular/core/testing";
import { TranslateService } from "@ngx-translate/core";
import { FoutAfhandelingService } from "../fout-afhandeling/fout-afhandeling.service";
import {
  SmartDocumentsTemplateGroupWithParentId,
  MappedSmartDocumentsTemplateFlattenedGroupWithParentId,
  MappedSmartDocumentsTemplateGroupWithParentId,
  MappedSmartDocumentsTemplateWithParentId,
  PlainTemplateMappings,
  SmartDocumentsService,
} from "./smart-documents.service";
import { GeneratedType } from "../shared/utils/generated-types";
import isEqual from "lodash/isEqual";

const TEST_INPUT_SMARTDOCUMENTS_LIST: GeneratedType<"RestSmartDocumentsTemplateGroup">[] =
  [
    {
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
                    },
                  ],
                },
              ],
              templates: [
                {
                  id: "id-template-level-2",
                  name: "level 1 template",
                },
              ],
            },
          ],
          templates: [
            {
              id: "id-template-level-1",
              name: "level 1 template",
            },
          ],
        },
      ],
      templates: [
        {
          id: "id-template-level-root",
          name: "root level template",
        },
      ],
    },
  ];

const INTERMEDIATE_RESULT_SMARTDOCUMENTS_WITH_PARENT_IDS: SmartDocumentsTemplateGroupWithParentId[] =
  [
    {
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
                      parentGroupId: "id-group-level-3",
                    },
                  ],
                },
              ],
              templates: [
                {
                  id: "id-template-level-2",
                  name: "level 1 template",
                  parentGroupId: "id-group-level-2",
                },
              ],
            },
          ],
          templates: [
            {
              id: "id-template-level-1",
              name: "level 1 template",
              parentGroupId: "id-group-level-1",
            },
          ],
        },
      ],
      templates: [
        {
          id: "id-template-level-root",
          name: "root level template",
          parentGroupId: "id-group-level-root",
        },
      ],
    },
  ];

const TEST_INPUT_MAPPED_SMARTDOCUMENTS_LIST: MappedSmartDocumentsTemplateGroupWithParentId[] =
  [
    {
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
          informatieObjectTypeUUID: "info-object-type-id-level-root",
        },
      ],
    },
  ];

const INTERMEDIATE_RESULT_MAPPED_SMARTDOCUMENTS_WITH_PARENT_IDS: MappedSmartDocumentsTemplateGroupWithParentId[] =
  [
    {
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
                      parentGroupId: "id-group-level-3",
                    },
                  ],
                },
              ],
              templates: [
                {
                  id: "id-template-level-2",
                  name: "level 1 template",
                  informatieObjectTypeUUID: "info-object-type-id-level-2",
                  parentGroupId: "id-group-level-2",
                },
              ],
            },
          ],
          templates: [
            {
              id: "id-template-level-1",
              name: "level 1 template",
              informatieObjectTypeUUID: "info-object-type-id-level-1",
              parentGroupId: "id-group-level-1",
            },
          ],
        },
      ],
      templates: [
        {
          id: "id-template-level-root",
          name: "root level template",
          informatieObjectTypeUUID: "info-object-type-id-level-root",
          parentGroupId: "id-group-level-root",
        },
      ],
    },
  ];

const INTERMEDIATE_RESULT_MAPPINGS_FLAT_ARRAY: PlainTemplateMappings[] = [
  {
    id: "id-template-level-root",
    parentGroupId: "id-group-level-root",
    informatieObjectTypeUUID: "info-object-type-id-level-root",
  },
  {
    id: "id-template-level-1",
    parentGroupId: "id-group-level-1",
    informatieObjectTypeUUID: "info-object-type-id-level-1",
  },
  {
    id: "id-template-level-2",
    parentGroupId: "id-group-level-2",
    informatieObjectTypeUUID: "info-object-type-id-level-2",
  },
  {
    id: "id-template-level-3",
    parentGroupId: "id-group-level-3",
    informatieObjectTypeUUID: "info-object-type-id-level-3",
  },
];

const INTERMEDIATE_RESULT_FLATTENED_GROUPS: MappedSmartDocumentsTemplateGroupWithParentId[] =
  [
    {
      id: "id-group-level-root",
      name: "Dimpact",
      templates: [
        {
          id: "id-template-level-root",
          name: "root level template",
          informatieObjectTypeUUID: "info-object-type-id-level-root",
          parentGroupId: "id-group-level-root",
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
          parentGroupId: "id-group-level-1",
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
          parentGroupId: "id-group-level-2",
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
          parentGroupId: "id-group-level-3",
        },
      ],
    },
  ];

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

  console.log("result: ", test, _isEqual);

  if (!_isEqual) {
    console.log("RESULT:", JSON.stringify(result));
    console.log("EXPECT:", JSON.stringify(expected));
  }
};

describe("SmartDocumentsService.flattenDocumentsTemplateGroup", () => {
  let smartDocumentsService: SmartDocumentsService;

  // Mock the dependencies
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

  // Mock the dependencies
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

  it("addParentIdToTemplates - Should add parentGroupId to all templates", () => {
    const result = smartDocumentsService.addParentIdToTemplates(
      TEST_INPUT_SMARTDOCUMENTS_LIST,
    );
    log(
      "addParentIdToTemplates",
      result,
      INTERMEDIATE_RESULT_SMARTDOCUMENTS_WITH_PARENT_IDS,
    );

    expect(
      isEqual(result, INTERMEDIATE_RESULT_SMARTDOCUMENTS_WITH_PARENT_IDS),
    ).toBe(true);
  });

  it("getTemplateMappings - Should add parentGroupId to all templates", () => {
    const result = smartDocumentsService.getTemplateMappings(
      INTERMEDIATE_RESULT_MAPPED_SMARTDOCUMENTS_WITH_PARENT_IDS,
    );
    log("getTemplateMappings", result, INTERMEDIATE_RESULT_MAPPINGS_FLAT_ARRAY);

    expect(isEqual(result, INTERMEDIATE_RESULT_MAPPINGS_FLAT_ARRAY)).toBe(true);
  });

  it("addTemplateMappings - Should add the mappings to original ", () => {
    const result = smartDocumentsService.addTemplateMappings(
      INTERMEDIATE_RESULT_SMARTDOCUMENTS_WITH_PARENT_IDS,
      INTERMEDIATE_RESULT_MAPPINGS_FLAT_ARRAY,
    );
    log(
      "addTemplateMappings",
      result,
      INTERMEDIATE_RESULT_MAPPED_SMARTDOCUMENTS_WITH_PARENT_IDS,
    );

    expect(
      isEqual(
        result,
        INTERMEDIATE_RESULT_MAPPED_SMARTDOCUMENTS_WITH_PARENT_IDS,
      ),
    ).toBe(true);
  });

  it("flattenNestedGroupsToRootGroups - Should add the mappings to original ", () => {
    const result = smartDocumentsService.flattenNestedGroupsToRootGroups(
      INTERMEDIATE_RESULT_MAPPED_SMARTDOCUMENTS_WITH_PARENT_IDS,
    );
    log(
      "flattenDocumentsTemplateGroup",
      result,
      INTERMEDIATE_RESULT_FLATTENED_GROUPS,
    );

    expect(isEqual(result, INTERMEDIATE_RESULT_FLATTENED_GROUPS)).toBe(true);
  });
});
