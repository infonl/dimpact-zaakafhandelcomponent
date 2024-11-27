/*
 * SPDX-FileCopyrightText: 2024 Lifely
 * SPDX-License-Identifier: EUPL-1.2+
 */

import { HttpClientTestingModule } from "@angular/common/http/testing";
import { TestBed } from "@angular/core/testing";
import { TranslateService } from "@ngx-translate/core";
import { FoutAfhandelingService } from "../fout-afhandeling/fout-afhandeling.service";
import {
  MappedSmartDocumentsTemplateFlattenedGroupWithParentId,
  MappedSmartDocumentsTemplateGroupWithParentId,
  MappedSmartDocumentsTemplateWithParentId,
  SmartDocumentsService,
} from "./smart-documents.service";
import { GeneratedType } from "../shared/utils/generated-types";

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

const TEST_INPUT_MAPPED_SMARTDOCUMENTS_LIST: GeneratedType<"RestMappedSmartDocumentsTemplateGroup">[] =
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

const INTERMEDIATE_RESULT_MAPPED_SMARTDOCUMENTS_WITH_PARENT_IDS = [
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

const INTERMEDIATE_RESULT_MAPPINGS_FLAT_ARRAY = [
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

describe("SmartDocumentsService.addParentIdToTemplates", () => {
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

  it("Should add parentGroupId to all templates", () => {
    const result = smartDocumentsService.addParentIdToTemplates(
      TEST_INPUT_MAPPED_SMARTDOCUMENTS_LIST,
    );

    // console.log("result", JSON.stringify(result));
    // console.log(
    //   "RESULT",
    //   JSON.stringify(INTERMEDIATE_RESULT_MAPPED_SMARTDOCUMENTS_WITH_PARENT_IDS),
    // );

    expect(result).toEqual(
      INTERMEDIATE_RESULT_MAPPED_SMARTDOCUMENTS_WITH_PARENT_IDS,
    );
  });

  it("Should add parentGroupId to all templates", () => {
    const result = smartDocumentsService.getTemplateMappings(
      INTERMEDIATE_RESULT_MAPPED_SMARTDOCUMENTS_WITH_PARENT_IDS,
    );

    // console.log("result", JSON.stringify(result));
    // console.log(
    //   "INTERMEDIATE_RESULT_MAPPINGS_FLAT_ARRAY",
    //   JSON.stringify(INTERMEDIATE_RESULT_MAPPINGS_FLAT_ARRAY),
    // );
    // console.log(
    //   "result::::",
    //   JSON.stringify(result) ===
    //     JSON.stringify(INTERMEDIATE_RESULT_MAPPINGS_FLAT_ARRAY),
    // );

    // // expect(result).toEqual(INTERMEDIATE_RESULT_MAPPINGS_FLAT_ARRAY);

    expect(result).toEqual(INTERMEDIATE_RESULT_MAPPINGS_FLAT_ARRAY);
  });

  it("Should add the mappings to original ", () => {
    const result = smartDocumentsService.addTemplateMappings(
      TEST_INPUT_SMARTDOCUMENTS_LIST,
      INTERMEDIATE_RESULT_MAPPINGS_FLAT_ARRAY,
    );

    // console.log("result", JSON.stringify(result));
    // console.log(
    //   "INTERMEDIATE_RESULT_MAPPINGS_FLAT_ARRAY",
    //   JSON.stringify(INTERMEDIATE_RESULT_MAPPINGS_FLAT_ARRAY),
    // );
    // console.log(
    //   "result::::",
    //   JSON.stringify(result) ===
    //     JSON.stringify(INTERMEDIATE_RESULT_MAPPINGS_FLAT_ARRAY),
    // );

    // // expect(result).toEqual(INTERMEDIATE_RESULT_MAPPINGS_FLAT_ARRAY);

    expect(result).toEqual(
      INTERMEDIATE_RESULT_MAPPED_SMARTDOCUMENTS_WITH_PARENT_IDS,
    );
  });
});
