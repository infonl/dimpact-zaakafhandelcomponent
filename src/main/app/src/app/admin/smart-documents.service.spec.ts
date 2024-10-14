import { TestBed } from "@angular/core/testing";
import { TranslateService } from "@ngx-translate/core";
import { HttpClientTestingModule } from "@angular/common/http/testing";
import { SmartDocumentsService } from "./smart-documents.service";
import { UtilService } from "../core/service/util.service";
import { FoutAfhandelingService } from "../fout-afhandeling/fout-afhandeling.service";

describe("SmartDocumentsService", () => {
  let smartDocumentsService: SmartDocumentsService;

  // Mock the dependencies
  const mockTranslateService = {
    translate: jest.fn(),
  };
  const mockUtilService = {
    someUtilFunction: jest.fn(),
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
        { provide: UtilService, useValue: mockUtilService },
        { provide: TranslateService, useValue: mockTranslateService },
      ],
    });

    smartDocumentsService = TestBed.inject(SmartDocumentsService);
  });

  it("should flatten the document groups and return expected output", () => {
    const result =
      smartDocumentsService.flattenDocumentsTemplateGroup(objInput);

    expect(result).toEqual(expectedOutput);
  });
});
