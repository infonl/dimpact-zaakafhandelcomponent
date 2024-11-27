/*
 * SPDX-FileCopyrightText: 2024 Lifely
 * SPDX-License-Identifier: EUPL-1.2+
 */

import { GeneratedType } from "../shared/utils/generated-types";
import {
  MappedSmartDocumentsTemplateGroupWithParentId,
  PlainTemplateMappings,
  SmartDocumentsTemplateGroupWithParentId,
} from "./smart-documents.service";

export const SMARTDOCUMENTS_TEMPLATE_GROUPS: GeneratedType<"RestSmartDocumentsTemplateGroup">[] =
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

export const SMARTDOCUMENTS_TEMPLATE_GROUPS_WITH_PARENT_IDS: SmartDocumentsTemplateGroupWithParentId[] =
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

export const MAPPED_SMARTDOCUMENTS_TEMPLATE_GROUPS_WITH_PARENT_IDS: MappedSmartDocumentsTemplateGroupWithParentId[] =
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

export const SOME_UNMAPPED_SMARTDOCUMENTS_TEMPLATE_GROUPS_WITH_PARENT_IDS: MappedSmartDocumentsTemplateGroupWithParentId[] =
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

export const PREPPED_FOR_REST_REQUEST_MAPPED_SMARTDOCUMENTS: GeneratedType<"RestMappedSmartDocumentsTemplateGroup">[] =
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

export const MAPPINGS_ONLY_FLAT_ARRAY: PlainTemplateMappings[] = [
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

export const ALL_GROUPS_FLATTENED: MappedSmartDocumentsTemplateGroupWithParentId[] =
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
