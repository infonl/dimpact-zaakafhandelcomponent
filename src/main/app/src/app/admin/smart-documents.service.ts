/*
 * SPDX-FileCopyrightText: 2024 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

import { Injectable } from "@angular/core";
import { map } from "rxjs";
import {
  PostBody,
  PutBody,
  ZacHttpClient,
} from "../shared/http/zac-http-client";
import { GeneratedType } from "../shared/utils/generated-types";

export type SmartDocumentsTemplateWithParentId =
  GeneratedType<"RestSmartDocumentsTemplate"> & {
    parentGroupId?: string | null;
  };
export type SmartDocumentsTemplateGroupWithParentId = Omit<
  GeneratedType<"RestSmartDocumentsTemplateGroup">,
  "groups" | "templates"
> & {
  groups?: SmartDocumentsTemplateGroupWithParentId[] | null;
  templates?: SmartDocumentsTemplateWithParentId[] | null;
};

export type MappedSmartDocumentsTemplateWithParentId =
  GeneratedType<"RestMappedSmartDocumentsTemplate"> & {
    parentGroupId?: string | null;
  };
export type MappedSmartDocumentsTemplateGroupWithParentId = Omit<
  GeneratedType<"RestMappedSmartDocumentsTemplateGroup">,
  "groups" | "templates"
> & {
  groups?: MappedSmartDocumentsTemplateGroupWithParentId[] | null;
  templates?: MappedSmartDocumentsTemplateWithParentId[] | null;
};

export type PlainTemplateMappings = Omit<
  MappedSmartDocumentsTemplateWithParentId,
  "name"
>;

export type MappedSmartDocumentsTemplateFlattenedGroupWithParentId = Omit<
  MappedSmartDocumentsTemplateGroupWithParentId,
  "groups"
>;

@Injectable({ providedIn: "root" })
export class SmartDocumentsService {
  constructor(private readonly zacHttpClient: ZacHttpClient) {}

  getAllSmartDocumentsTemplateGroups() {
    return this.zacHttpClient.GET(
      "/rest/zaakafhandelparameters/smartdocuments-templates",
    );
  }

  getTemplatesMapping(zaakafhandelUUID: string) {
    return this.zacHttpClient
      .GET(
        "/rest/zaakafhandelparameters/{zaakafhandelUUID}/smartdocuments-templates-mapping",
        { path: { zaakafhandelUUID } },
      )
      .pipe(map((data) => this.flattenNestedGroups(data)));
  }

  storeTemplatesMapping(
    zaakafhandelUUID: string,
    body: PostBody<"/rest/zaakafhandelparameters/{zaakafhandelUUID}/smartdocuments-templates-mapping">,
  ) {
    return this.zacHttpClient.POST(
      "/rest/zaakafhandelparameters/{zaakafhandelUUID}/smartdocuments-templates-mapping",
      body,
      { path: { zaakafhandelUUID } },
    );
  }

  getTemplateGroup(
    body: PutBody<"/rest/zaakafhandelparameters/smartdocuments-template-group">,
    templateName: string,
    informatieObjectTypeUUID: string,
  ) {
    return this.zacHttpClient
      .PUT("/rest/zaakafhandelparameters/smartdocuments-template-group", body)
      .pipe(
        map((data) =>
          this.getOnlyOneTemplate(data, templateName, informatieObjectTypeUUID),
        ),
      );
  }

  getOnlyOneTemplate = (
    templateGroup: GeneratedType<"RestSmartDocumentsTemplateGroup">,
    templateName: string,
    informatieObjectTypeUUID: string,
  ): GeneratedType<"RestMappedSmartDocumentsTemplateGroup">[] => [
    {
      id: templateGroup.id,
      name: templateGroup.name,
      groups: null,
      templates: [
        {
          id: templateGroup.templates!.find(
            ({ name }) => name === templateName,
          )!.id,
          name: templateName,
          informatieObjectTypeUUID,
        },
      ],
    },
  ];

  getOnlyMappedTemplates = (
    data: MappedSmartDocumentsTemplateGroupWithParentId[],
  ): GeneratedType<"RestMappedSmartDocumentsTemplateGroup">[] => {
    return data
      .map((group) => {
        const templates = (group.templates || [])
          .filter((template) => template.informatieObjectTypeUUID)
          // eslint-disable-next-line @typescript-eslint/no-unused-vars
          .map(({ parentGroupId, ...template }) => template);

        const groups = group.groups
          ? this.getOnlyMappedTemplates(group.groups)
          : [];

        if (templates.length || groups.length) {
          const { ...cleanedGroup } = group;
          return {
            ...cleanedGroup,
            templates,
            groups,
          };
        }

        return null;
      })
      .filter(Boolean);
  };

  flattenNestedGroups(
    groups: MappedSmartDocumentsTemplateGroupWithParentId[],
  ): MappedSmartDocumentsTemplateFlattenedGroupWithParentId[] {
    const result = [];

    function traverse(group) {
      const { id, name, templates = [] } = group;
      if (templates.length) result.push({ id, name, templates });

      if (group.groups) {
        group.groups.forEach(traverse);
      }
    }

    groups.forEach(traverse);

    return result;
  }

  addTemplateMappings = (
    groups: GeneratedType<"RestSmartDocumentsTemplateGroup">[],
    uuidsToAdd: PlainTemplateMappings[],
  ): MappedSmartDocumentsTemplateGroupWithParentId[] => {
    const updateGroup = (
      group: MappedSmartDocumentsTemplateGroupWithParentId,
    ): MappedSmartDocumentsTemplateGroupWithParentId => ({
      ...group,
      templates: (group.templates || []).map((template) => ({
        ...template,
        informatieObjectTypeUUID: uuidsToAdd.find(
          (uuidItem) =>
            uuidItem.id === template.id &&
            uuidItem.parentGroupId === template.parentGroupId,
        )?.informatieObjectTypeUUID,
      })),
      groups: group.groups ? group.groups.map(updateGroup) : [],
    });

    return groups.map(updateGroup);
  };

  getTemplateMappings = (
    data: MappedSmartDocumentsTemplateGroupWithParentId[],
  ): PlainTemplateMappings[] => {
    return data.flatMap((item) => {
      const itemTemplates =
        item.templates?.map((template) => ({
          id: template.id,
          parentGroupId: template.parentGroupId,
          informatieObjectTypeUUID: template.informatieObjectTypeUUID,
        })) ?? [];

      const groupTemplates =
        item.groups?.flatMap((group) => [
          ...group.templates.map((template) => ({
            id: template.id,
            parentGroupId: template.parentGroupId,
            informatieObjectTypeUUID: template.informatieObjectTypeUUID,
          })),

          ...this.getTemplateMappings(group.groups || []),
        ]) ?? [];

      return [...itemTemplates, ...groupTemplates];
    });
  };

  addParentIdsToMakeTemplatesUnique = <
    T extends
      | GeneratedType<"RestSmartDocumentsTemplateGroup">
      | GeneratedType<"RestMappedSmartDocumentsTemplateGroup">,
  >(
    data?: T[],
  ): (Omit<T, "templates"> & {
    templates: (T["templates"][number] & { parentGroupId: string })[];
  })[] => {
    if (!data) {
      return [];
    }

    return data.map((group) => {
      return {
        ...group,
        templates: group.templates?.map((template: T["templates"][number]) => ({
          ...template,
          parentGroupId: group.id,
        })),
        groups: group.groups
          ? this.addParentIdsToMakeTemplatesUnique(group.groups)
          : [],
      };
    });
  };
}
