/*
 * SPDX-FileCopyrightText: 2024 Lifely
 * SPDX-License-Identifier: EUPL-1.2+
 */

import { HttpClientTestingModule } from "@angular/common/http/testing";
import { TestBed } from "@angular/core/testing";
import { TranslateService } from "@ngx-translate/core";
import isEqual from "lodash/isEqual";
import { FoutAfhandelingService } from "../fout-afhandeling/fout-afhandeling.service";
import { GeneratedType } from "../shared/utils/generated-types";
import {
  MappedSmartDocumentsTemplateGroupWithParentId,
  PlainTemplateMappings,
  SmartDocumentsService,
  SmartDocumentsTemplateGroupWithParentId,
} from "./smart-documents.service";

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

const SMARTDOCUMENTS_WITH_PARENT_IDS: SmartDocumentsTemplateGroupWithParentId[] =
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

const MAPPED_SMARTDOCUMENTS_WITH_PARENT_IDS: MappedSmartDocumentsTemplateGroupWithParentId[] =
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

const INPUT_SOME_UNMAPPED_SMARTDOCUMENTS_WITH_PARENT_IDS: MappedSmartDocumentsTemplateGroupWithParentId[] =
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
                  informatieObjectTypeUUID: "", // unmapped
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
          informatieObjectTypeUUID: "", // unmapped
          parentGroupId: "id-group-level-root",
        },
      ],
    },
  ];

const RESULT_ONLY_MAPPED_SMARTDOCUMENTS_WITH_PARENT_IDS: MappedSmartDocumentsTemplateGroupWithParentId[] =
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
              templates: [],
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
      templates: [],
    },
  ];

const MAPPINGS_ONLY_FLAT_ARRAY: PlainTemplateMappings[] = [
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

const RESULT_FLATTENED_GROUPS: MappedSmartDocumentsTemplateGroupWithParentId[] =
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
      TEST_INPUT_SMARTDOCUMENTS_LIST,
    );
    log(
      "addParentIdsToMakeTemplatesUnique",
      result,
      SMARTDOCUMENTS_WITH_PARENT_IDS,
    );

    expect(isEqual(result, SMARTDOCUMENTS_WITH_PARENT_IDS)).toBe(true);
  });

  it("getTemplateMappings - Should put all mappings from mapped SmartDocuments with parent id's into a flat array", () => {
    const result = smartDocumentsService.getTemplateMappings(
      MAPPED_SMARTDOCUMENTS_WITH_PARENT_IDS,
    );
    log("getTemplateMappings", result, MAPPINGS_ONLY_FLAT_ARRAY);

    expect(isEqual(result, MAPPINGS_ONLY_FLAT_ARRAY)).toBe(true);
  });

  it("addTemplateMappings - Should add all the mappings from flat array into SmartDocuments with parent id's", () => {
    const result = smartDocumentsService.addTemplateMappings(
      SMARTDOCUMENTS_WITH_PARENT_IDS,
      MAPPINGS_ONLY_FLAT_ARRAY,
    );
    log("addTemplateMappings", result, MAPPED_SMARTDOCUMENTS_WITH_PARENT_IDS);

    expect(isEqual(result, MAPPED_SMARTDOCUMENTS_WITH_PARENT_IDS)).toBe(true);
  });

  it("flattenNestedGroupsToRootGroups - Should flatten mapped SmartDocuments with parents id's nested array into flat groups array", () => {
    const result = smartDocumentsService.flattenNestedGroups(
      MAPPED_SMARTDOCUMENTS_WITH_PARENT_IDS,
    );
    log("flattenDocumentsTemplateGroup", result, RESULT_FLATTENED_GROUPS);

    expect(isEqual(result, RESULT_FLATTENED_GROUPS)).toBe(true);
  });

  it("getOnlyMappedTemplates - Should strip all unmapped branches from the mapped SmartDocuments with parents id's", () => {
    const result = smartDocumentsService.getOnlyMappedTemplates(
      INPUT_SOME_UNMAPPED_SMARTDOCUMENTS_WITH_PARENT_IDS,
    );
    log(
      "getOnlyMappedTemplates",
      result,
      RESULT_ONLY_MAPPED_SMARTDOCUMENTS_WITH_PARENT_IDS,
    );

    expect(
      isEqual(result, RESULT_ONLY_MAPPED_SMARTDOCUMENTS_WITH_PARENT_IDS),
    ).toBe(true);
  });
});
