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
  GROUPS_FLATTENED,
  MAPPED_SMARTDOCUMENTS_TEMPLATE_GROUPS_WITH_PARENT_IDS,
  MAPPINGS_ONLY_FLAT_ARRAY,
  PREPPED_FOR_REST_REQUEST_MAPPED_SMARTDOCUMENTS,
  SMARTDOCUMENTS_TEMPLATE_GROUPS,
  SMARTDOCUMENTS_TEMPLATE_GROUPS_WITH_PARENT_IDS,
  SOME_UNMAPPED_SMARTDOCUMENTS_TEMPLATE_GROUPS_WITH_PARENT_IDS,
} from "./smart-documents.service.test-data";

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

    expect(
      isEqual(result, SMARTDOCUMENTS_TEMPLATE_GROUPS_WITH_PARENT_IDS),
    ).toBe(true);
  });

  it("getTemplateMappings - Should put all mappings from mapped SmartDocuments with parent id's into a flat array", () => {
    const result = smartDocumentsService.getTemplateMappings(
      MAPPED_SMARTDOCUMENTS_TEMPLATE_GROUPS_WITH_PARENT_IDS,
    );

    expect(isEqual(result, MAPPINGS_ONLY_FLAT_ARRAY)).toBe(true);
  });

  it("addTemplateMappings - Should add all the mappings from flat array into SmartDocuments with parent id's", () => {
    const result = smartDocumentsService.addTemplateMappings(
      SMARTDOCUMENTS_TEMPLATE_GROUPS_WITH_PARENT_IDS,
      MAPPINGS_ONLY_FLAT_ARRAY,
    );

    expect(
      isEqual(result, MAPPED_SMARTDOCUMENTS_TEMPLATE_GROUPS_WITH_PARENT_IDS),
    ).toBe(true);
  });

  it("flattenNestedGroupsToRootGroups - Should flatten mapped SmartDocuments with parents id's nested array into flat groups array", () => {
    const result = smartDocumentsService.flattenNestedGroups(
      MAPPED_SMARTDOCUMENTS_TEMPLATE_GROUPS_WITH_PARENT_IDS,
    );

    expect(isEqual(result, GROUPS_FLATTENED)).toBe(true);
  });

  it("getOnlyMappedTemplates - Should strip all unmapped branches from the mapped SmartDocuments with parents id's", () => {
    const result = smartDocumentsService.getOnlyMappedTemplates(
      SOME_UNMAPPED_SMARTDOCUMENTS_TEMPLATE_GROUPS_WITH_PARENT_IDS,
    );

    expect(
      isEqual(result, PREPPED_FOR_REST_REQUEST_MAPPED_SMARTDOCUMENTS),
    ).toBe(true);
  });

  it("All functions - Should handle empty array", () => {
    expect(
      isEqual(smartDocumentsService.addParentIdsToMakeTemplatesUnique([]), []),
    ).toBe(true);
    expect(isEqual(smartDocumentsService.getTemplateMappings([]), [])).toBe(
      true,
    );
    expect(isEqual(smartDocumentsService.addTemplateMappings([], []), [])).toBe(
      true,
    );
    expect(isEqual(smartDocumentsService.flattenNestedGroups([]), [])).toBe(
      true,
    );
    expect(isEqual(smartDocumentsService.getOnlyMappedTemplates([]), [])).toBe(
      true,
    );
  });
});
